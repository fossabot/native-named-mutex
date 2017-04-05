package io.github.hcoona.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class NamedMutexFileLockImpl extends NamedMutex {
  private static final ConcurrentHashMap<String, ReentrantLock> INTERNAL_LOCK_CACHE =
      new ConcurrentHashMap<>();

  private final FileChannel lockFileChannel;
  private final ScheduledExecutorService executor;
  private FileLock lock;
  private final ReentrantLock internal_lock;
  private boolean disposed = false;

  @VisibleForTesting
  NamedMutexFileLockImpl(String name) throws IOException {
    this(false, name);
  }

  @VisibleForTesting
  NamedMutexFileLockImpl(boolean initiallyOwned, String name) throws IOException {
    Path lockFilePath = Paths.get(System.getProperty("java.io.tmpdir"), name + ".lock");
    this.executor = Executors.newSingleThreadScheduledExecutor();

    if (!INTERNAL_LOCK_CACHE.containsKey(name)) {
      INTERNAL_LOCK_CACHE.putIfAbsent(name, new ReentrantLock());
    }
    this.internal_lock = INTERNAL_LOCK_CACHE.get(name);

    this.lockFileChannel = FileChannel.open(lockFilePath,
        StandardOpenOption.CREATE,
        StandardOpenOption.READ,
        StandardOpenOption.WRITE);
    if (internal_lock.tryLock()) {
      try {
        this.lock = lockFileChannel.tryLock();
      } finally {
        if (lock == null || !lock.isValid()) {
          internal_lock.unlock();
        }
      }
    }
  }

  @Override
  public boolean waitOne() throws Exception {
    Preconditions.checkState(!disposed);

    internal_lock.lockInterruptibly();
    try {
      if (lock == null) {
        lock = lockFileChannel.lock();
      } else if (!lock.isValid()) {
        lock = lockFileChannel.lock();
      } else {
        internal_lock.unlock();
      }

      return true;
    } finally {
      if (lock == null || !lock.isValid()) {
        internal_lock.unlock();
      }
    }
  }

  @Override
  public boolean waitOne(long interval, TimeUnit intervalTimeUnit) throws Exception {
    Preconditions.checkState(!disposed);
    Preconditions.checkArgument(interval >= -1);

    if (internal_lock.tryLock(interval, intervalTimeUnit)) {
      try {
        if (lock != null && lock.isValid()) {
          internal_lock.unlock();
          return true;
        } else {
          ScheduledFuture<FileLock> lockFuture = executor.schedule(
              (Callable<FileLock>) lockFileChannel::lock,
              interval, intervalTimeUnit);

          try {
            lock = lockFuture.get(interval, intervalTimeUnit);
            return lock.isValid();
          } catch (TimeoutException ignored) {
            lockFuture.cancel(true);
            return false;
          }
        }
      } finally {
        if (lock == null || !lock.isValid()) {
          internal_lock.unlock();
        }
      }
    } else {
      return false;
    }
  }

  @Override
  public void release() throws Exception {
    Preconditions.checkState(!disposed);

    if (internal_lock.tryLock()) {
      try {
        if (lock != null && lock.isValid()) {
          lock.release();
          internal_lock.unlock();
        }
      } finally {
        internal_lock.unlock();
      }
    }
  }

  @Override
  public void close() throws Exception {
    if (!disposed) {
      release();
      lockFileChannel.close();
      disposed = true;
    }
  }
}
