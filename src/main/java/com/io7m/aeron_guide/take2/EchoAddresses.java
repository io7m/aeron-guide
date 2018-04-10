package com.io7m.aeron_guide.take2;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * Functions to parse addresses.
 */

public final class EchoAddresses
{
  private EchoAddresses()
  {

  }

  /**
   * Extract an IP address from the given string of the form "ip:port", where
   * {@code ip} may be an IPv4 or IPv6 address, and {@code port} is an unsigned
   * integer port value.
   *
   * @param text The text
   *
   * @return An IP address
   *
   * @throws IllegalArgumentException If the input is unparseable
   */

  public static InetAddress extractAddress(
    final String text)
    throws IllegalArgumentException
  {
    try {
      final URI uri = new URI("fake://" + text);
      return InetAddress.getByName(uri.getHost());
    } catch (final URISyntaxException | UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
