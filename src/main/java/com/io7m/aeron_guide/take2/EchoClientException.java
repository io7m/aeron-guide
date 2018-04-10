package com.io7m.aeron_guide.take2;

import java.util.Objects;

/**
 * The type of exceptions raised by the client.
 */

public abstract class EchoClientException extends Exception
{
  /**
   * Create an exception.
   *
   * @param message The message
   */

  public EchoClientException(final String message)
  {
    super(Objects.requireNonNull(message, "message"));
  }

  /**
   * Create an exception.
   *
   * @param cause The cause
   */

  public EchoClientException(final Throwable cause)
  {
    super(Objects.requireNonNull(cause, "cause"));
  }
}
