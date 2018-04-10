package com.io7m.aeron_guide.tests;

import com.io7m.aeron_guide.take2.EchoServerPortAllocationException;
import com.io7m.aeron_guide.take2.EchoServerPortAllocator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.core.StringContains.containsString;

public final class EchoServerPortAllocatorTest
{
  @Rule public final ExpectedException expected = ExpectedException.none();

  @Test
  public void testBadPortRange_0()
  {
    this.expected.expect(IllegalArgumentException.class);
    this.expected.expectMessage(containsString("Base port"));
    EchoServerPortAllocator.create(0, 1000);
  }

  @Test
  public void testBadPortRange_1()
  {
    this.expected.expect(IllegalArgumentException.class);
    this.expected.expectMessage(containsString("Base port"));
    EchoServerPortAllocator.create(65536, 1000);
  }

  @Test
  public void testBadPortRange_2()
  {
    this.expected.expect(IllegalArgumentException.class);
    this.expected.expectMessage(containsString("Uppermost port"));
    EchoServerPortAllocator.create(65530, 1000);
  }

  @Test
  public void testBadPortRange_3()
  {
    this.expected.expect(IllegalArgumentException.class);
    this.expected.expectMessage(containsString("Uppermost port"));
    EchoServerPortAllocator.create(1, -1000);
  }

  @Test
  public void testAllocateExhausted()
    throws Exception
  {
    final EchoServerPortAllocator alloc =
      EchoServerPortAllocator.create(1000, 8);

    final int[] ports = alloc.allocate(8);
    for (final int port : ports) {
      Assert.assertTrue(port + " must be >= 1000", port >= 1000);
      Assert.assertTrue(port + " must be < 1016", port < 1008);
    }

    this.expected.expect(EchoServerPortAllocationException.class);
    alloc.allocate(1);
  }

  @Test
  public void testAllocateFree()
    throws Exception
  {
    final EchoServerPortAllocator alloc =
      EchoServerPortAllocator.create(1000, 8);

    {
      final int[] ports = alloc.allocate(8);
      for (final int port : ports) {
        Assert.assertTrue(port + " must be >= 1000", port >= 1000);
        Assert.assertTrue(port + " must be < 1016", port < 1008);
      }

      for (final int port : ports) {
        alloc.free(port);
      }
    }

    {
      final int[] ports = alloc.allocate(8);
      for (final int port : ports) {
        Assert.assertTrue(port + " must be >= 1000", port >= 1000);
        Assert.assertTrue(port + " must be < 1016", port < 1008);
      }
    }
  }
}
