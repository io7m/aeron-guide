package com.io7m.aeron_guide.take2;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A counter for IP addresses.
 */

public final class EchoServerAddressCounter
{
  private final Map<InetAddress, Integer> counts;

  private EchoServerAddressCounter()
  {
    this.counts = new HashMap<>();
  }

  /**
   * Create a new counter.
   *
   * @return A new counter
   */

  public static EchoServerAddressCounter create()
  {
    return new EchoServerAddressCounter();
  }

  /**
   * @param address The IP address
   *
   * @return The current count for the given address
   */

  public int countFor(
    final InetAddress address)
  {
    Objects.requireNonNull(address, "address");

    if (this.counts.containsKey(address)) {
      return this.counts.get(address).intValue();
    }

    return 0;
  }

  /**
   * Increment the count for the given address.
   *
   * @param address The IP address
   *
   * @return The current count for the given address
   */

  public int increment(
    final InetAddress address)
  {
    Objects.requireNonNull(address, "address");

    if (this.counts.containsKey(address)) {
      final int next = this.counts.get(address).intValue() + 1;
      this.counts.put(address, Integer.valueOf(next));
      return next;
    }

    this.counts.put(address, Integer.valueOf(1));
    return 1;
  }

  /**
   * Decrement the count for the given address.
   *
   * @param address The IP address
   *
   * @return The current count for the given address
   */

  public int decrement(
    final InetAddress address)
  {
    Objects.requireNonNull(address, "address");

    if (this.counts.containsKey(address)) {
      final int next = this.counts.get(address).intValue() - 1;
      if (next <= 0) {
        this.counts.remove(address);
        return 0;
      }

      this.counts.put(address, Integer.valueOf(next));
      return next;
    }

    return 0;
  }
}
