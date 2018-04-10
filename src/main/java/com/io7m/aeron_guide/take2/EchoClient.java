package com.io7m.aeron_guide.take2;

import io.aeron.Aeron;
import io.aeron.ConcurrentPublication;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mindlessly simple Echo client.
 */

public final class EchoClient implements Closeable
{
  private static final Logger LOG = LoggerFactory.getLogger(EchoClient.class);

  private static final int ECHO_STREAM_ID = 0x2044f002;

  private static final Pattern PATTERN_ERROR =
    Pattern.compile("^ERROR (.*)$");
  private static final Pattern PATTERN_CONNECT =
    Pattern.compile("^CONNECT ([0-9]+) ([0-9]+) ([0-9A-F]+)$");
  private static final Pattern PATTERN_ECHO =
    Pattern.compile("^ECHO (.*)$");

  private final MediaDriver media_driver;
  private final Aeron aeron;
  private final EchoClientConfiguration configuration;
  private final SecureRandom random;
  private volatile int remote_data_port;
  private volatile int remote_control_port;
  private volatile boolean remote_ports_received;
  private volatile boolean failed;
  private volatile int remote_session;
  private volatile int duologue_key;

  private EchoClient(
    final MediaDriver in_media_driver,
    final Aeron in_aeron,
    final EchoClientConfiguration in_configuration)
  {
    this.media_driver =
      Objects.requireNonNull(in_media_driver, "media_driver");
    this.aeron =
      Objects.requireNonNull(in_aeron, "aeron");
    this.configuration =
      Objects.requireNonNull(in_configuration, "configuration");

    this.random = new SecureRandom();
  }

  /**
   * Create a new client.
   *
   * @param configuration The client configuration data
   *
   * @return A new client
   *
   * @throws EchoClientCreationException On any initialization error
   */

  public static EchoClient create(
    final EchoClientConfiguration configuration)
    throws EchoClientException
  {
    Objects.requireNonNull(configuration, "configuration");

    final String directory =
      configuration.baseDirectory()
        .toAbsolutePath()
        .toString();

    final MediaDriver.Context media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .publicationReservedSessionIdLow(EchoSessions.RESERVED_SESSION_ID_LOW)
        .publicationReservedSessionIdHigh(EchoSessions.RESERVED_SESSION_ID_HIGH)
        .aeronDirectoryName(directory);

    final Aeron.Context aeron_context =
      new Aeron.Context().aeronDirectoryName(directory);

    MediaDriver media_driver = null;

    try {
      media_driver = MediaDriver.launch(media_context);

      Aeron aeron = null;
      try {
        aeron = Aeron.connect(aeron_context);
      } catch (final Exception e) {
        closeIfNotNull(aeron);
        throw e;
      }

      return new EchoClient(media_driver, aeron, configuration);
    } catch (final Exception e) {
      try {
        closeIfNotNull(media_driver);
      } catch (final Exception c_ex) {
        e.addSuppressed(c_ex);
      }
      throw new EchoClientCreationException(e);
    }
  }

  private static void closeIfNotNull(
    final AutoCloseable closeable)
    throws Exception
  {
    if (closeable != null) {
      closeable.close();
    }
  }

  /**
   * Command-line entry point.
   *
   * @param args Command-line arguments
   *
   * @throws Exception On any error
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    if (args.length < 4) {
      LOG.error(
        "usage: directory remote-address remote-data-port remote-control-port");
      System.exit(1);
    }

    final Path directory = Paths.get(args[0]);
    final InetAddress remote_address = InetAddress.getByName(args[1]);
    final int remote_data_port = Integer.parseUnsignedInt(args[2]);
    final int remote_control_port = Integer.parseUnsignedInt(args[3]);

    final ImmutableEchoClientConfiguration configuration =
      ImmutableEchoClientConfiguration.builder()
        .baseDirectory(directory)
        .remoteAddress(remote_address)
        .remoteInitialControlPort(remote_control_port)
        .remoteInitialPort(remote_data_port)
        .build();

    try (final EchoClient client = create(configuration)) {
      client.run();
    }
  }

  /**
   * Run the client, returning when the client is finished.
   *
   * @throws EchoClientException On any error
   */

  public void run()
    throws EchoClientException
  {
    /*
     * Generate a one-time pad.
     */

    this.duologue_key = this.random.nextInt();

    final UnsafeBuffer buffer =
      new UnsafeBuffer(BufferUtil.allocateDirectAligned(1024, 16));

    final String session_name;
    try (final Subscription subscription = this.setupAllClientsSubscription()) {
      try (final Publication publication = this.setupAllClientsPublication()) {

        /*
         * Send a one-time pad to the server.
         */

        EchoMessages.sendMessage(
          publication,
          buffer,
          "HELLO " + Integer.toUnsignedString(this.duologue_key, 16).toUpperCase());

        session_name = Integer.toString(publication.sessionId());
        this.waitForConnectResponse(subscription, session_name);
      } catch (final IOException e) {
        throw new EchoClientIOException(e);
      }
    }

    /*
     * Connect to the publication and subscription that the server has sent
     * back to this client.
     */

    try (final Subscription subscription = this.setupConnectSubscription()) {
      try (final Publication publication = this.setupConnectPublication()) {
        this.runEchoLoop(buffer, session_name, subscription, publication);
      } catch (final IOException e) {
        throw new EchoClientIOException(e);
      }
    }
  }

  private void runEchoLoop(
    final UnsafeBuffer buffer,
    final String session_name,
    final Subscription subscription,
    final Publication publication)
    throws IOException
  {
    final FragmentHandler handler =
      new FragmentAssembler(
        (data, offset, length, header) ->
          onEchoResponse(session_name, data, offset, length));

    while (true) {

      /*
       * Send ECHO messages to the server and wait for responses.
       */

      EchoMessages.sendMessage(
        publication,
        buffer,
        "ECHO " + Long.toUnsignedString(this.random.nextLong(), 16));

      for (int index = 0; index < 100; ++index) {
        subscription.poll(handler, 1000);

        try {
          Thread.sleep(10L);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private static void onEchoResponse(
    final String session_name,
    final DirectBuffer buffer,
    final int offset,
    final int length)
  {
    final String response =
      EchoMessages.parseMessageUTF8(buffer, offset, length);

    LOG.debug("[{}] response: {}", session_name, response);

    final Matcher echo_matcher = PATTERN_ECHO.matcher(response);
    if (echo_matcher.matches()) {
      final String message = echo_matcher.group(1);
      LOG.debug("[{}] ECHO {}", session_name, message);
      return;
    }

    LOG.error(
      "[{}] server returned unrecognized message: {}",
      session_name,
      response);
  }

  private Publication setupConnectPublication()
    throws EchoClientTimedOutException
  {
    final ConcurrentPublication publication =
      EchoChannels.createPublicationWithSession(
        this.aeron,
        this.configuration.remoteAddress(),
        this.remote_data_port,
        this.remote_session,
        ECHO_STREAM_ID);

    for (int index = 0; index < 1000; ++index) {
      if (publication.isConnected()) {
        LOG.debug("CONNECT publication connected");
        return publication;
      }

      try {
        Thread.sleep(10L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    publication.close();
    throw new EchoClientTimedOutException("Making CONNECT publication to server");
  }

  private Subscription setupConnectSubscription()
    throws EchoClientTimedOutException
  {
    final Subscription subscription =
      EchoChannels.createSubscriptionDynamicMDCWithSession(
        this.aeron,
        this.configuration.remoteAddress(),
        this.remote_control_port,
        this.remote_session,
        ECHO_STREAM_ID);

    for (int index = 0; index < 1000; ++index) {
      if (subscription.isConnected() && subscription.imageCount() > 0) {
        LOG.debug("CONNECT subscription connected");
        return subscription;
      }

      try {
        Thread.sleep(10L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    subscription.close();
    throw new EchoClientTimedOutException(
      "Making CONNECT subscription to server");
  }

  private void waitForConnectResponse(
    final Subscription subscription,
    final String session_name)
    throws EchoClientTimedOutException, EchoClientRejectedException
  {
    LOG.debug("waiting for response");

    final FragmentHandler handler =
      new FragmentAssembler(
        (data, offset, length, header) ->
          this.onInitialResponse(session_name, data, offset, length));

    for (int index = 0; index < 1000; ++index) {
      subscription.poll(handler, 1000);

      if (this.failed) {
        throw new EchoClientRejectedException("Server rejected this client");
      }

      if (this.remote_ports_received) {
        return;
      }

      try {
        Thread.sleep(10L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    throw new EchoClientTimedOutException(
      "Waiting for CONNECT response from server");
  }

  /**
   * Parse the initial response from the server.
   */

  private void onInitialResponse(
    final String session_name,
    final DirectBuffer buffer,
    final int offset,
    final int length)
  {
    final String response =
      EchoMessages.parseMessageUTF8(buffer, offset, length);

    LOG.trace("[{}] response: {}", session_name, response);

    /*
     * Try to extract the session identifier to determine whether the message
     * was intended for this client or not.
     */

    final int space = response.indexOf(" ");
    if (space == -1) {
      LOG.error(
        "[{}] server returned unrecognized message: {}",
        session_name,
        response);
      return;
    }

    final String message_session = response.substring(0, space);
    if (!Objects.equals(message_session, session_name)) {
      LOG.trace(
        "[{}] ignored message intended for another client",
        session_name);
      return;
    }

    /*
     * The message was intended for this client. Try to parse it as one
     * of the available message types.
     */

    final String text = response.substring(space).trim();

    final Matcher error_matcher = PATTERN_ERROR.matcher(text);
    if (error_matcher.matches()) {
      final String message = error_matcher.group(1);
      LOG.error("[{}] server returned an error: {}", session_name, message);
      this.failed = true;
      return;
    }

    final Matcher connect_matcher = PATTERN_CONNECT.matcher(text);
    if (connect_matcher.matches()) {
      final int port_data =
        Integer.parseUnsignedInt(connect_matcher.group(1));
      final int port_control =
        Integer.parseUnsignedInt(connect_matcher.group(2));
      final int session_crypted =
        Integer.parseUnsignedInt(connect_matcher.group(3), 16);

      LOG.debug(
        "[{}] connect {} {} (encrypted {})",
        session_name,
        Integer.valueOf(port_data),
        Integer.valueOf(port_control),
        Integer.valueOf(session_crypted));
      this.remote_control_port = port_control;
      this.remote_data_port = port_data;
      this.remote_session = this.duologue_key ^ session_crypted;
      this.remote_ports_received = true;
      return;
    }

    LOG.error(
      "[{}] server returned unrecognized message: {}",
      session_name,
      text);
  }

  private Publication setupAllClientsPublication()
    throws EchoClientTimedOutException
  {
    final ConcurrentPublication publication =
      EchoChannels.createPublication(
        this.aeron,
        this.configuration.remoteAddress(),
        this.configuration.remoteInitialPort(),
        ECHO_STREAM_ID);

    for (int index = 0; index < 1000; ++index) {
      if (publication.isConnected()) {
        LOG.debug("initial publication connected");
        return publication;
      }

      try {
        Thread.sleep(10L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    publication.close();
    throw new EchoClientTimedOutException("Making initial publication to server");
  }

  private Subscription setupAllClientsSubscription()
    throws EchoClientTimedOutException
  {
    final Subscription subscription =
      EchoChannels.createSubscriptionDynamicMDC(
        this.aeron,
        this.configuration.remoteAddress(),
        this.configuration.remoteInitialControlPort(),
        ECHO_STREAM_ID);

    for (int index = 0; index < 1000; ++index) {
      if (subscription.isConnected() && subscription.imageCount() > 0) {
        LOG.debug("initial subscription connected");
        return subscription;
      }

      try {
        Thread.sleep(10L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    subscription.close();
    throw new EchoClientTimedOutException(
      "Making initial subscription to server");
  }

  @Override
  public void close()
  {
    this.aeron.close();
    this.media_driver.close();
  }
}
