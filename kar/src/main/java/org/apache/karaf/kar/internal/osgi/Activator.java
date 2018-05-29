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
package org.apache.karaf.kar.internal.osgi;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.kar.KarService;
import org.apache.karaf.kar.internal.KarServiceImpl;
import org.apache.karaf.kar.internal.KarsMBeanImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ManagedService;

import java.io.File;

@Services(
        requires = @RequireService(FeaturesService.class),
        provides = @ProvideService(KarService.class)
)
@Managed("org.apache.karaf.kar")
public class Activator extends BaseActivator implements ManagedService {

    protected void doStart() throws Exception {
        FeaturesService featuresService = getTrackedService(FeaturesService.class);
        if (featuresService == null) {
            return;
        }

        boolean noAutoRefreshBundles = getBoolean("noAutoRefreshBundles", false);
        boolean noAutoStartBundles = getBoolean("noAutoStartBundles", false);
        String karStorage = getString("karStorage", System.getProperty("karaf.data") + File.separator + "kar");

        KarServiceImpl karService = new KarServiceImpl(
                System.getProperty("karaf.base"),
                karStorage,
                featuresService
        );
        karService.setNoAutoRefreshBundles(noAutoRefreshBundles);
        karService.setNoAutoStartBundles(noAutoStartBundles);
        register(KarService.class, karService);

        KarsMBeanImpl mbean = new KarsMBeanImpl();
        mbean.setKarService(karService);
        registerMBean(mbean, "type=kar");
    }

}
