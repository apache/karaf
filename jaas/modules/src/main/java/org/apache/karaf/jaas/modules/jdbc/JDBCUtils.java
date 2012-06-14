/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.modules.jdbc;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.naming.InitialContext;

public final class JDBCUtils {

    public static final String DATASOURCE = "datasource";
    public static final String JNDI = "jndi:";
    public static final String OSGI = "osgi:";

    private JDBCUtils() {

    }

    /**
     * Looks up a datasource from the url. The datasource can be passed either as jndi name or osgi ldap filter.
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static Object createDatasource(BundleContext bc, String url) throws Exception {
        if (url == null) {
            throw new Exception("Illegal datasource url format. Datasource URL cannot be null.");
        } else if (url.trim().length() == 0) {
            throw new Exception("Illegal datasource url format. Datasource URL cannot be empty.");
        } else if (url.startsWith(JNDI)) {
            String jndiName = url.substring(JNDI.length());
            InitialContext ic = new InitialContext();
            return ic.lookup(jndiName);
        } else if (url.startsWith(OSGI)) {
            String osgiFilter = url.substring(OSGI.length());
            String clazz = null;
            String filter = null;
            String[] tokens = osgiFilter.split("/", 2);
            if (tokens != null) {
                if (tokens.length > 0) {
                    clazz = tokens[0];
                }
                if (tokens.length > 1) {
                    filter = tokens[1];
                }
            }
            ServiceReference[] references = bc.getServiceReferences(clazz, filter);
            if (references != null) {
                ServiceReference ref = references[0];
                Object ds = bc.getService(ref);
                bc.ungetService(ref);
                return ds;
            } else {
                throw new Exception("Unable to find service reference for datasource: " + clazz + "/" + filter);
            }
        } else {
            throw new Exception("Illegal datasource url format");
        }
    }
}
