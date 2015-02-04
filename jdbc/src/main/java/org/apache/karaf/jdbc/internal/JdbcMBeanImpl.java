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
package org.apache.karaf.jdbc.internal;

import org.apache.karaf.jdbc.JdbcMBean;
import org.apache.karaf.jdbc.JdbcService;

import javax.management.MBeanException;
import javax.management.openmbean.*;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of the JDBC MBean.
 */
public class JdbcMBeanImpl implements JdbcMBean {

    private JdbcService jdbcService;

    @Override
    public TabularData getDatasources() throws MBeanException {
        try {
            CompositeType type = new CompositeType("DataSource", "JDBC DataSource",
                    new String[]{ "name", "product", "version", "url", "status"},
                    new String[]{ "Name", "Database product", "Database version", "JDBC URL", "Status" },
                    new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING });
            TabularType tableType = new TabularType("JDBC DataSources", "Table of the JDBC DataSources",
                    type, new String[]{ "name" });
            TabularData table = new TabularDataSupport(tableType);

            for (String datasource : jdbcService.datasources()) {
                try {
                    Map<String, String> info = jdbcService.info(datasource);
                    CompositeData data = new CompositeDataSupport(type,
                            new String[]{"name", "product", "version", "url", "status"},
                            new Object[]{datasource, info.get("db.product"), info.get("db.version"), info.get("url"), "OK"});
                    table.put(data);
                } catch (Exception e) {
                    CompositeData data = new CompositeDataSupport(type,
                            new String[]{"name", "product", "version", "url", "status"},
                            new Object[]{datasource, "", "", "", "ERROR"});
                    table.put(data);
                }
            }
            return table;
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    @Override
    public void create(String name, String driverName, String driverClass, String databaseName, String url, String user, String password) throws MBeanException {
        try {
            jdbcService.create(name, driverName, driverClass, databaseName, url, user, password);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    @Override
    public void delete(String name) throws MBeanException {
        try {
            jdbcService.delete(name);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    @Override
    public Map<String, String> info(String datasource) throws MBeanException {
        try {
            return jdbcService.info(datasource);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public TabularData tables(String datasource) throws MBeanException {
        try {
            Map<String, List<String>> result = jdbcService.tables(datasource);
            OpenType[] stringTypes = new OpenType[result.keySet().size()];
            for (int i = 0; i < stringTypes.length; i++) {
                stringTypes[i] = SimpleType.STRING;
            }
            String[] columns = result.keySet().toArray(new String[result.keySet().size()]);

            CompositeType type = new CompositeType("Columns", "Columns",
                    columns, columns, stringTypes);
            TabularType rows = new TabularType("Result", "Result Rows", type, columns);
            TabularData table = new TabularDataSupport(rows);

            int rowCount = result.get(result.keySet().iterator().next()).size();

            for (int i = 0; i < rowCount; i++) {
                Object[] row = new Object[columns.length];
                for (int j = 0; j < columns.length; j++) {
                    row[j] = result.get(columns[j]).get(i);
                }
                CompositeData data = new CompositeDataSupport(type, columns, row);
                table.put(data);
            }

            return table;
        } catch (Exception e) {
            e.printStackTrace();
            throw new MBeanException(null, e.getMessage());
        }
    }

    @Override
    public void execute(String datasource, String command) throws MBeanException {
        try {
            jdbcService.execute(datasource, command);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public TabularData query(String datasource, String query) throws MBeanException {
        try {
            Map<String, List<String>> result = jdbcService.query(datasource, query);
            OpenType[] stringTypes = new OpenType[result.keySet().size()];
            for (int i = 0; i < stringTypes.length; i++) {
                stringTypes[i] = SimpleType.STRING;
            }
            String[] columns = result.keySet().toArray(new String[result.keySet().size()]);

            CompositeType type = new CompositeType("Columns", "Columns",
                    columns, columns, stringTypes);
            TabularType rows = new TabularType("Result", "Result Rows", type, columns);
            TabularData table = new TabularDataSupport(rows);

            int rowCount = result.get(result.keySet().iterator().next()).size();

            for (int i = 0; i < rowCount; i++) {
                Object[] row = new Object[columns.length];
                for (int j = 0; j < columns.length; j++) {
                    row[j] = result.get(columns[j]).get(i);
                }
                CompositeData data = new CompositeDataSupport(type, columns, row);
                table.put(data);
            }

            return table;
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public JdbcService getJdbcService() {
        return jdbcService;
    }

    public void setJdbcService(JdbcService jdbcService) {
        this.jdbcService = jdbcService;
    }

}
