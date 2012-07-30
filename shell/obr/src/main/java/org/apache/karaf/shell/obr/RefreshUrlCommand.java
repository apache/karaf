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
package org.apache.karaf.shell.obr;

import java.util.List;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(scope = "obr", name = "refreshUrl", description = "Reloads the repositories to obtain a fresh list of bundles.")
public class RefreshUrlCommand extends ObrCommandSupport {

    @Option(name = "-i", aliases = { "--index" }, description = "Use index to identify URL", required = false, multiValued = false)
    boolean useIndex;

    @Argument(index = 0, name = "ids", description = "Repository URLs (or indexes if you use -i) to refresh (leave empty for all)", required = false, multiValued = true)
    List<String> ids;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
		if (ids != null && !ids.isEmpty()) {
			for (String id : ids) {
                if (useIndex) {
                    Repository[] repos = admin.listRepositories();
                    int index = Integer.parseInt(id);
                    if (index >= 0 && index < repos.length) {
                        admin.addRepository(repos[index].getURI());
                    } else {
                        System.err.println("Invalid index");
                    }
                } else {
				    admin.addRepository(id);
                }
			}
		} else {
			Repository[] repos = admin.listRepositories();
			if ((repos != null) && (repos.length > 0)) {
				for (int i = 0; i < repos.length; i++) {
					admin.addRepository(repos[i].getURI());
				}
			}
		}
    }

}
