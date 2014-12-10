/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.apache.karaf.profile.command;

import java.util.List;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.profile.command.completers.ProfileCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(name = "change-parents", scope = "profile", description = "Replace the profile's parents with the specified list of parents")
@Service
public class ProfileChangeParents implements Action {

    @Argument(index = 0, required = true, name = "profile", description = "Name of the profile.")
    @Completion(ProfileCompleter.class)
    private String profileId;

    @Argument(index = 1, name = "parents", description = "The list of new parent profiles.", required = true, multiValued = true)
    @Completion(ProfileCompleter.class)
    private List<String> parentIds;

    @Reference
    private ProfileService profileService;

    @Override
    public Object execute() throws Exception {
        Profile profile = profileService.getRequiredProfile(profileId);
        Profile newProfile = ProfileBuilder.Factory.createFrom(profile)
                .addParents(parentIds)
                .getProfile();
        profileService.updateProfile(newProfile);
        return null;
    }

}
