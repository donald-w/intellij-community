/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.lang.UrlClassLoader;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dmitry Avdeev
 */
public class UrlClassLoaderTest extends TestCase {

  public void testBootstrapResources() {
    String name = "com/sun/xml/internal/messaging/saaj/soap/LocalStrings.properties";
    assertNotNull(UrlClassLoaderTest.class.getClassLoader().getResourceAsStream(name));
    assertNull(UrlClassLoader.build().get().getResourceAsStream(name));
    assertNotNull(UrlClassLoader.build().allowBootstrapResources().get().getResourceAsStream(name));
  }

  public void _testConcurrentResourceLoading() throws IOException, ClassNotFoundException, ExecutionException, InterruptedException {
    final List<String> resourceNames = ContainerUtil.newArrayList();
    List<URL> urls = ContainerUtil.newArrayList();

    for (File file : new File(PathManager.getHomePathFor(UrlClassLoader.class) + "/lib").listFiles()) {
      if (file.getName().endsWith(".jar")) {
        urls.add(file.toURI().toURL());

        ZipFile zipFile = new ZipFile(file);
        try {
          Enumeration<? extends ZipEntry> entries = zipFile.entries();
          while (entries.hasMoreElements()) {
            resourceNames.add(entries.nextElement().getName());
          }
        }
        finally {
          zipFile.close();
        }

      }
    }

    int attemptCount = 1000;
    int threadCount = 3;
    final int resourceCount = 20;

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threadCount);
    final Random random = new Random();
    UrlClassLoader.CachePool pool = UrlClassLoader.createCachePool();
    for (int attempt = 0; attempt < attemptCount; attempt++) {
      final UrlClassLoader loader = UrlClassLoader.build().urls(urls).parent(null).
        useCache(pool, new UrlClassLoader.CachingCondition() {
          @Override
          public boolean shouldCacheData(@NotNull URL url) {
            return true; // fails also without cache pool (but with cache enabled), but takes much longer
          }
        }).get();
      //System.out.println("Attempt " + attempt);

      final List<String> namesToLoad = ContainerUtil.newArrayList();
      for (int j = 0; j < resourceCount; j++) {
        namesToLoad.add(resourceNames.get(random.nextInt(resourceNames.size())));
      }

      List<Future> futures = ContainerUtil.newArrayList();
      for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(new Runnable() {
          @Override
          public void run() {
            for (String name : namesToLoad) {
              try {
                assertNotNull(loader.findResource(name));
              }
              catch (Throwable e) {
                System.out.println("Failed loading " + name);
                TimeoutUtil.sleep(100);
                System.out.println("Repeated: " + loader.findResource(name));
                throw new RuntimeException(e);
              }
            }
          }
        }));
      }

      for (Future future : futures) {
        future.get();
      }
    }

  }
}
