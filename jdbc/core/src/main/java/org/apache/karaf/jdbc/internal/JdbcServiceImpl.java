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

import org.apache.karaf.jdbc.JdbcService;
import org.apache.karaf.util.TemplateUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Default implementation of the JDBC Service.
 */
public class JdbcServiceImpl implements JdbcService {

    public static enum TYPES {
        DB2("wrap:mvn:com.ibm.db2.jdbc/db2jcc/", "9.7", "datasource-db2.xml"),
        DERBY("mvn:org.apache.derby/derby/", "10.8.2.2", "datasource-derby.xml"),
        GENERIC(null, null, "datasource-generic.xml"),
        H2("mvn:com.h2database/h2/", "1.3.163", "datasource-h2.xml"),
        HSQL("mvn:org.hsqldb/hsqldb/", "2.3.2", "datasource-hsql.xml"),
        MYSQL("mvn:mysql/mysql-connector-java/", "5.1.18", "datasource-mysql.xml"),
        ORACLE("wrap:mvn:ojdbc/ojdbc/", "11.2.0.2.0", "datasource-oracle.xml"),
        POSTGRES("wrap:mvn:postgresql/postgresql/", "9.1-901.jdbc4", "datasource-postgres.xml");

        private final String bundleUrl;
        private final String defaultVersion;
        private final String templateFile;

        TYPES(String bundleUrl, String defaultVersion, String templateFile) {
            this.bundleUrl = bundleUrl;
            this.defaultVersion = defaultVersion;
            this.templateFile = templateFile;
        }

        public void installBundle(BundleContext bundleContext, String version) throws Exception {
            String location = this.bundleUrl + getWithDefault(version, this.defaultVersion);
            bundleContext.installBundle(location, null).start();
        }

        private String getWithDefault(String st, String defaultSt) {
            return (st == null)? defaultSt : st;
        }

        public void copyDataSourceFile(File outFile, HashMap<String, String> properties) {
            InputStream is = this.getClass().getResourceAsStream(templateFile);
            if (is == null) {
                throw new IllegalArgumentException("Template resource " + templateFile + " doesn't exist");
            }
            TemplateUtils.createFromTemplate(outFile, is, properties);
        }

    }

    private BundleContext bundleContext;

    @Override
    public void create(String name, String type, String driverClassName, String version, String url, String user, String password, boolean tryToInstallBundles) throws Exception {
        if (type == null) {
            throw new IllegalStateException("No database type supplied");
        }
        TYPES dbType = TYPES.valueOf(type.toUpperCase());

        if (tryToInstallBundles) {
            dbType.installBundle(bundleContext, version);
        }

        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");
        File outFile = new File(deployFolder, "datasource-" + name + ".xml");

        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("name", name);
        properties.put("driver", driverClassName);
        properties.put("url", url);
        properties.put("user", user);
        properties.put("password", password);

        dbType.copyDataSourceFile(outFile, properties);
    }

    @Override
    public void delete(String name) throws Exception {
        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");
        File datasourceFile = new File(deployFolder, "datasource-" + name + ".xml");
        if (!datasourceFile.exists()) {
            throw new IllegalArgumentException("The JDBC datasource file "+ datasourceFile.getPath() + " doesn't exist");
        }
        datasourceFile.delete();
    }

    @Override
    public List<String> datasources() throws Exception {
        List<String> datasources = new ArrayList<String>();

        ServiceReference<?>[] references = bundleContext.getServiceReferences((String) null, "(|(" + Constants.OBJECTCLASS + "=" + DataSource.class.getName() + ")("
        + Constants.OBJECTCLASS + "=" + XADataSource.class.getName() + "))");
        if (references != null) {
            for (ServiceReference reference : references) {
                if (reference.getProperty("osgi.jndi.service.name") != null) {
                    datasources.add((String) reference.getProperty("osgi.jndi.service.name"));
                } else if (reference.getProperty("datasource") != null) {
                    datasources.add((String) reference.getProperty("datasource"));
                } else if (reference.getProperty("name") != null) {
                    datasources.add((String) reference.getProperty("name"));
                } else {
                    datasources.add(reference.getProperty(Constants.SERVICE_ID).toString());
                }
            }
        }
        return datasources;
    }

    @Override
    public List<String> datasourceFileNames() throws Exception {
        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");

        String[] datasourceFileNames = deployFolder.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("datasource-") && name.endsWith(".xml");
            }
        });

        return Arrays.asList(datasourceFileNames);
    }

    @Override
    public Map<String, List<String>> query(String datasource, String query) throws Exception {
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, datasource);
        try {
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            Statement statement = jdbcConnector.createStatement();
            ResultSet resultSet = jdbcConnector.register(statement.executeQuery(query));
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int c = 1; c <= metaData.getColumnCount(); c++) {
                map.put(metaData.getColumnLabel(c), new ArrayList<String>());
            }
            while (resultSet.next()) {
                for (int c = 1; c <= metaData.getColumnCount(); c++) {
                    map.get(metaData.getColumnLabel(c)).add(resultSet.getString(c));
                }
            }
            return map;
        } finally {
            jdbcConnector.close();
        }
    }

    @Override
    public void execute(String datasource, String command) throws Exception {
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, datasource);
        try {
            jdbcConnector.createStatement().execute(command);
        } finally {
            jdbcConnector.close();
        }
    }

    @Override
    public Map<String, List<String>> tables(String datasource) throws Exception {
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, datasource);
        try {

            DatabaseMetaData dbMetaData = jdbcConnector.connect().getMetaData();
            ResultSet resultSet = jdbcConnector.register(dbMetaData.getTables(null, null, null, null));
            ResultSetMetaData metaData = resultSet.getMetaData();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            for (int c = 1; c <= metaData.getColumnCount(); c++) {
                map.put(metaData.getColumnLabel(c), new ArrayList<String>());
            }
            while (resultSet.next()) {
                for (int c = 1; c <= metaData.getColumnCount(); c++) {
                    map.get(metaData.getColumnLabel(c)).add(resultSet.getString(c));
                }
            }
            return map;
        } finally {
            jdbcConnector.close();
        }
    }

    @Override
    public Map<String, String> info(String datasource) throws Exception {
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, datasource);
        try {
            DatabaseMetaData dbMetaData = jdbcConnector.connect().getMetaData();
            Map<String, String> map = new HashMap<String, String>();
            map.put("db.product", dbMetaData.getDatabaseProductName());
            map.put("db.version", dbMetaData.getDatabaseProductVersion());
            map.put("url", dbMetaData.getURL());
            map.put("username", dbMetaData.getUserName());
            map.put("driver.name", dbMetaData.getDriverName());
            map.put("driver.version", dbMetaData.getDriverVersion());
            return map;
        } finally {
            jdbcConnector.close();
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
