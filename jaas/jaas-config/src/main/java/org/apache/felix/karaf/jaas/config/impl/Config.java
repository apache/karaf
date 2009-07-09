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
package org.apache.felix.karaf.jaas.config.impl;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.felix.karaf.jaas.boot.ProxyLoginModule;
import org.apache.felix.karaf.jaas.config.JaasRealm;
import org.osgi.framework.BundleContext;

/**
 * An implementation of JaasRealm which is created
 * by the spring namespace handler.
 */
public class Config implements JaasRealm {

    private String name;
    private int rank;
    private Module[] modules;
    private BundleContext bundleContext;
    private transient AppConfigurationEntry[] entries;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Module[] getModules() {
        return modules;
    }

    public void setModules(Module[] modules) {
        this.modules = modules;
        this.entries = null;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public AppConfigurationEntry[] getEntries() {
        if (this.entries == null && this.modules != null) {
            Module[] modules = this.modules;
            AppConfigurationEntry[] entries = new AppConfigurationEntry[modules.length];
            for (int i = 0; i < modules.length; i++) {
                Map<String,Object> options = new HashMap<String,Object>();
                if (modules[i].getOptions() != null) {
                    for (Map.Entry e : modules[i].getOptions().entrySet()) {
                        options.put(e.getKey().toString(), e.getValue());
                    }
                }
                options.put(ProxyLoginModule.PROPERTY_MODULE, modules[i].getClassName());
                options.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
                entries[i] = new AppConfigurationEntry(ProxyLoginModule.class.getName(),
                                                       getControlFlag(modules[i].getFlags()),
                                                       options);
            }
            this.entries = entries;
        }
        return this.entries;
    }

    private AppConfigurationEntry.LoginModuleControlFlag getControlFlag(String flags) {
        if ("required".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
        }
        if ("optional".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
        }
        if ("requisite".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        }
        if ("sufficient".equalsIgnoreCase(flags)) {
            return AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
        }
        return null;
    }
}
