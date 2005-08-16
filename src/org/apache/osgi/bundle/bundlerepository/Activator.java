/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.osgi.bundle.bundlerepository;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator
{
    private transient BundleContext m_context = null;
    private transient BundleRepositoryImpl m_br = null;

    public void start(BundleContext context)
    {
        m_context = context;

        // Register bundle repository service.
        m_br = new BundleRepositoryImpl(m_context);
        context.registerService(
            org.apache.osgi.service.bundlerepository.BundleRepository.class.getName(),
            m_br, null);

        // We dynamically import the shell service API, so it
        // might not actually be available, so be ready to catch
        // the exception when we try to register the command service.
        try
        {
            // Register "obr" shell command service as a
            // wrapper for the bundle repository service.
            context.registerService(
                org.apache.osgi.service.shell.Command.class.getName(),
                new ObrCommandImpl(m_context, m_br), null);
        }
        catch (Throwable th)
        {
            // Ignore.
        }
    }

    public void stop(BundleContext context)
    {
    }
}