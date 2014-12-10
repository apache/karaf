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


import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.profile.command.completers.ProfileCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(name = "rename", scope = "profile", description = "Rename the specified source profile")
@Service
public class ProfileRename implements Action {

    @Option(name = "--version", description = "The profile version to rename. Defaults to the current default version.")
    private String versionId;

    @Option(name = "-f", aliases = "--force", description = "Flag to allow replacing the target profile (if exists).")
    private boolean force;

    @Argument(index = 0, required = true, name = "profile name", description = "Name of the profile.")
    @Completion(ProfileCompleter.class)
    private String profileName;

    @Argument(index = 1, required = true, name = "new profile name", description = "New name of the profile.")
    private String newName;

    @Reference
    private ProfileService profileService;

    @Override
    public Object execute() throws Exception {
        Profile profile = ProfileBuilder.Factory.createFrom(profileService.getProfile(profileName))
                .identity(newName)
                .getProfile();
        profileService.createProfile(profile);
        profileService.deleteProfile(profileName);
        return null;
    }

}
