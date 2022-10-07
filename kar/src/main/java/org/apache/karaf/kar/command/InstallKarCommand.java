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
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.UriCompleter;

import java.net.URI;

@Command(scope = "kar", name = "install", description = "Installs a KAR file.")
@Service
public class InstallKarCommand implements Action {

    @Argument(index = 0, name = "url", description = "The URL of the KAR file to install.", required = true, multiValued = false)
    @Completion(UriCompleter.class)
    private String url;

    @Option(name = "--no-start", description = "Do not start the bundles automatically", required = false, multiValued = false)
    private boolean noAutoStartBundle = false;

    @Option(name = "--no-refresh", description = "Do not refresh the bundles automatically", required = false, multiValued = false)
    private boolean noAutoRefreshBundle = false;

    @Reference
    private KarService karService;

    public Object execute() throws Exception {
        karService.install(new URI(url), noAutoStartBundle, noAutoRefreshBundle);
        return null;
    }
    
}
