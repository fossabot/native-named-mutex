package io.github.hcoona.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NamedMutexTest {

  @Test
  public void testCannotWaitOne() throws Exception {
    final String name = "/test_named-mutex_cannot-wait-one";
    try (NamedMutex ignored = NamedMutex.newInstance(true, name)) {
      ExecutorService executor = Executors.newSingleThreadExecutor();

      Future<Boolean> waitOneFuture = executor.submit(() -> {
        try (NamedMutex mutex2 = NamedMutex.newInstance(true, name)) {
          return mutex2.waitOne(500, TimeUnit.MILLISECONDS);
        }
      });

      Assert.assertFalse(waitOneFuture.get());
    }
  }

  @Test
  public void testCanWaitOne() throws Exception {
    final String name = "/test_named-mutex_can-wait-one";
    try (NamedMutex mutex_owned = NamedMutex.newInstance(true, name)) {
      mutex_owned.release();
      ExecutorService executor = Executors.newSingleThreadExecutor();

      Future<Boolean> waitOneFuture = executor.submit(() -> {
        try (NamedMutex mutex2 = NamedMutex.newInstance(true, name)) {
          return mutex2.waitOne(5, TimeUnit.SECONDS);
        }
      });

      Assert.assertTrue(waitOneFuture.get());
    } catch (PosixErrorException e) {
      System.err.println("Error code = " + e.getErrorCode());
    }
  }

}
