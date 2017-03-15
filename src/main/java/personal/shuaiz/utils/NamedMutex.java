package personal.shuaiz.utils;

import com.sun.jna.Platform;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.TimeUnit;

/**
 * A synchronization primitive that can also be used for inter-process synchronization.
 */
public abstract class NamedMutex implements AutoCloseable {
  /**
   * Create a platform specified implementation of NamedMutex.
   *
   * @param name
   *     the name of the mutex
   * @return A platform specified implementation of NamedMutex
   *
   * @throws Exception
   *     Native errors
   */
  public static NamedMutex newInstance(String name) throws Exception {
    if (Platform.isWindows()) {
      return new NamedMutexWindowsImpl(name);
    } else if (Platform.isLinux()) {
      throw new NotImplementedException();
    } else {
      throw new NotImplementedException();
    }
  }

  /**
   * Create a platform specified implementation of NamedMutex.
   *
   * @param initiallyOwned
   *     indicates whether the calling thread should have initial ownership of the mutex
   * @param name
   *     the name of the mutex
   * @return A platform specified implementation of NamedMutex
   *
   * @throws Exception
   *     Native errors
   */
  public static NamedMutex newInstance(boolean initiallyOwned, String name) throws Exception {
    if (Platform.isWindows()) {
      return new NamedMutexWindowsImpl(initiallyOwned, name);
    } else if (Platform.isLinux()) {
      throw new NotImplementedException();
    } else {
      throw new NotImplementedException();
    }
  }

  /**
   * Blocks the current thread until the current mutex receives a signal.
   *
   * @return true if the current instance receives a signal. If the current instance is never
   * signaled, WaitOne never returns.
   */
  public abstract boolean waitOne() throws Exception;

  /**
   * Blocks the current thread until the current mutex receives a signal or the waiting interval
   * arrives.
   *
   * @param interval
   *     The interval to wait
   * @param intervalTimeUnit
   *     The time unit of interval
   * @return true if the current instance receives a signal; otherwise, false.
   */
  public abstract boolean waitOne(long interval, TimeUnit intervalTimeUnit) throws Exception;

  /**
   * Release the Mutex object once.
   */
  public abstract void release() throws Exception;

  /**
   * Release all resources used by current instance.
   *
   * @throws Exception
   *     Native errors when cleanup
   */
  @Override
  public abstract void close() throws Exception;

  @Override
  @SuppressWarnings("checkstyle:nofinalizer")
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}
