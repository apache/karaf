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
package org.apache.karaf.profile.impl;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.ProfileMBean;
import org.apache.karaf.profile.ProfileService;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.util.*;

import static org.apache.karaf.profile.impl.Utils.join;

public class ProfileMBeanImpl extends StandardMBean implements ProfileMBean {

    private ProfileService profileService;

    public ProfileMBeanImpl() throws NotCompliantMBeanException {
        super(ProfileMBean.class);
    }

    @Override
    public Map<String, String> getProfiles() {
        List<String> ids = new ArrayList<>(profileService.getProfiles());
        HashMap<String, String> results = new HashMap<>();
        Collections.sort(ids);
        for (String id : ids) {
            Profile profile = profileService.getProfile(id);
            if (profile != null) {
                String parents = join(" ", profile.getParentIds());
                results.put(id, parents);
            }
        }
        return results;
    }

    @Override
    public void rename(String name, String newName) {
        Profile profile = ProfileBuilder.Factory.createFrom(profileService.getProfile(name))
                .identity(newName)
                .getProfile();
        profileService.createProfile(profile);
        profileService.deleteProfile(name);
    }

    @Override
    public void delete(String name) {
        profileService.deleteProfile(name);
    }

    @Override
    public void create(String name, List<String> parents) {
        Profile profile = ProfileBuilder.Factory.create(name)
                .setParents(parents)
                .getProfile();
        profileService.createProfile(profile);
    }

    @Override
    public void copy(String source, String target) {
        Profile profile = ProfileBuilder.Factory.createFrom(profileService.getProfile(source))
                .identity(target)
                .getProfile();
        profileService.createProfile(profile);
    }

    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

}
