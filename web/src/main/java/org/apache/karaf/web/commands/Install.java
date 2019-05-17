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
package org.apache.karaf.web.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.web.WebContainerService;

@Command(scope = "web", name = "install", description = "Install a web application")
@Service
public class Install implements Action {

    @Argument(index = 0, name = "location", description = "The web application artifact location", required = true, multiValued = false)
    String location;

    @Argument(index = 1, name = "contextPath", description = "The web context path to bind the web application in the web container", required = true, multiValued = false)
    String contextPath;

    @Reference
    private WebContainerService webContainerService;

    @Override
    public Object execute() throws Exception {
        webContainerService.install(location, contextPath);
        return null;
    }

}
