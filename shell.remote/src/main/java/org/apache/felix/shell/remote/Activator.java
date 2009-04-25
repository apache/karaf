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
package org.apache.felix.shell.remote;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for the telnet console.
 */
public class Activator implements BundleActivator
{
    private ServiceMediator m_services;
    private Listener m_listener;

    public void start(BundleContext context) throws Exception
    {
        //1. Prepare mediator
        m_services = new ServiceMediator(context);

        //2. Prepare the listener
        m_listener = new Listener(context, m_services);
    }

    public void stop(BundleContext context) throws Exception
    {
        if (m_listener != null)
        {
            m_listener.deactivate();
            m_listener = null;
        }
        if (m_services != null)
        {
            m_services.deactivate();
            m_services = null;
        }
    }
}