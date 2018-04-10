package com.io7m.aeron_guide.tests;

import com.io7m.aeron_guide.take2.EchoServerSessionAllocationException;
import com.io7m.aeron_guide.take2.EchoServerSessionAllocator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.SecureRandom;

import static org.hamcrest.core.StringContains.containsString;

public final class EchoServerSessionAllocatorTest
{
  @Rule public final ExpectedException expected = ExpectedException.none();

  @Test
  public void testBadRange_0()
  {
    this.expected.expect(IllegalArgumentException.class);
    this.expected.expectMessage(containsString("Maximum"));
    EchoServerSessionAllocator.create(0, -1, new SecureRandom());
  }

  @Test
  public void testAllocateExhausted()
    throws Exception
  {
    final EchoServerSessionAllocator alloc =
      EchoServerSessionAllocator.create(0, 0, new SecureRandom());

    final int port = alloc.allocate();
    Assert.assertTrue(port + " must be >= 0", port >= 0);
    Assert.assertTrue(port + " must be < 1", port < 1);

    this.expected.expect(EchoServerSessionAllocationException.class);
    this.expected.expectMessage("No session IDs left to allocate");
    alloc.allocate();
  }

  @Test
  public void testAllocateMany()
    throws Exception
  {
    final EchoServerSessionAllocator alloc =
      EchoServerSessionAllocator.create(0, 65535, new SecureRandom());

    for (int index = 0; index < 65500; ++index) {
      final int port = alloc.allocate();
      Assert.assertTrue(port + " must be >= 0", port >= 0);
      Assert.assertTrue(port + " must be < 65536", port < 65536);
    }

    this.expected.expect(EchoServerSessionAllocationException.class);
    for (int index = 0; index < 65500; ++index) {
      alloc.allocate();
    }
  }

  @Test
  public void testAllocateFree()
    throws Exception
  {
    final EchoServerSessionAllocator alloc =
      EchoServerSessionAllocator.create(0, 65535, new SecureRandom());

    for (int index = 0; index < 65500; ++index) {
      final int port = alloc.allocate();
      Assert.assertTrue(port + " must be >= 0", port >= 0);
      Assert.assertTrue(port + " must be < 65536", port < 65536);
    }

    for (int index = 0; index < 65536; ++index) {
      alloc.free(index);
    }

    for (int index = 0; index < 65500; ++index) {
      final int port = alloc.allocate();
      Assert.assertTrue(port + " must be >= 0", port >= 0);
      Assert.assertTrue(port + " must be < 65536", port < 65536);
    }
  }
}
