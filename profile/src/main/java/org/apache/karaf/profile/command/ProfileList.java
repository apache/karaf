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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import static org.apache.karaf.profile.impl.Utils.join;


@Command(name = "list", scope = "profile", description = "Lists all profiles")
@Service
public class ProfileList implements Action {

    @Option(name = "--hidden", description = "Display hidden profiles")
    private boolean hidden;

    @Reference
    private ProfileService profileService;

    @Override
    public Object execute() throws Exception {
        List<String> ids = new ArrayList<>(profileService.getProfiles());
        Collections.sort(ids);
        ShellTable table = new ShellTable();
        table.column("id");
        table.column("parents");
        for (String id : ids) {
            Profile profile = profileService.getProfile(id);
            if (profile != null && (hidden || !profile.isHidden())) {
                String parents = join(" ", profile.getParentIds());
                table.addRow().addContent(id, parents);
            }
        }
        table.print(System.out);
        return null;
    }

}
