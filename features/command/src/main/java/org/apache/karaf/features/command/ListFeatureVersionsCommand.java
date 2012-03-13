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
import java.util.Arrays;
import java.util.List;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

@Command(scope = "feature", name = "version-list", description = "Lists all versions of a feature available from the currently available repositories.")
public class ListFeatureVersionsCommand extends FeaturesCommandSupport {

	@Argument(index = 0, name = "feature", description = "Name of feature.", required = true, multiValued = false)
	String feature;

    private static final String VERSION = "Version";
    private static final String REPOSITORY = "Repository";
    private static final String REPOSITORY_URL = "Repository URL";

    private class VersionInRepository { 
    	public String version;
    	public Repository repository;
    }
    
    protected void doExecute(FeaturesService admin) throws Exception {

        List<VersionInRepository> versionsInRepositories = new ArrayList<VersionInRepository>();
        
        for (Repository r : Arrays.asList(admin.listRepositories())) {
            for (Feature f : r.getFeatures()) {

                if (f.getName().equals(feature)) { 
                	VersionInRepository versionInRepository = new VersionInRepository();
                	versionInRepository.repository = r;
                	versionInRepository.version = f.getVersion();
                	versionsInRepositories.add(versionInRepository);
                }
            }
        }

    	
        if (versionsInRepositories.size() == 0) {
            System.out.println("No versions available for features '" + feature + "'");
            return;
        }

        // Print column headers.
        int maxVersionSize = VERSION.length();
        for (VersionInRepository vir : versionsInRepositories) {
            maxVersionSize = Math.max(maxVersionSize, vir.version.length());
        }
        int maxRepositorySize = REPOSITORY.length();
        for (VersionInRepository vir : versionsInRepositories) {
        	maxRepositorySize = Math.max(maxRepositorySize, vir.repository.getName().length());
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(VERSION).append("   ");
        for (int i = VERSION.length(); i < maxVersionSize; i++) {
            sb.append(" ");
        }
        sb.append(REPOSITORY).append(" ");
        for (int i = REPOSITORY.length(); i < maxRepositorySize; i++) {
            sb.append(" ");
        }
        sb.append(" ");
        sb.append(REPOSITORY_URL);
        System.out.println(sb.toString());

        // Print the version data.
        for (VersionInRepository vir : versionsInRepositories) {

            sb.setLength(0);


            sb.append("[");
            String str = vir.version;
            sb.append(str);
            for (int i = str.length(); i < maxVersionSize; i++) {
                sb.append(" ");
            }
            sb.append("] ");

            str = vir.repository.getName();
            sb.append(str);
            for (int i = str.length(); i < maxRepositorySize; i++) {
                sb.append(" ");
            }

            sb.append(" ");
            sb.append(vir.repository.getURI());
            System.out.println(sb.toString());
        }

    }

}
