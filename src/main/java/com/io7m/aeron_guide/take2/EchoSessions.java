package com.io7m.aeron_guide.take2;

/**
 * Known session numbers.
 */

public final class EchoSessions
{
  /**
   * The inclusive lower bound of the reserved sessions range.
   */

  public static final int RESERVED_SESSION_ID_LOW = 1;

  /**
   * The inclusive upper bound of the reserved sessions range.
   */

  public static final int RESERVED_SESSION_ID_HIGH = 2147483647;

  private EchoSessions()
  {

  }
}
