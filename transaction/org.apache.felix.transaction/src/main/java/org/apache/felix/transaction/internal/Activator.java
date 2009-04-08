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
package org.apache.felix.transaction.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Activator implements BundleActivator, ManagedServiceFactory {

    private static final Logger log = LoggerFactory.getLogger("org.apache.felix.transaction");

    private BundleContext bundleContext;
    private Map managers = new HashMap<String, TransactionManagerService>();

    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, getName());
        bundleContext.registerService(ManagedServiceFactory.class.getName(), this, props);

        Hashtable ht = new Hashtable();
        updated("initial", ht);
    }

    private void set(Hashtable ht, String key) {
        String o = bundleContext.getProperty(key);
        if (o == null) {
            o = System.getenv(key.toUpperCase().replaceAll(".", "_"));
            if (o == null) {
                return;
            }
        }
        ht.put(key, o);
    }

    public void stop(BundleContext context) throws Exception {
        for (Iterator w = managers.values().iterator(); w.hasNext();) {
            try {
                TransactionManagerService mgr = (TransactionManagerService) w.next();
                w.remove();
                mgr.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public String getName() {
        return "org.apache.felix.transaction";
    }

    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        deleted(pid);
        TransactionManagerService mgr = new TransactionManagerService(pid, properties, bundleContext);
        managers.put(pid, mgr);
        try {
            mgr.start();
        } catch (Exception e) {
            log.error("Error starting transaction manager", e);
        }
    }

    public void deleted(String pid) {
        TransactionManagerService mgr = (TransactionManagerService) managers.remove(pid);
        if (mgr != null) {
            try {
                mgr.close();
            } catch (Exception e) {
                log.error("Error stopping transaction manager", e);
            }
        }
    }

}