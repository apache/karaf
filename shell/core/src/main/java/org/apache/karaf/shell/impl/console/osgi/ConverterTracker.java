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
package org.apache.karaf.shell.impl.console.osgi;

import org.apache.felix.service.command.Converter;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracker for Converter.
 */
public class ConverterTracker extends ServiceTracker<Converter, Converter> {

    private SessionFactoryImpl sessionFactory;

    public ConverterTracker(SessionFactoryImpl sessionFactory, BundleContext context) {
        super(context, Converter.class, null);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Converter addingService(ServiceReference<Converter> reference) {
        Converter service = super.addingService(reference);
        sessionFactory.getCommandProcessor().addConverter(service);
        return service;
    }

    @Override
    public void removedService(ServiceReference<Converter> reference, Converter service) {
        sessionFactory.getCommandProcessor().removeConverter(service);
        super.removedService(reference, service);
    }
}
