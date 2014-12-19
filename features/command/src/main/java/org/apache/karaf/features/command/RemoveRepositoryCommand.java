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
package org.apache.karaf.features.command;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

@Command(scope = "features", name = "removeRepository", description = "Removes the specified repository features service.")
public class RemoveRepositoryCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "repositories", description = "Name of the repositories to remove.", required = true, multiValued = false)
    private List<String> repositories;

    protected void doExecute(FeaturesService admin) throws Exception {
        ArrayList<Repository> repositoriesToRemove = new ArrayList<Repository>();
        for (String repository : repositories) {
            Pattern pattern = Pattern.compile(repository);
            for (Repository r : admin.listRepositories()) {
                Matcher matcher = pattern.matcher(r.getName());
                if (matcher.matches()) {
                    repositoriesToRemove.add(r);
                }
            }
        }

        for (Repository r : repositoriesToRemove) {
            System.out.println("Removing repository " + r.getName() + " (" + r.getURI() + ")");
            try {
                admin.removeRepository(r.getURI());
            } catch (Exception e) {
                System.err.println("Can't remove repository " + r.getName() + " (" + r.getURI() + "): " + e.getMessage());
            }
        }

    }
}
