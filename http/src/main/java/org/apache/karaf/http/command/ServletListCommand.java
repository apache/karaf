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
package org.apache.karaf.http.command;

import java.util.Arrays;

import org.apache.karaf.http.core.ServletInfo;
import org.apache.karaf.http.core.ServletService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "http", name = "list", description = "Lists details for servlets.")
@Service
public class ServletListCommand implements Action {

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Reference
    private ServletService servletService;
    
    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column(new Col("ID"));
        table.column(new Col("Servlet"));
        table.column(new Col("Servlet-Name"));
        table.column(new Col("State"));
        table.column(new Col("Alias"));
        table.column(new Col("Url"));

        for (ServletInfo info : servletService.getServlets()) {
            table.addRow().addContent(info.getBundleId(), info.getClassName(), info.getName(),
                                      info.getStateString(), info.getAlias(), Arrays.toString(info.getUrls()));
        }
        table.print(System.out, !noFormat);
        return null;
    }

    public void setServletService(ServletService servletService) {
        this.servletService = servletService;
    }
}
