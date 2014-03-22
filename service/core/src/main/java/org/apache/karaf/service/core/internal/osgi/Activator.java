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
package org.apache.karaf.service.core.internal.osgi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.karaf.service.core.internal.ServicesMBeanImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration mbeanRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        ServicesMBeanImpl mbean = new ServicesMBeanImpl();
        mbean.setBundleContext(context);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("jmx.objectname", "org.apache.karaf:type=service,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = context.registerService(
                getInterfaceNames(mbean),
                mbean,
                props
        );
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        mbeanRegistration.unregister();
    }

    private String[] getInterfaceNames(Object object) {
        List<String> names = new ArrayList<String>();
        for (Class cl = object.getClass(); cl != Object.class; cl = cl.getSuperclass()) {
            addSuperInterfaces(names, cl);
        }
        return names.toArray(new String[names.size()]);
    }

    private void addSuperInterfaces(List<String> names, Class clazz) {
        for (Class cl : clazz.getInterfaces()) {
            names.add(cl.getName());
            addSuperInterfaces(names, cl);
        }
    }

}
