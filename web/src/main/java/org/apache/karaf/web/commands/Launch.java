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

import java.awt.Desktop;
import java.net.URI;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.web.WebContainerService;

@Command(scope = "web", name = "launch", description = "Start the web application in a browser of the given bundle ID.")
@Service
public class Launch implements Action {

    @Argument(index = 0, name = "id", description = "The bundle ID to start the browser with", required = true, multiValued = false)
    Long id;

    @Option(name = "--base", description = "The base URL to browse to, otherwise default localhost:8181 will be used.", required = false)
    String baseUrl = null; 
    
    @Reference
    private WebContainerService webContainerService;

    public void setWebContainerService(WebContainerService webContainerService) {
        this.webContainerService = webContainerService;
    }

    @Override
    public Object execute() throws Exception {
        String webContextPath = webContainerService.getWebContextPath(id);
        
        if (baseUrl == null)
        	baseUrl = "http://localhost:8181";
        
        if (!webContextPath.startsWith("/"))
        	webContextPath = "/"+webContextPath;
        
        String uri = baseUrl + webContextPath;
        
        Desktop.getDesktop().browse(new URI(uri));
        
        return null;
    }
    
}
