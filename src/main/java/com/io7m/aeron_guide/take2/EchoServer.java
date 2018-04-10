package com.io7m.aeron_guide.take2;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
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
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mindlessly simple Echo server.
 */

public final class EchoServer implements Closeable
{
  /**
   * The stream used for echo messages.
   */

  public static final int ECHO_STREAM_ID;

  private static final Logger LOG = LoggerFactory.getLogger(EchoServer.class);

  private static final Pattern PATTERN_HELLO =
    Pattern.compile("^HELLO ([0-9A-F]+)$");

  static {
    ECHO_STREAM_ID = 0x2044f002;
  }

  private final MediaDriver media_driver;
  private final Aeron aeron;
  private final EchoServerExecutorService executor;
  private final ClientState clients;
  private final EchoServerConfiguration configuration;

  private EchoServer(
    final Clock in_clock,
    final EchoServerExecutorService in_exec,
    final MediaDriver in_media_driver,
    final Aeron in_aeron,
    final EchoServerConfiguration in_config)
  {
    this.executor =
      Objects.requireNonNull(in_exec, "executor");
    this.media_driver =
      Objects.requireNonNull(in_media_driver, "media_driver");
    this.aeron =
      Objects.requireNonNull(in_aeron, "aeron");
    this.configuration =
      Objects.requireNonNull(in_config, "configuration");

    this.clients =
      new ClientState(
        this.aeron,
        Objects.requireNonNull(in_clock, "clock"),
        this.executor,
        this.configuration);
  }

  /**
   * Create a new server.
   *
   * @param clock         A clock used for internal operations involving time
   * @param configuration The server configuration
   *
   * @return A new server
   *
   * @throws EchoServerException On any initialization error
   */

  public static EchoServer create(
    final Clock clock,
    final EchoServerConfiguration configuration)
    throws EchoServerException
  {
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(configuration, "configuration");

    final String directory =
      configuration.baseDirectory().toAbsolutePath().toString();

    final MediaDriver.Context media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .publicationReservedSessionIdLow(EchoSessions.RESERVED_SESSION_ID_LOW)
        .publicationReservedSessionIdHigh(EchoSessions.RESERVED_SESSION_ID_HIGH)
        .aeronDirectoryName(directory);

    final Aeron.Context aeron_context =
      new Aeron.Context()
        .aeronDirectoryName(directory);

    EchoServerExecutorService exec = null;
    try {
      exec = EchoServerExecutor.create();

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

        return new EchoServer(clock, exec, media_driver, aeron, configuration);
      } catch (final Exception e) {
        closeIfNotNull(media_driver);
        throw e;
      }
    } catch (final Exception e) {
      try {
        closeIfNotNull(exec);
      } catch (final Exception c_ex) {
        e.addSuppressed(c_ex);
      }
      throw new EchoServerCreationException(e);
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

  private static String connectMessage(
    final String session_name,
    final int port_data,
    final int port_control,
    final String session)
  {
    return new StringBuilder(64)
      .append(session_name)
      .append(" CONNECT ")
      .append(port_data)
      .append(" ")
      .append(port_control)
      .append(" ")
      .append(session)
      .toString();
  }

  private static String errorMessage(
    final String session_name,
    final String message)
  {
    return new StringBuilder(64)
      .append(session_name)
      .append(" ERROR ")
      .append(message)
      .toString();
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
    if (args.length < 6) {
      LOG.error(
        "usage: directory local-address local-initial-data-port local-initial-control-port local-clients-base-port client-count");
      System.exit(1);
    }

    final Path directory = Paths.get(args[0]);
    final InetAddress local_address = InetAddress.getByName(args[1]);
    final int local_initial_data_port = Integer.parseUnsignedInt(args[2]);
    final int local_initial_control_port = Integer.parseUnsignedInt(args[3]);
    final int local_clients_base_port = Integer.parseUnsignedInt(args[4]);
    final int client_count = Integer.parseUnsignedInt(args[5]);

    final EchoServerConfiguration config =
      ImmutableEchoServerConfiguration.builder()
        .baseDirectory(directory)
        .localAddress(local_address)
        .localInitialPort(local_initial_data_port)
        .localInitialControlPort(local_initial_control_port)
        .localClientsBasePort(local_clients_base_port)
        .clientMaximumCount(client_count)
        .maximumConnectionsPerAddress(3)
        .build();

    try (final EchoServer server = create(Clock.systemUTC(), config)) {
      server.run();
    }
  }

  /**
   * Run the server, returning when the server is finished.
   */

  public void run()
  {
    try (final Publication publication = this.setupAllClientsPublication()) {
      try (final Subscription subscription = this.setupAllClientsSubscription()) {

        final FragmentHandler handler =
          new FragmentAssembler(
            (buffer, offset, length, header) ->
              this.onInitialClientMessage(
                publication,
                buffer,
                offset,
                length,
                header));

        while (true) {
          this.executor.execute(() -> {
            subscription.poll(handler, 100);
            this.clients.poll();
          });

          try {
            Thread.sleep(100L);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }

  private void onInitialClientMessage(
    final Publication publication,
    final DirectBuffer buffer,
    final int offset,
    final int length,
    final Header header)
  {
    final String message =
      EchoMessages.parseMessageUTF8(buffer, offset, length);

    final String session_name =
      Integer.toString(header.sessionId());
    final Integer session_boxed =
      Integer.valueOf(header.sessionId());

    this.executor.execute(() -> {
      try {
        this.clients.onInitialClientMessageProcess(
          publication,
          session_name,
          session_boxed,
          message);
      } catch (final Exception e) {
        LOG.error("could not process client message: ", e);
      }
    });
  }

  /**
   * Configure the publication for the "all-clients" channel.
   */

  private Publication setupAllClientsPublication()
  {
    return EchoChannels.createPublicationDynamicMDC(
      this.aeron,
      this.configuration.localAddress(),
      this.configuration.localInitialControlPort(),
      ECHO_STREAM_ID);
  }

  /**
   * Configure the subscription for the "all-clients" channel.
   */

  private Subscription setupAllClientsSubscription()
  {
    return EchoChannels.createSubscriptionWithHandlers(
      this.aeron,
      this.configuration.localAddress(),
      this.configuration.localInitialPort(),
      ECHO_STREAM_ID,
      this::onInitialClientConnected,
      this::onInitialClientDisconnected);
  }

  private void onInitialClientConnected(
    final Image image)
  {
    this.executor.execute(() -> {
      LOG.debug(
        "[{}] initial client connected ({})",
        Integer.toString(image.sessionId()),
        image.sourceIdentity());

      this.clients.onInitialClientConnected(
        image.sessionId(),
        EchoAddresses.extractAddress(image.sourceIdentity()));
    });
  }

  private void onInitialClientDisconnected(
    final Image image)
  {
    this.executor.execute(() -> {
      LOG.debug(
        "[{}] initial client disconnected ({})",
        Integer.toString(image.sessionId()),
        image.sourceIdentity());

      this.clients.onInitialClientDisconnected(image.sessionId());
    });
  }

  @Override
  public void close()
  {
    this.aeron.close();
    this.media_driver.close();
  }

  private static final class ClientState
  {
    private final Map<Integer, InetAddress> client_session_addresses;
    private final Map<Integer, EchoServerDuologue> client_duologues;
    private final EchoServerPortAllocator port_allocator;
    private final Aeron aeron;
    private final Clock clock;
    private final EchoServerConfiguration configuration;
    private final UnsafeBuffer send_buffer;
    private final EchoServerExecutorService exec;
    private final EchoServerAddressCounter address_counter;
    private final EchoServerSessionAllocator session_allocator;

    ClientState(
      final Aeron in_aeron,
      final Clock in_clock,
      final EchoServerExecutorService in_exec,
      final EchoServerConfiguration in_configuration)
    {
      this.aeron =
        Objects.requireNonNull(in_aeron, "Aeron");
      this.clock =
        Objects.requireNonNull(in_clock, "Clock");
      this.exec =
        Objects.requireNonNull(in_exec, "Executor");
      this.configuration =
        Objects.requireNonNull(in_configuration, "Configuration");

      this.client_duologues = new HashMap<>(32);
      this.client_session_addresses = new HashMap<>(32);

      this.port_allocator =
        EchoServerPortAllocator.create(
          this.configuration.localClientsBasePort(),
          2 * this.configuration.clientMaximumCount());

      this.address_counter =
        EchoServerAddressCounter.create();

      this.session_allocator =
        EchoServerSessionAllocator.create(
          EchoSessions.RESERVED_SESSION_ID_LOW,
          EchoSessions.RESERVED_SESSION_ID_HIGH,
          new SecureRandom());

      this.send_buffer =
        new UnsafeBuffer(BufferUtil.allocateDirectAligned(1024, 16));
    }

    void onInitialClientMessageProcess(
      final Publication publication,
      final String session_name,
      final Integer session_boxed,
      final String message)
      throws EchoServerException, IOException
    {
      this.exec.assertIsExecutorThread();

      LOG.debug("[{}] received: {}", session_name, message);

      /*
       * The HELLO command is the only acceptable message from clients
       * on the all-clients channel.
       */

      final Matcher hello_matcher = PATTERN_HELLO.matcher(message);
      if (!hello_matcher.matches()) {
        EchoMessages.sendMessage(
          publication,
          this.send_buffer,
          errorMessage(session_name, "bad message"));
        return;
      }

      /*
       * Check to see if there are already too many clients connected.
       */

      if (this.client_duologues.size() >= this.configuration.clientMaximumCount()) {
        LOG.debug("server is full");
        EchoMessages.sendMessage(
          publication,
          this.send_buffer,
          errorMessage(session_name, "server full"));
        return;
      }

      /*
       * Check to see if this IP address already has the maximum number of
       * duologues allocated to it.
       */

      final InetAddress owner =
        this.client_session_addresses.get(session_boxed);

      if (this.address_counter.countFor(owner) >=
        this.configuration.maximumConnectionsPerAddress()) {
        LOG.debug("too many connections for IP address");
        EchoMessages.sendMessage(
          publication,
          this.send_buffer,
          errorMessage(session_name, "too many connections for IP address"));
        return;
      }

      /*
       * Parse the one-time pad with which the client wants the server to
       * encrypt the identifier of the session that will be created.
       */

      final int duologue_key =
        Integer.parseUnsignedInt(hello_matcher.group(1), 16);

      /*
       * Allocate a new duologue, encrypt the resulting session ID, and send
       * a message to the client telling it where to find the new duologue.
       */

      final EchoServerDuologue duologue =
        this.allocateNewDuologue(session_name, session_boxed, owner);

      final String session_crypt =
        Integer.toUnsignedString(duologue_key ^ duologue.session(), 16)
          .toUpperCase();

      EchoMessages.sendMessage(
        publication,
        this.send_buffer,
        connectMessage(
          session_name,
          duologue.portData(),
          duologue.portControl(),
          session_crypt));
    }

    private EchoServerDuologue allocateNewDuologue(
      final String session_name,
      final Integer session_boxed,
      final InetAddress owner)
      throws
      EchoServerPortAllocationException,
      EchoServerSessionAllocationException
    {
      this.address_counter.increment(owner);

      final EchoServerDuologue duologue;
      try {
        final int[] ports = this.port_allocator.allocate(2);
        try {
          final int session = this.session_allocator.allocate();
          try {
            duologue =
              EchoServerDuologue.create(
                this.aeron,
                this.clock,
                this.exec,
                this.configuration.localAddress(),
                owner,
                session,
                ports[0],
                ports[1]);
            LOG.debug("[{}] created new duologue", session_name);
            this.client_duologues.put(session_boxed, duologue);
          } catch (final Exception e) {
            this.session_allocator.free(session);
            throw e;
          }
        } catch (final EchoServerSessionAllocationException e) {
          this.port_allocator.free(ports[0]);
          this.port_allocator.free(ports[1]);
          throw e;
        }
      } catch (final EchoServerPortAllocationException e) {
        this.address_counter.decrement(owner);
        throw e;
      }
      return duologue;
    }

    void onInitialClientDisconnected(
      final int session_id)
    {
      this.exec.assertIsExecutorThread();

      this.client_session_addresses.remove(Integer.valueOf(session_id));
    }

    void onInitialClientConnected(
      final int session_id,
      final InetAddress client_address)
    {
      this.exec.assertIsExecutorThread();

      this.client_session_addresses.put(
        Integer.valueOf(session_id), client_address);
    }

    public void poll()
    {
      this.exec.assertIsExecutorThread();

      final Iterator<Map.Entry<Integer, EchoServerDuologue>> iter =
        this.client_duologues.entrySet().iterator();

      /*
       * Get the current time; used to expire duologues.
       */

      final Instant now = this.clock.instant();

      while (iter.hasNext()) {
        final Map.Entry<Integer, EchoServerDuologue> entry = iter.next();
        final EchoServerDuologue duologue = entry.getValue();

        final String session_name =
          Integer.toString(entry.getKey().intValue());

        /*
         * If the duologue has either been closed, or has expired, it needs
         * to be deleted.
         */

        boolean delete = false;
        if (duologue.isExpired(now)) {
          LOG.debug("[{}] duologue expired", session_name);
          delete = true;
        }

        if (duologue.isClosed()) {
          LOG.debug("[{}] duologue closed", session_name);
          delete = true;
        }

        if (delete) {
          try {
            duologue.close();
          } finally {
            LOG.debug("[{}] deleted duologue", session_name);
            iter.remove();
            this.port_allocator.free(duologue.portData());
            this.port_allocator.free(duologue.portControl());
            this.address_counter.decrement(duologue.ownerAddress());
          }
          continue;
        }

        /*
         * Otherwise, poll the duologue for activity.
         */

        duologue.poll();
      }
    }
  }
}
