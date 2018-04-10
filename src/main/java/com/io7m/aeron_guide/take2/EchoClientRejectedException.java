package com.io7m.aeron_guide.take2;

/**
 * The server rejected this client when it tried to connect.
 */

public final class EchoClientRejectedException extends EchoClientException
{
  /**
   * Create an exception.
   *
   * @param message The message
   */

  public EchoClientRejectedException(final String message)
  {
    super(message);
  }
}
