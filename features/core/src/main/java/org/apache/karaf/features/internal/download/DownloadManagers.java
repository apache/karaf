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
package org.apache.karaf.features.internal.download;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.features.internal.download.impl.MavenDownloadManager;
import org.ops4j.pax.url.mvn.MavenResolver;

public final class DownloadManagers {

    private DownloadManagers() { }

    public static DownloadManager createDownloadManager(MavenResolver resolver, ScheduledExecutorService executorService) {
        return createDownloadManager(resolver, executorService, 0, 0);
    }

    public static DownloadManager createDownloadManager(MavenResolver resolver, ScheduledExecutorService executorService,
                                                        long scheduleDelay, int scheduleMaxRun) {
        return new MavenDownloadManager(resolver, executorService, scheduleDelay, scheduleMaxRun);
    }
}
