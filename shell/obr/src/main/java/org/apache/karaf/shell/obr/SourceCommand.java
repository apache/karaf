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

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.obr.util.FileUtil;

@Command(scope = "obr", name = "source", description = "Downloads the sources for an OBR bundle.")
public class SourceCommand extends ObrCommandSupport {

    @Option(name = "-x", aliases = {}, description = "Extract the archive", required = false, multiValued = false)
    boolean extract;

    @Argument(index = 0, name = "folder", description = "Local folder for storing sources", required = true, multiValued = false)
    String localDir;

    @Argument(index = 1, name = "bundles", description = "List of bundles to download the sources for", required = true, multiValued = true)
    List<String> bundles;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        for (String bundle : bundles) {
            String[] target = getTarget(bundle);
            Resource resource = selectNewestVersion(searchRepository(admin, target[0], target[1]));
            if (resource == null)
            {
                System.err.println("Unknown bundle and/or version: " + target[0]);
            }
            else
            {
                URI srcURL = (URI) resource.getProperties().get(Resource.SOURCE_URI);
                if (srcURL != null)
                {
                    FileUtil.downloadSource(System.out, System.err, srcURL.toURL(), localDir, extract);
                }
                else
                {
                    System.err.println("Missing source URL: " + target[0]);
                }
            }
        }
    }

}
