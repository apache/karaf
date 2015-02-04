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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.karaf.jdbc.JdbcService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the JDBC Service.
 */
public class JdbcServiceImpl implements JdbcService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcServiceImpl.class);

    private BundleContext bundleContext;
    private ConfigurationAdmin configAdmin;
    
    @Override
    public void create(String name, String driverName, String driverClass, String databaseName, String url, String user, String password) throws Exception {
        if (driverName == null && driverClass == null) {
            throw new IllegalStateException("No driverName or driverClass supplied");
        }
        if (datasources().contains(name)) {
            throw new IllegalArgumentException("There is already a DataSource with the name " + name);
        }
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(DataSourceFactory.JDBC_DATASOURCE_NAME, name);
        if (driverName != null) {
            properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, driverName);
        }
        if (driverClass != null) {
            properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, driverClass);
        }
        if (databaseName != null) {
            properties.put(DataSourceFactory.JDBC_DATABASE_NAME, databaseName);
        }
        if (url != null) {
            properties.put(DataSourceFactory.JDBC_URL, url);
        }
        if (user != null) {
            properties.put(DataSourceFactory.JDBC_USER, user);
        }
        if (password != null) {
            properties.put(DataSourceFactory.JDBC_PASSWORD, password);
        }
        Configuration config = configAdmin.createFactoryConfiguration("org.ops4j.datasource", null);
        config.update(properties);
    }

    @Override
    public void delete(String name) throws Exception {
        String filter = String.format("(%s=%s)", DataSourceFactory.JDBC_DATASOURCE_NAME, name);
        Configuration[] configs = configAdmin.listConfigurations(filter);
        for (Configuration config : configs) {
            config.delete();
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<String> datasources() throws Exception {
        List<String> datasources = new ArrayList<>();
        Collection<ServiceReference<DataSource>> references = bundleContext.getServiceReferences(DataSource.class, null);
        if (references == null) {
            return datasources;
        }
        for (ServiceReference reference : references) {
            String dsName = (String)reference.getProperty(DataSourceFactory.JDBC_DATASOURCE_NAME);
            if (dsName != null) {
                datasources.add(dsName);
            }
        }
        return datasources;
    }

    @Override
    public Map<String, List<String>> query(String datasource, String query) throws Exception {
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, lookupDataSource(datasource));
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
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, lookupDataSource(datasource));
        try {
            jdbcConnector.createStatement().execute(command);
        } finally {
            jdbcConnector.close();
        }
    }

    @Override
    public Map<String, List<String>> tables(String datasource) throws Exception {
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, lookupDataSource(datasource));
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
        JdbcConnector jdbcConnector = new JdbcConnector(bundleContext, lookupDataSource(datasource));
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
        } catch (Exception e) {
            LOGGER.error("Can't get information about datasource {}", datasource, e);
            throw e;
        } finally {
            jdbcConnector.close();
        }
    }

    private ServiceReference<?> lookupDataSource(String name) {
        ServiceReference<?>[] references;
        try {
            references = bundleContext.getServiceReferences((String) null,
                    "(&(|(" + Constants.OBJECTCLASS + "=" + DataSource.class.getName() + ")"
                            + "(" + Constants.OBJECTCLASS + "=" + XADataSource.class.getName() + "))"
                            + "(|(osgi.jndi.service.name=" + name + ")(datasource=" + name + ")(name=" + name + ")(service.id=" + name + ")))");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Error finding datasource with name " + name, e);
        }
        if (references == null || references.length == 0) {
            throw new IllegalArgumentException("No JDBC datasource found for " + name);
        }
        if (references.length > 1) {
            Arrays.sort(references);
            if (getRank(references[references.length - 1]) == getRank(references[references.length - 2])) {
                LOGGER.warn("Multiple JDBC datasources found with the same service ranking for " + name);
            }
        }
        return references[references.length - 1];
    }

    private int getRank(ServiceReference<?> reference) {
        Object rankObj = reference.getProperty(Constants.SERVICE_RANKING);
        // If no rank, then spec says it defaults to zero.
        rankObj = (rankObj == null) ? new Integer(0) : rankObj;
        // If rank is not Integer, then spec says it defaults to zero.
        return (rankObj instanceof Integer) ? (Integer) rankObj : 0;
    }

    @Override
    public List<String> factoryNames() throws Exception {
        List<String> factories = new ArrayList<>();

        Collection<ServiceReference<DataSourceFactory>> references = bundleContext.getServiceReferences(DataSourceFactory.class, null);
        if (references == null) {
            return factories;
        }
        for (ServiceReference<DataSourceFactory> reference : references) {
            String driverName = (String)reference.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_NAME);
            if (driverName != null) {
                factories.add(driverName);
            }
        }

        return factories;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
}
