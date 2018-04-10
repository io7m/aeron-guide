package com.io7m.aeron_guide.take2;

/**
 * A session could not be allocated.
 */

public final class EchoServerSessionAllocationException
  extends EchoServerException
{
  /**
   * Create an exception.
   *
   * @param message The message
   */

  public EchoServerSessionAllocationException(
    final String message)
  {
    super(message);
  }
}
