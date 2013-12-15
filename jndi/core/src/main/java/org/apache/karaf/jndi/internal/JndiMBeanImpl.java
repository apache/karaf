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
package org.apache.karaf.jndi.internal;

import org.apache.karaf.jndi.JndiService;
import org.apache.karaf.jndi.JndiMBean;

import javax.management.MBeanException;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the JndiMBean
 */
public class JndiMBeanImpl implements JndiMBean {

    private JndiService jndiService;

    @Override
    public Map<String, String> getNames() throws MBeanException {
        try {
            return this.jndiService.names();
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public List<String> getContexts() throws MBeanException {
        try {
            return this.jndiService.contexts();
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public Map<String, String> getNames(String context) throws MBeanException {
        try {
            return this.jndiService.names(context);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public List<String> getContexts(String context) throws MBeanException {
        try {
            return this.jndiService.contexts(context);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void alias(String name, String alias) throws MBeanException {
        try {
            this.jndiService.alias(name, alias);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void bind(Long serviceId, String name) throws MBeanException {
        try {
            this.jndiService.bind(serviceId, name);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void unbind(String name) throws MBeanException {
        try {
            this.jndiService.unbind(name);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void create(String context) throws MBeanException {
        try {
            this.jndiService.create(context);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void delete(String context) throws MBeanException {
        try {
            this.jndiService.delete(context);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    public JndiService getJndiService() {
        return jndiService;
    }

    public void setJndiService(JndiService jndiService) {
        this.jndiService = jndiService;
    }

}
