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
package org.apache.karaf.jaas.jasypt.impl;

import java.util.Hashtable;

import org.apache.karaf.jaas.modules.EncryptionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration<EncryptionService> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("name", "jasypt");
        registration = context.registerService(
                EncryptionService.class,
                new JasyptEncryptionService(),
                props);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        registration.unregister();
    }
}
