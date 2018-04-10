package com.io7m.aeron_guide.take2;

import org.immutables.value.Value;

import java.net.InetAddress;
import java.nio.file.Path;

/**
 * Configuration values for the client.
 */

@Value.Immutable
public interface EchoClientConfiguration
{
  /**
   * @return The base directory that will be used for the client; should be unique for each client instance
   */

  @Value.Parameter
  Path baseDirectory();

  /**
   * @return The address of the server
   */

  @Value.Parameter
  InetAddress remoteAddress();

  /**
   * @return The server's data port
   */

  @Value.Parameter
  int remoteInitialPort();

  /**
   * @return The server's control port
   */

  @Value.Parameter
  int remoteInitialControlPort();
}
