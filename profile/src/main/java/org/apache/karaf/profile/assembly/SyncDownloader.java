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
package org.apache.karaf.profile.assembly;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.karaf.features.internal.download.DownloadCallback;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.util.MultiException;

public class SyncDownloader implements Downloader {

    private final Downloader delegate;
    private final Map<String, DownloadCallback> callbacks = new LinkedHashMap<>();
    private final Map<String, StreamProvider> providers = new HashMap<>();

    public SyncDownloader(Downloader delegate) {
        this.delegate = delegate;
    }

    @Override
    public void await() throws InterruptedException, MultiException {
        delegate.await();
        MultiException exception = new MultiException("Error");
        for (String loc : callbacks.keySet()) {
            try {
                callbacks.get(loc).downloaded(providers.get(loc));
            } catch (Exception e) {
                exception.addSuppressed(e);
            }
        }
        callbacks.clear();
        providers.clear();
        exception.throwIfExceptions();
    }

    @Override
    public void download(String location, DownloadCallback downloadCallback) throws MalformedURLException {
        callbacks.put(location, downloadCallback);
        delegate.download(location, provider -> providers.put(location, provider));
    }
}
