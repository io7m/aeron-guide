package com.io7m.aeron_guide.take2;

import java.util.concurrent.Executor;

/**
 * A simple executor service. {@link Runnable} values are executed in the order
 * that they are submitted on a single <i>executor thread</i>.
 */

public interface EchoServerExecutorService extends AutoCloseable, Executor
{
  /**
   * @return {@code true} if the caller of this method is running on the executor thread
   */

  boolean isExecutorThread();

  /**
   * Raise {@link IllegalStateException} iff {@link #isExecutorThread()} would
   * currently return {@code false}.
   */

  default void assertIsExecutorThread()
  {
    if (!this.isExecutorThread()) {
      throw new IllegalStateException(
        "The current thread is not a server executor thread");
    }
  }
}
