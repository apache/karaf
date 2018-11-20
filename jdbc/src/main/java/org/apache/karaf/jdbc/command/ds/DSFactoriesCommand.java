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
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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

    private Comparator<DataSourceFactoryInfo> comparator = new DataSourceFactoryComparator();
    
    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Class");
        table.column("Version");
        table.column("Registration bundle");

        Set<DataSourceFactoryInfo> factories = new TreeSet<>(comparator);

        Collection<ServiceReference<DataSourceFactory>> refs = context.getServiceReferences(DataSourceFactory.class, null);
        for (ServiceReference<DataSourceFactory> ref : refs) {
            DataSourceFactoryInfo info = new DataSourceFactoryInfo();
            info.driverName = (String)ref.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_NAME);
            info.driverClass = (String)ref.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS);
            info.driverVersion = (String)ref.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION);
            if (ref.getBundle() != null && ref.getBundle().getSymbolicName() != null) {
                info.bundle = String.format("%s [%s]",
                        ref.getBundle().getSymbolicName(),
                        ref.getBundle().getBundleId());
            } else {
                info.bundle = "";
            }
            factories.add(info);
        }
        for (DataSourceFactoryInfo info : factories) {
            table.addRow().addContent(info.driverName, info.driverClass, info.driverVersion, info.bundle);
        }

        table.print(System.out);
        return null;
    }

    private static class DataSourceFactoryComparator implements Comparator<DataSourceFactoryInfo> {
        @Override
        public int compare(DataSourceFactoryInfo dsf1, DataSourceFactoryInfo dsf2) {
            int r1 = 0;
            int r2 = 0;
            int r3 = 0;

            if (dsf1.bundle != null || dsf2.bundle != null) {
                if (dsf1.bundle == null) {
                    r1 = -1;
                } else if (dsf2.bundle == null) {
                    r1 = 1;
                } else {
                    r1 = dsf1.bundle.compareTo(dsf2.bundle);
                }
            }
            if (dsf1.driverName != null || dsf2.driverName != null) {
                if (dsf1.driverName == null) {
                    r2 = -1;
                } else if (dsf2.driverName == null) {
                    r2 = 1;
                } else {
                    r2 = dsf1.driverName.compareTo(dsf2.driverName);
                }
            }
            if (dsf1.driverClass != null || dsf2.driverClass != null) {
                if (dsf1.driverClass == null) {
                    r3 = -1;
                } else if (dsf2.driverClass == null) {
                    r3 = 1;
                } else {
                    r3 = dsf1.driverClass.compareTo(dsf2.driverClass);
                }
            }

            return r1 == 0 ? (r2 == 0 ? r3 : r2) : r1;
        }
    }

    private static class DataSourceFactoryInfo {
        public String driverName;
        public String driverClass;
        public String driverVersion;
        public String bundle;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataSourceFactoryInfo that = (DataSourceFactoryInfo) o;
            return Objects.equals(driverName, that.driverName) &&
                    Objects.equals(driverClass, that.driverClass) &&
                    Objects.equals(driverVersion, that.driverVersion) &&
                    Objects.equals(bundle, that.bundle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(driverName, driverClass, driverVersion, bundle);
        }
    }

}
