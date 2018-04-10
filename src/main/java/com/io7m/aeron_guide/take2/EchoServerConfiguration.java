package com.io7m.aeron_guide.take2;

import org.immutables.value.Value;

import java.net.InetAddress;
import java.nio.file.Path;

@Value.Immutable
public interface EchoServerConfiguration
{
  /**
   * @return The base directory that will be used for the server; should be unique for each server instance
   */

  @Value.Parameter
  Path baseDirectory();

  /**
   * @return The local address to which the server will bind
   */

  @Value.Parameter
  InetAddress localAddress();

  /**
   * @return The port that the server will use for client introductions
   */

  @Value.Parameter
  int localInitialPort();

  /**
   * @return The dynamic MDC control port that the server will use for client introductions
   */

  @Value.Parameter
  int localInitialControlPort();

  /**
   * @return The base port that will be used for individual client duologues
   */

  @Value.Parameter
  int localClientsBasePort();

  /**
   * @return The maximum number of clients that will be allowed on the server
   */

  @Value.Parameter
  int clientMaximumCount();

  /**
   * @return The maximum number of duologues that will be allowed per remote client IP address
   */

  @Value.Parameter
  int maximumConnectionsPerAddress();
}
