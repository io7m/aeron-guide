package com.io7m.aeron_guide.tests;

import io.aeron.Aeron;
import io.aeron.ConcurrentPublication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;

public final class EchoTestPacking
{
  private EchoTestPacking()
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

    final ConcurrentPublication pub1 =
      aeron.addPublication("aeron:udp?endpoint=127.0.0.1:9001|session-id=1000", 23);

    final Subscription sub1 =
      aeron.addSubscription("aeron:udp?endpoint=127.0.0.1:9001|session=1000", 23);

    final UnsafeBuffer buffer =
      new UnsafeBuffer(BufferUtil.allocateDirectAligned(1024, 16));

    int sequence = 0;
    while (true) {
      for (int index = 0; index < 2000; ++index) {
        final int bytes =
          buffer.putStringUtf8(
            0,
            String.format("%08x", Integer.valueOf(sequence)));

        pub1.offer(buffer, 0, bytes);
        ++sequence;
      }

      sub1.poll((data, offset, length, header) -> {
        System.out.println("sub1: " + length);
      }, 3000);

      System.out.println("--");

      try {
        Thread.sleep(1000L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
