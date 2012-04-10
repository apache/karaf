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
package org.apache.karaf.obr.command;

import java.util.List;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.felix.service.command.CommandSession;

@Command(scope = "obr", name = "list", description = "Lists OBR bundles, optionally providing the given packages.")
public class ListCommand implements Action {

    @Argument(index = 0, name = "packages", description = "A list of packages separated by whitespaces.", required = false, multiValued = true)
    List<String> packages;

    RepositoryAdmin repoAdmin;

    public void setRepoAdmin(RepositoryAdmin repoAdmin) {
        this.repoAdmin = repoAdmin;
    }

    @Override
    public Object execute(CommandSession session) throws Exception {
        StringBuilder substr = new StringBuilder();

        if (packages != null) {
            for (String packageName : packages) {
                substr.append(" ");
                substr.append(packageName);
            }
        }

        String query;
        if ((substr == null) || (substr.length() == 0)) {
            query = "(|(presentationname=*)(symbolicname=*))";
        } else {
            query = "(|(presentationname=*" + substr + "*)(symbolicname=*" + substr + "*))";
        }
        Resource[] resources = repoAdmin.discoverResources(query);
        int maxPName = 4;
        int maxSName = 13;
        int maxVersion = 7;
        for (Resource resource : resources) {
            maxPName = Math.max(maxPName, emptyIfNull(resource.getPresentationName()).length());
            maxSName = Math.max(maxSName, emptyIfNull(resource.getSymbolicName()).length());
            maxVersion = Math.max(maxVersion, emptyIfNull(resource.getVersion()).length());
        }

        String formatHeader = "  %-" + maxPName + "s  %-" + maxSName + "s   %-" + maxVersion + "s";
        String formatLine = "[%-" + maxPName + "s] [%-" + maxSName + "s] [%-" + maxVersion + "s]";
        System.out.println(String.format(formatHeader, "NAME", "SYMBOLIC NAME", "VERSION"));
        for (Resource resource : resources) {
            System.out.println(String.format(formatLine, emptyIfNull(resource.getPresentationName()), emptyIfNull(resource.getSymbolicName()), emptyIfNull(resource.getVersion())));
        }

        if (resources == null || resources.length == 0) {
            System.out.println("No matching bundles.");
        }
        return null;
    }

    private String emptyIfNull(Object st) {
        return st == null ? "" : st.toString();
    }

}
