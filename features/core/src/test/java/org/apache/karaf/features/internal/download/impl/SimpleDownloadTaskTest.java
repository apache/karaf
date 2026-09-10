/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal.download.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleDownloadTaskTest {

    private Path tempDir;
    private ScheduledExecutorService executorService;
    private String previousKarafData;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("SimpleDownloadTaskTest");
        executorService = new ScheduledThreadPoolExecutor(1);
        previousKarafData = System.getProperty("karaf.data");
        System.setProperty("karaf.data", tempDir.resolve("karaf-data").toString());
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
        if (previousKarafData != null) {
            System.setProperty("karaf.data", previousKarafData);
        } else {
            System.clearProperty("karaf.data");
        }
        deleteRecursively(tempDir);
    }

    // Reproduces the race in gh-2808: two overlapping downloads of the same URL (e.g. two
    // feature installs, each with their own DownloadManager) can start before either has written
    // the destination file. With the old delete-then-renameTo sequence, whichever download loses
    // the race deletes the file the other just wrote, and a concurrent reader can observe the
    // destination transiently missing. Files.move(..., ATOMIC_MOVE) closes that window: the
    // destination is always either the old or new content, never briefly absent.
    @Test
    public void concurrentDownloadsOfSameUrlNeverExposeATransientlyMissingFile() throws Exception {
        File basePath = tempDir.resolve("basePath").toFile();
        basePath.mkdirs();

        File source = tempDir.resolve("source.jar").toFile();
        try (FileOutputStream os = new FileOutputStream(source)) {
            os.write("content".getBytes(StandardCharsets.UTF_8));
        }
        String url = source.toURI().toURL().toString();
        File destination = new File(basePath, Integer.toHexString(new URL(url).toString().hashCode()) + "-source.jar");

        int writers = 6;
        int rounds = 200;
        AtomicInteger missingFileObservations = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(writers + 1);
        try {
            for (int round = 0; round < rounds; round++) {
                Files.deleteIfExists(destination.toPath());

                CountDownLatch ready = new CountDownLatch(writers);
                CountDownLatch go = new CountDownLatch(1);
                CountDownLatch done = new CountDownLatch(writers);
                AtomicBoolean stopReader = new AtomicBoolean(false);

                for (int i = 0; i < writers; i++) {
                    pool.submit(() -> {
                        ready.countDown();
                        awaitUninterruptibly(go);
                        try {
                            new SimpleDownloadTask(executorService, url, basePath).download(null);
                        } catch (Exception ignore) {
                            // An individual attempt failing outright is not the bug under test --
                            // only a *transiently missing* destination file is.
                        } finally {
                            done.countDown();
                        }
                    });
                }
                Future<?> reader = pool.submit(() -> {
                    boolean everSeen = false;
                    while (!stopReader.get()) {
                        boolean exists = destination.exists();
                        if (exists) {
                            everSeen = true;
                        } else if (everSeen) {
                            missingFileObservations.incrementAndGet();
                        }
                    }
                });

                ready.await();
                go.countDown();
                done.await(10, TimeUnit.SECONDS);
                stopReader.set(true);
                reader.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(
                "A concurrent reader observed the destination file transiently missing after it had "
                        + "already been created once in the same round -- the delete-then-rename race "
                        + "from gh-2808",
                0, missingFileObservations.get());
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> p.toFile().delete());
    }
}
