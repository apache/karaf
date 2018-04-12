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
package org.apache.karaf.profile.command;

import java.util.List;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(name = "create", scope = "profile", description = "Create a new profile with the specified name and parents", detailedDescription = "classpath:profileCreate.txt")
@Service
public class ProfileCreate implements Action {

    @Option(name = "--parents", multiValued = true, required = false, description = "Optionally specifies one or multiple parent profiles. To specify multiple parent profiles, specify this flag multiple times on the command line. For example, --parents foo --parents bar.")
    private List<String> parents;
    @Argument(index = 0)
    private String profileId;
    @Reference
    private ProfileService profileService;

    @Override
    public Object execute() throws Exception {
        Profile profile = ProfileBuilder.Factory.create(profileId)
                .setParents(parents)
                .getProfile();
		profileService.createProfile(profile);
        return null;
    }

}
