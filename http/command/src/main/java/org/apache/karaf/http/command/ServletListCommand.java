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
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.table.Col;
import org.apache.karaf.shell.table.ShellTable;

@Command(scope = "http", name = "list", description = "Lists details for servlets.")
public class ServletListCommand extends OsgiCommandSupport {

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    private ServletService servletService;
    
    public ServletListCommand(ServletService servletService) {
        this.servletService = servletService;
    }

    @Override
    protected Object doExecute() throws Exception {
        ShellTable table = new ShellTable();
        table.column(new Col("ID"));
        table.column(new Col("Servlet"));
        table.column(new Col("Servlet-Name"));
        table.column(new Col("State"));
        table.column(new Col("Alias"));
        table.column(new Col("Url"));

        for (ServletInfo info : servletService.getServlets()) {
            table.addRow().addContent(info.getBundle().getBundleId(), info.getClassName(), info.getName(),
                                      info.getStateString(), info.getAlias(), Arrays.toString(info.getUrls()));
        }
        table.print(System.out, !noFormat);
        return null;
    }

}
