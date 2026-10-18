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
package org.apache.karaf.services.web.internal.command;

import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.WebService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "web", name = "servlet-list", description = "List the servlets and the resources served by the web container.")
@Service
public class ServletListCommand implements Action {

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Reference
    private WebService webService;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Context");
        table.column("Name");
        table.column("Patterns");
        table.column("Class");
        table.column("Bundle").alignRight();
        table.column("Service").alignRight();
        table.column("State");
        table.column("Message");
        for (WebElement servlet : webService.getServlets()) {
            table.addRow().addContent(Elements.path(servlet), Elements.text(servlet.getName()),
                    String.join(", ", servlet.getPatterns()), Elements.text(servlet.getClassName()),
                    Elements.bundle(servlet), Elements.service(servlet), servlet.getState(),
                    Elements.text(servlet.getMessage()));
        }
        table.print(System.out, !noFormat);
        return null;
    }

}
