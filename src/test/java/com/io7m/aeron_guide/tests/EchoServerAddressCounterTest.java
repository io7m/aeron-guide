package com.io7m.aeron_guide.tests;

import com.io7m.aeron_guide.take2.EchoServerAddressCounter;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;

public final class EchoServerAddressCounterTest
{
  @Test
  public void testCounter()
    throws Exception
  {
    final EchoServerAddressCounter counter =
      EchoServerAddressCounter.create();

    final InetAddress addr_0 = InetAddress.getByName("127.0.0.1");

    Assert.assertEquals(0L, (long) counter.countFor(addr_0));
    counter.increment(addr_0);
    Assert.assertEquals(1L, (long) counter.countFor(addr_0));
    counter.increment(addr_0);
    Assert.assertEquals(2L, (long) counter.countFor(addr_0));
    counter.decrement(addr_0);
    Assert.assertEquals(1L, (long) counter.countFor(addr_0));
    counter.decrement(addr_0);
    Assert.assertEquals(0L, (long) counter.countFor(addr_0));
    counter.decrement(addr_0);
    Assert.assertEquals(0L, (long) counter.countFor(addr_0));
  }
}
