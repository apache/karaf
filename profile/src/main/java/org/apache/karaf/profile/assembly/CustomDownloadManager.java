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

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.impl.AbstractDownloadTask;
import org.apache.karaf.features.internal.download.impl.MavenDownloadManager;
import org.apache.karaf.profile.Profile;
import org.ops4j.pax.url.mvn.MavenResolver;

public class CustomDownloadManager extends MavenDownloadManager {

    private final Profile profile;
    private final Map<String, String> translatedUrls;

    public CustomDownloadManager(MavenResolver resolver, ScheduledExecutorService executor) {
        this(resolver, executor, null, null);
    }

    public CustomDownloadManager(MavenResolver resolver, ScheduledExecutorService executor, Profile profile) {
        this(resolver, executor, profile, null);
    }

    public CustomDownloadManager(MavenResolver resolver, ScheduledExecutorService executor, Profile profile, Map<String, String> translatedUrls) {
        super(resolver, executor, 0, 1);
        this.profile = profile;
        this.translatedUrls = translatedUrls;
    }

    @Override
    protected AbstractDownloadTask createCustomDownloadTask(String url) {
        return new CustomSimpleDownloadTask(executorService, profile, url);
    }

    @Override
    public Downloader createDownloader() {
        return new CustomMavenDownloader();
    }

    class CustomMavenDownloader extends MavenDownloader {
        protected AbstractDownloadTask createDownloadTask(String url) {
            if (translatedUrls != null && translatedUrls.containsKey(url)) {
                url = translatedUrls.get(url);
            }
            return super.createDownloadTask(url);
        }
    }


}
