package com.io7m.aeron_guide.scratchpad;

import io.aeron.Aeron;
import io.aeron.ConcurrentPublication;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.Header;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class ExampleMDC1
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ExampleMDC1.class);

  private ExampleMDC1()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final MediaDriver.Context server_media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .aeronDirectoryName("/home/someone/var/tmp/aeron/example-server-0");

    final Aeron.Context server_aeron_context =
      new Aeron.Context()
        .aeronDirectoryName("/home/someone/var/tmp/aeron/example-server-0");

    final MediaDriver server_media_driver =
      MediaDriver.launch(server_media_context);

    final Aeron server_aeron =
      Aeron.connect(server_aeron_context);

    final MediaDriver.Context client_media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .aeronDirectoryName("/home/someone/var/tmp/aeron/example-client-0");

    final Aeron.Context client_aeron_context =
      new Aeron.Context()
        .aeronDirectoryName("/home/someone/var/tmp/aeron/example-client-0");

    final MediaDriver client_media_driver =
      MediaDriver.launch(client_media_context);

    final Aeron client_aeron =
      Aeron.connect(client_aeron_context);

    final ConcurrentPublication server_pub =
      server_aeron.addPublication(
        "aeron:udp?control=127.0.0.3:9001|endpoint=127.0.0.3:9000",
        23);

    final Subscription client_sub =
      client_aeron.addSubscription(
        "aeron:udp?control=127.0.0.3:9001|control-mode=dynamic|endpoint=127.0.0.2:8000",
        23,
        image -> LOG.debug("client: i have connected"),
        image -> LOG.debug("client: i have disconnected"));

    final Subscription server_sub =
      server_aeron.addSubscription(
        "aeron:udp?endpoint=127.0.0.3:9000",
        23,
        image -> LOG.debug("server: a client has connected"),
        image -> LOG.debug("server: a client has disconnected"));

    final ConcurrentPublication client_pub =
      client_aeron.addPublication(
        "aeron:udp?endpoint=127.0.0.3:9000",
        23);

    final UnsafeBuffer buffer =
      new UnsafeBuffer(BufferUtil.allocateDirectAligned(2048, 16));

    while (true) {
      if (server_pub.isConnected()) {
        sendMessage(server_pub, buffer, "server " + nowTimestamp());
      }
      if (client_pub.isConnected()) {
        sendMessage(client_pub, buffer, "client " + nowTimestamp());
      }

      readMessages("client" , client_sub);
      readMessages("server", server_sub);

      Thread.sleep(1000L);
    }
  }

  private static void readMessages(
    final String name,
    final Subscription sub)
    throws InterruptedException
  {
    if (sub.isConnected()) {
      for (int index = 0; index < 10; ++index) {
        sub.poll((buffer, offset, length, header) -> {
          onMessage(name, buffer, offset, length, header);
        }, 10);
        Thread.sleep(100L);
      }
    }
  }

  private static String nowTimestamp()
  {
    return ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
  }

  private static void onMessage(
    final String name,
    final DirectBuffer buffer,
    final int offset,
    final int length,
    final Header header)
  {
    final byte[] buf = new byte[length];
    buffer.getBytes(offset, buf);
    final String message = new String(buf, UTF_8);
    LOG.debug(
      "onMessage: {}: [0x{}] {}",
      name,
      Integer.toUnsignedString(header.sessionId()),
      message);
  }

  private static boolean sendMessage(
    final Publication pub,
    final UnsafeBuffer buffer,
    final String text)
  {
    LOG.debug(
      "send: [session 0x{}] {}",
      Integer.toUnsignedString(pub.sessionId()),
      text);

    final byte[] value = text.getBytes(UTF_8);
    buffer.putBytes(0, value);

    long result = 0L;
    for (int index = 0; index < 5; ++index) {
      result = pub.offer(buffer, 0, text.length());
      if (result < 0L) {
        try {
          Thread.sleep(100L);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        continue;
      }
      return true;
    }

    LOG.error("could not send: {}", Long.valueOf(result));
    return false;
  }
}
