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
package org.apache.karaf.shell.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.Action;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public abstract class OsgiCommandSupport extends AbstractAction implements Action, BundleContextAware {

    protected BundleContext bundleContext;
    protected List<ServiceReference<?>> usedReferences;

    @Override
    public Object execute(CommandSession session) throws Exception {
        try {
            return super.execute(session);
        } finally {
            ungetServices();
        }
    }

    public BundleContext getBundleContext() {
        Bundle framework = bundleContext.getBundle(0);
        return framework == null? bundleContext: framework.getBundleContext();
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected <T> List<T> getAllServices(Class<T> clazz) {
        try {
            return getAllServices(clazz, null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected <T> List<T> getAllServices(Class<T> clazz, String filter) throws InvalidSyntaxException {
        Collection<ServiceReference<T>> references = getBundleContext().getServiceReferences(clazz, filter);
        List<T> services = new ArrayList<T>();
        if (references != null) {
            for (ServiceReference<T> ref : references) {
                T t = getService(clazz, ref);
                services.add(t);
            }
        }
        return services;
    }

    protected <T> T getService(Class<T> clazz) {
        ServiceReference<T> sr = getBundleContext().getServiceReference(clazz);
        if (sr != null) {
            return getService(clazz, sr);
        } else {
            return null;
        }
    }

    protected <T> T getService(Class<T> clazz, ServiceReference<T> reference) {
        T t = getBundleContext().getService(reference);
        if (t != null) {
            if (usedReferences == null) {
                usedReferences = new ArrayList<ServiceReference<?>>();
            }
            usedReferences.add(reference);
        }
        return t;
    }

    protected void ungetServices() {
        if (usedReferences != null) {
            for (ServiceReference<?> ref : usedReferences) {
                getBundleContext().ungetService(ref);
            }
        }
    }

}
