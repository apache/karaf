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
package org.apache.karaf.jdbc.command.ds;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.jdbc.command.JdbcCommandSupport;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;

@Command(scope = "jdbc", name = "ds-factories", description = "List the JDBC DataSourceFactories")
@Service
public class DSFactoriesCommand extends JdbcCommandSupport {

    @Reference
    BundleContext context;
    
    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Class");
        table.column("Version");
        Collection<ServiceReference<DataSourceFactory>> refs = context.getServiceReferences(DataSourceFactory.class, null);
        for (ServiceReference<DataSourceFactory> ref : refs) {
            String driverName = (String)ref.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_NAME);
            String driverClass = (String)ref.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS);
            String driverVersion = (String)ref.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION);
            table.addRow().addContent(driverName, driverClass, driverVersion);
        }
        table.print(System.out);
        return null;
    }

}
