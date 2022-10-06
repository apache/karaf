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
package org.apache.karaf.kar.command;

import org.apache.karaf.kar.KarService;
import org.apache.karaf.kar.command.completers.KarCompleter;
import org.apache.karaf.shell.api.action.*;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "kar", name = "uninstall", description = "Uninstall a KAR file.")
@Service
public class UninstallKarCommand implements Action {

    @Argument(index = 0, name = "name", description = "The name of the KAR file to uninstall.", required = true, multiValued = false)
    @Completion(KarCompleter.class)
    private String name;

    @Option(name = "--no-refresh", description = "Do not refresh the bundles automatically", required = false, multiValued = false)
    private boolean noAutoRefreshBundle = false;

    @Reference
    private KarService karService;

    @Override
    public Object execute() throws Exception {
        karService.uninstall(name, noAutoRefreshBundle);
        return null;
    }
    
}
