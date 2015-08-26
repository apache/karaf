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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        MSSQL("wrap:mvn:net.sourceforge.jtds/jtds/", "1.2.4", "datasource-mssql.xml"),
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
            if (version != null) {
                bundleContext.installBundle(this.bundleUrl + version, null).start();
            } else {
                bundleContext.installBundle(this.bundleUrl + this.defaultVersion, null).start();
            }
        }

        public void copyDataSourceFile(File outFile, HashMap<String, String> properties) throws Exception {
            if (!outFile.exists()) {
                InputStream is = JdbcServiceImpl.class.getResourceAsStream(this.templateFile);
                if (is == null) {
                    throw new IllegalArgumentException("Resource " + this.templateFile + " doesn't exist");
                }
                try {
                    // read it line at a time so that we can use the platform line ending when we write it out
                    PrintStream out = new PrintStream(new FileOutputStream(outFile));
                    try {
                        Scanner scanner = new Scanner(is);
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            line = filter(line, properties);
                            out.println(line);
                        }
                    } finally {
                        safeClose(out);
                    }
                } finally {
                    safeClose(is);
                }
            } else {
                throw new IllegalArgumentException("File " + outFile.getPath() + " already exists. Remove it if you wish to recreate it.");
            }
        }

        private void safeClose(InputStream is) throws IOException {
            if (is == null)
                return;
            try {
                is.close();
            } catch (Throwable ignore) {
                // nothing to do
            }
        }

        private void safeClose(OutputStream is) throws IOException {
            if (is == null)
                return;
            try {
                is.close();
            } catch (Throwable ignore) {
                // nothing to do
            }
        }

        private String filter(String line, HashMap<String, String> props) {
            for (Map.Entry<String, String> i : props.entrySet()) {
                int p1 = line.indexOf(i.getKey());
                if (p1 >= 0) {
                    String l1 = line.substring(0, p1);
                    String l2 = line.substring(p1 + i.getKey().length());
                    line = l1 + i.getValue() + l2;
                }
            }
            return line;
        }
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(JdbcServiceImpl.class);

    private BundleContext bundleContext;

    public void create(String name, 
                       String type, 
                       String driverClassName, 
                       String version, 
                       String url, 
                       String user, 
                       String password, 
                       boolean tryToInstallBundles) throws Exception {
        if (tryToInstallBundles) {
            TYPES.valueOf(type.toUpperCase()).installBundle(bundleContext, version);
        }

        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");
        File outFile = new File(deployFolder, "datasource-" + name + ".xml");

        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("${name}", name);
        properties.put("${driver}", driverClassName);
        properties.put("${url}", url);
        properties.put("${user}", user);
        properties.put("${password}", password);
        
        TYPES.valueOf(type.toUpperCase()).copyDataSourceFile(outFile, properties);
    }

    public void create(String name, 
                       String type, 
                       String driverClassName, 
                       String version, 
                       String user, 
                       String password, 
                       String servername, 
                       String databasename, 
                       String portnumber,
                       boolean tryToInstallBundles) throws Exception {
        if (tryToInstallBundles) {
            TYPES.valueOf(type.toUpperCase()).installBundle(bundleContext, version);
        }

        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");
        File outFile = new File(deployFolder, "datasource-" + name + ".xml");

        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("${name}", name);
        properties.put("${driver}", driverClassName);
        properties.put("${user}", user);
        properties.put("${password}", password);
        properties.put("${servername}", servername);
        properties.put("${databasename}", databasename);
        properties.put("${portnumber}", portnumber);
        
        TYPES.valueOf(type.toUpperCase()).copyDataSourceFile(outFile, properties);
    }
    
    public void delete(String name) throws Exception {
        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");
        File datasourceFile = new File(deployFolder, "datasource-" + name + ".xml");
        if (!datasourceFile.exists()) {
            throw new IllegalArgumentException("The JDBC datasource file "+ datasourceFile.getPath() + " doesn't exist");
        }
        datasourceFile.delete();
    }

    public List<String> datasources() throws Exception {
        List<String> datasources = new ArrayList<String>();
        ServiceReference[] references = bundleContext.getServiceReferences((String) null, "(|(" + Constants.OBJECTCLASS + "=" + DataSource.class.getName() + ")("
                + Constants.OBJECTCLASS + "=" + XADataSource.class.getName() + "))");
        if (references != null) {
            for (ServiceReference reference : references) {
                if (reference.getProperty("osgi.jndi.service.name") != null) {
                    datasources.add(reference.getProperty("osgi.jndi.service.name").toString());
                }
                if (reference.getProperty("datasource") != null) {
                    datasources.add(reference.getProperty("datasource").toString());
                }
                if (reference.getProperty("name") != null) {
                    datasources.add(reference.getProperty("name").toString());
                }
                datasources.add(reference.getProperty(Constants.SERVICE_ID).toString());
            }
        }
        return datasources;
    }

    public Map<String, Set<String>> aliases() throws Exception {
        Map<String, Set<String>> aliases = new LinkedHashMap<String, Set<String>>();

        ServiceReference<?>[] references = bundleContext.getServiceReferences((String) null,
                "(|(" + Constants.OBJECTCLASS + "=" + DataSource.class.getName() + ")("
                        + Constants.OBJECTCLASS + "=" + XADataSource.class.getName() + "))");
        if (references != null) {
            List<ServiceReference<?>> refs = Arrays.asList(references);
            Collections.sort(refs);
            Collections.reverse(refs);
            for (ServiceReference<?> reference : refs) {
                Set<String> names = new LinkedHashSet<String>();
                if (reference.getProperty("osgi.jndi.service.name") != null) {
                    names.add(reference.getProperty("osgi.jndi.service.name").toString());
                }
                if (reference.getProperty("datasource") != null) {
                    names.add(reference.getProperty("datasource").toString());
                }
                if (reference.getProperty("name") != null) {
                    names.add(reference.getProperty("name").toString());
                }
                String id = reference.getProperty(Constants.SERVICE_ID).toString();
                names.add(id);
                aliases.put(id, names);
            }
        }
        return aliases;
   }

    public List<String> datasourceFileNames() throws Exception {
        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");

        String[] datasourceFileNames = deployFolder.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("datasource-") && name.endsWith(".xml");
            }
        });

        return Arrays.asList(datasourceFileNames);
    }

    public Map<String, List<String>> query(String datasource, String query) throws Exception {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        ServiceReference reference = this.lookupDataSource(datasource);
        Connection connection = null;
        Statement statement = null;
        try {
            Object ds = bundleContext.getService(reference);
            if (ds instanceof DataSource) {
                connection = ((DataSource) ds).getConnection();
            }
            if (ds instanceof XADataSource) {
                connection = ((XADataSource) ds).getXAConnection().getConnection();
            }
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int c = 1; c <= metaData.getColumnCount(); c++) {
                map.put(metaData.getColumnLabel(c), new ArrayList<String>());
            }
            while (resultSet.next()) {
                for (int c = 1; c <= metaData.getColumnCount(); c++) {
                    map.get(metaData.getColumnLabel(c)).add(resultSet.getString(c));
                }
            }
            resultSet.close();
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return map;
    }

    public void execute(String datasource, String command) throws Exception {
        ServiceReference reference = this.lookupDataSource(datasource);
        Connection connection = null;
        Statement statement = null;
        try {
            Object ds = bundleContext.getService(reference);
            if (ds instanceof DataSource) {
                connection = ((DataSource) ds).getConnection();
            }
            if (ds instanceof XADataSource) {
                connection = ((XADataSource) ds).getXAConnection().getConnection();
            }
            statement = connection.createStatement();
            statement.execute(command);
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
    }

    public Map<String, List<String>> tables(String datasource) throws Exception {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        ServiceReference reference = this.lookupDataSource(datasource);
        Connection connection = null;
        try {
            Object ds = bundleContext.getService(reference);
            if (ds instanceof DataSource) {
                connection = ((DataSource) ds).getConnection();
            }
            if (ds instanceof XADataSource) {
                connection = ((XADataSource) ds).getXAConnection().getConnection();
            }
            DatabaseMetaData dbMetaData = connection.getMetaData();
            ResultSet resultSet = dbMetaData.getTables(null, null, null, null);
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int c = 1; c <= metaData.getColumnCount(); c++) {
                map.put(metaData.getColumnLabel(c), new ArrayList<String>());
            }
            while (resultSet.next()) {
                for (int c = 1; c <= metaData.getColumnCount(); c++) {
                    map.get(metaData.getColumnLabel(c)).add(resultSet.getString(c));
                }
            }
            resultSet.close();
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return map;
    }

    public Map<String, String> info(String datasource) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        ServiceReference reference = this.lookupDataSource(datasource);
        Connection connection = null;
        try {
            Object ds = bundleContext.getService(reference);
            if (ds instanceof DataSource) {
                connection = ((DataSource) ds).getConnection();
            }
            if (ds instanceof XADataSource) {
                connection = ((XADataSource) ds).getXAConnection().getConnection();
            }
            DatabaseMetaData dbMetaData = connection.getMetaData();
            map.put("db.product", dbMetaData.getDatabaseProductName());
            map.put("db.version", dbMetaData.getDatabaseProductVersion());
            map.put("url", dbMetaData.getURL());
            map.put("username", dbMetaData.getUserName());
            map.put("driver.name", dbMetaData.getDriverName());
            map.put("driver.version", dbMetaData.getDriverVersion());
        } catch (Exception e) {
            LOGGER.error("Can't get information about datasource {}", datasource, e);
            throw e;
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return map;
    }

    private ServiceReference lookupDataSource(String name) throws Exception {
        ServiceReference[] references = bundleContext.getServiceReferences((String) null,
                "(&(|(" + Constants.OBJECTCLASS + "=" + DataSource.class.getName() + ")"
                        + "(" + Constants.OBJECTCLASS + "=" + XADataSource.class.getName() + "))"
                        + "(|(osgi.jndi.service.name=" + name + ")(datasource=" + name + ")(name=" + name + ")(service.id=" + name + ")))");
        if (references == null || references.length == 0) {
            throw new IllegalArgumentException("No JDBC datasource found for " + name);
        }
        if (references.length > 1) {
            throw new IllegalArgumentException("Multiple JDBC datasource found for " + name);
        }
        return references[0];
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
