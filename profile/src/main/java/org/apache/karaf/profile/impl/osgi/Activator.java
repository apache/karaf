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
package org.apache.karaf.profile.impl.osgi;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.profile.impl.ProfileMBeanImpl;
import org.apache.karaf.profile.impl.ProfileServiceImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ManagedService;

@Services(
        provides = @ProvideService(ProfileService.class)
)
@Managed("org.apache.karaf.profile")
public class Activator extends BaseActivator implements ManagedService {

    @Override
    protected void doStart() throws Exception {
        Path root = Paths.get(getString("profilesDirectory", System.getProperty("karaf.home") + "/profiles"));
        ProfileServiceImpl service = new ProfileServiceImpl(root);
        register(ProfileService.class, service);

        ProfileMBeanImpl profileMBean = new ProfileMBeanImpl();
        profileMBean.setProfileService(service);
        registerMBean(profileMBean, "type=profile");
    }

}
