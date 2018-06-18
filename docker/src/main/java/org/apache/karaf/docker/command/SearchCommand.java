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
package org.apache.karaf.docker.command;

import org.apache.karaf.docker.ImageSearch;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "docker", name = "search", description = "Search the Docker Hub for images")
@Service
public class SearchCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "term", description = "Search term", required = true, multiValued = false)
    String term;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Description");
        table.column("Automated");
        table.column("Official");
        table.column("Star Count");
        for (ImageSearch search : getDockerService().search(term, url)) {
            table.addRow().addContent(
                    search.getName(),
                    search.getDescription(),
                    search.isAutomated(),
                    search.isOfficial(),
                    search.getStarCount()
            );
        }
        table.print(System.out);
        return null;
    }

}
