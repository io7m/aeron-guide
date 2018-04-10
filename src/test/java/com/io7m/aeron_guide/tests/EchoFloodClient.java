package com.io7m.aeron_guide.tests;

import com.io7m.aeron_guide.take2.EchoClient;
import com.io7m.aeron_guide.take2.ImmutableEchoClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EchoFloodClient
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EchoFloodClient.class);

  private EchoFloodClient()
  {

  }

  public static void main(
    final String[] args)
  {
    final ExecutorService exec = Executors.newFixedThreadPool(8);
    for (int index = 0; index < 8; ++index) {
      final int f_index = index;

      exec.submit(() -> {
        Thread.currentThread().setUncaughtExceptionHandler(
          (t, e) -> LOG.error("uncaught exception: ", e));

        final Path directory = Paths.get("/tmp/aeron-client-" + f_index);
        final InetAddress remote_address = InetAddress.getByName("127.0.0.1");

        final ImmutableEchoClientConfiguration configuration =
          ImmutableEchoClientConfiguration.builder()
            .baseDirectory(directory)
            .remoteAddress(remote_address)
            .remoteInitialControlPort(9001)
            .remoteInitialPort(9000)
            .build();

        try (final EchoClient client = EchoClient.create(configuration)) {
          client.run();
        } catch (final Exception e) {
          LOG.error("run error: ", e);
        }
        return null;
      });
    }

    try {
      Thread.sleep(1000L);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    exec.shutdown();
  }
}
