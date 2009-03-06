/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.servicemix.kernel.gshell.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gshell.command.CommandAction;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.io.IO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;

public abstract class OsgiCommandSupport implements CommandAction, BundleContextAware {

    protected Log log = LogFactory.getLog(getClass());
    protected BundleContext bundleContext;
    protected CommandContext commandContext;
    protected IO io;
    protected Variables variables;
    protected List<ServiceReference> usedReferences;
    
    public Object execute(CommandContext commandContext) throws Exception {
        this.commandContext = commandContext;
        this.io = commandContext.getIo();
        this.variables = commandContext.getVariables();
        try {
            return doExecute();
        } finally {
            ungetServices();
        }
    }

    protected abstract Object doExecute() throws Exception;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected <T> List<T> getAllServices(Class<T> clazz, String filter) throws Exception {
        ServiceReference[] references = getBundleContext().getAllServiceReferences(clazz.getName(), filter);
        if (references == null) {
            return null;
        }
        List<T> services = new ArrayList<T>();
        for (ServiceReference ref : references) {
            T t = getService(clazz, ref);
            services.add(t);
        }
        return services;
    }

    protected <T> T getService(Class<T> clazz, ServiceReference reference) {
        T t = (T) getBundleContext().getService(reference);
        if (t != null) {
            if (usedReferences == null) {
                usedReferences = new ArrayList<ServiceReference>();
            }
            usedReferences.add(reference);
        }
        return t;
    }

    protected void ungetServices() {
        if (usedReferences != null) {
            for (ServiceReference ref : usedReferences) {
                getBundleContext().ungetService(ref);
            }
        }
    }

}
