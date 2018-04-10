package com.io7m.aeron_guide.tests;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ConcurrentPublication;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;

import static java.lang.Boolean.TRUE;

public final class EchoTestSessions
{
  private EchoTestSessions()
  {

  }

  public static void main(final String[] args)
  {
    final MediaDriver.Context media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .aeronDirectoryName("/tmp/echotest");

    final Aeron.Context aeron_context =
      new Aeron.Context()
        .aeronDirectoryName("/tmp/echotest");

    final MediaDriver media_driver =
      MediaDriver.launch(media_context);

    final Aeron aeron =
      Aeron.connect(aeron_context);

    final String pub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint("127.0.0.1:9000")
        .sessionId(Integer.valueOf(1000))
        .build();

    final String sub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint("127.0.0.1:9001")
        .sessionId(Integer.valueOf(1000))
        .build();

    final String pub2_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint("127.0.0.1:9002")
        .sessionId(Integer.valueOf(1000))
        .build();

    try (final Subscription sub = aeron.addSubscription(sub_uri, 23)) {
      try (final ConcurrentPublication pub = aeron.addPublication(pub_uri, 23)) {
        aeron.addPublication(pub2_uri, 23);

        final UnsafeBuffer buffer =
          new UnsafeBuffer(BufferUtil.allocateDirectAligned(4, 16));

        while (true) {
          pub.offer(buffer, 0, 4);

          sub.poll(new FragmentAssembler((buffer1, offset, length, header) -> {
            System.out.println(length);
          }), 10);

          try {
            Thread.sleep(1000L);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }
}
