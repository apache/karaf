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
package org.apache.karaf.management.mbeans.system.internal;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.management.mbeans.system.SystemMBean;
import org.osgi.framework.BundleContext;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * System MBean implementation.
 */
public class SystemMBeanImpl extends StandardMBean implements SystemMBean {

    private BundleContext bundleContext;

    public SystemMBeanImpl() throws NotCompliantMBeanException {
        super(SystemMBean.class);
    }

    public String getName() {
        return bundleContext.getProperty("karaf.name");
    }

    public void setName(String name) {
        try {
            String karafBase = bundleContext.getProperty("karaf.base");
            File etcDir = new File(karafBase, "etc");
            File syspropsFile = new File(etcDir, "system.properties");
            FileInputStream fis = new FileInputStream(syspropsFile);
            Properties props = new Properties();
            props.load(fis);
            fis.close();
            props.setProperty("karaf.name", name);
            FileOutputStream fos = new FileOutputStream(syspropsFile);
            props.store(fos, "");
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void shutdown() throws Exception {
        bundleContext.getBundle(0).stop();
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
