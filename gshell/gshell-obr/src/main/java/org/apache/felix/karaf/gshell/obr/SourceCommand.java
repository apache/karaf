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
package org.apache.felix.karaf.gshell.obr;

import java.net.URL;
import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;

public class SourceCommand extends ObrCommandSupport {

    @Option(name = "-x", description = "Extract")
    boolean extract;

    @Argument(required = true, index = 0, description = "Local directory")
    String localDir;

    @Argument(required = true, index = 1, multiValued = true, description = "List of bundles")
    List<String> bundles;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        for (String bundle : bundles) {
            String[] target = getTarget(bundle);
            Resource resource = selectNewestVersion(searchRepository(admin, target[0], target[1]));
            if (resource == null)
            {
                io.err.println("Unknown bundle and/or version: " + target[0]);
            }
            else
            {
                URL srcURL = (URL) resource.getProperties().get(Resource.SOURCE_URL);
                if (srcURL != null)
                {
                    FileUtil.downloadSource(io.out, io.err, srcURL, localDir, extract);
                }
                else
                {
                    io.err.println("Missing source URL: " + target[0]);
                }
            }
        }
    }

}
