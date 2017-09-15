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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.util.MultiException;
import org.ops4j.pax.url.mvn.MavenResolver;

public class MavenDownloadManager implements DownloadManager {

    protected final MavenResolver mavenResolver;

    protected final ScheduledExecutorService executorService;

    protected final long scheduleDelay;

    protected final int scheduleMaxRun;

    protected File tmpPath;

    private final Map<String, AbstractDownloadTask> downloaded = new HashMap<>();

    private final Map<String, AbstractDownloadTask> downloading = new HashMap<>();

    private final Object lock = new Object();

    private volatile int allPending = 0;

    public MavenDownloadManager(MavenResolver mavenResolver, ScheduledExecutorService executorService,
                                long scheduleDelay, int scheduleMaxRun) {
        this.mavenResolver = mavenResolver;
        this.executorService = executorService;
        this.scheduleDelay = scheduleDelay;
        this.scheduleMaxRun = scheduleMaxRun;

        String karafRoot = System.getProperty("karaf.home", "karaf");
        String karafData = System.getProperty("karaf.data", karafRoot + "/data");
        this.tmpPath = new File(karafData, "tmp");
    }

    public int getPending() {
        return allPending;
    }

    @Override
    public Downloader createDownloader() {
        return new MavenDownloader();
    }

    @SuppressWarnings({
     "unchecked", "rawtypes"
    })
    @Override
    public Map<String, StreamProvider> getProviders() {
        return (Map) Collections.synchronizedMap(downloaded);
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    protected class MavenDownloader implements Downloader {

        private volatile int pending = 0;
        private final MultiException exception = new MultiException("Error");

        public int pending() {
            return pending;
        }

        @Override
        public void await() throws InterruptedException, MultiException {
            synchronized (lock) {
                while (pending != 0) {
                    lock.wait();
                }
            }
            exception.throwIfExceptions();
        }

        @Override
        public void download(final String location, final DownloadCallback downloadCallback) throws MalformedURLException {
            AbstractDownloadTask task;
            synchronized (lock) {
                task = downloaded.get(location);
                if (task == null) {
                    task = downloading.get(location);
                }
            }
            if (task == null) {
                task = createDownloadTask(location);
            }
            synchronized (lock) {
                AbstractDownloadTask prev = downloaded.get(location);
                if (prev == null) {
                    prev = downloading.get(location);
                }
                if (prev == null) {
                    downloading.put(location, task);
                    executorService.execute(task);
                } else {
                    task = prev;
                }
                pending++;
                allPending++;
            }
            final AbstractDownloadTask downloadTask = task;
            task.addListener(future -> {
                try {
                    // Call the callback
                    if (downloadCallback != null) {
                        downloadCallback.downloaded(downloadTask);
                    }
                    // Make sure we log any download error if the callback suppressed it
                    downloadTask.getFile();
                } catch (Throwable e) {
                    exception.addSuppressed(e);
                } finally {
                    synchronized (lock) {
                        downloading.remove(location);
                        downloaded.put(location, downloadTask);
                        --allPending;
                        if (--pending == 0) {
                            lock.notifyAll();
                        }
                    }
                }
            });
        }

        protected AbstractDownloadTask createDownloadTask(String url) {
            AbstractDownloadTask task = doCreateDownloadTask(url);
            if (task instanceof AbstractRetryableDownloadTask) {
                AbstractRetryableDownloadTask rt = (AbstractRetryableDownloadTask) task;
                if (scheduleDelay > 0) {
                    rt.setScheduleDelay(scheduleDelay);
                }
                if (scheduleMaxRun > 0) {
                    rt.setScheduleMaxRun(scheduleMaxRun);
                }
            }
            return task;
        }

        protected AbstractDownloadTask doCreateDownloadTask(final String url) {
            final String mvnUrl = DownloadManagerHelper.stripUrl(url);
            if (mvnUrl.startsWith("mvn:")) {
                if (!mvnUrl.equals(url)) {
                    return new ChainedDownloadTask(executorService, url, mvnUrl);
                } else {
                    return new MavenDownloadTask(executorService, mavenResolver, mvnUrl);
                }
            } else {
                return createCustomDownloadTask(url);
            }
        }

        class ChainedDownloadTask extends AbstractDownloadTask {

            private String innerUrl;

            public ChainedDownloadTask(ScheduledExecutorService executorService, String url, String innerUrl) {
                super(executorService, url);
                this.innerUrl = innerUrl;
            }

            @Override
            public void run() {
                try {
                    MavenDownloader.this.download(innerUrl, provider -> {
                        try {
                            AbstractDownloadTask future = (AbstractDownloadTask) provider;
                            String file = future.getFile().toURI().toURL().toExternalForm();
                            String real = url.replace(innerUrl, file);
                            MavenDownloader.this.download(real, provider1 -> {
                                try {
                                    setFile(provider1.getFile());
                                } catch (IOException e) {
                                    setException(e);
                                } catch (Throwable t) {
                                    setException(new IOException(t));
                                }
                            });
                        } catch (IOException e) {
                            setException(e);
                        } catch (Throwable t) {
                            setException(new IOException(t));
                        }
                    });
                } catch (IOException e) {
                    setException(e);
                } catch (Throwable t) {
                    setException(new IOException(t));
                }
            }

        }

    }

    protected AbstractDownloadTask createCustomDownloadTask(final String url) {
        return new SimpleDownloadTask(executorService, url, tmpPath);
    }

}
