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

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Command(scope = "obr", name = "start", description = "Deploys and starts a list of bundles using OBR.")
@Service
public class StartCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "bundles", description = "List of bundles to deploy (separated by whitespaces). The bundles are identified using the following syntax: symbolic_name,version where version is optional.", required = true, multiValued = true)
    protected List<String> bundles;

    @Option(name = "-d", aliases = { "--deployOptional" }, description = "Deploy optional bundles", required = false, multiValued = false)
    protected boolean deployOptional = false;

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        doDeploy(admin, bundles, true, deployOptional);
    }

}
