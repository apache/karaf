/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
// DWB3: dynamically load optional framework components to reduce dependencies
// DWB4: get() with trailing colon causes org.osgi.framework.InvalidSyntaxException
package org.apache.felix.gogo.runtime.osgi;

import org.apache.felix.gogo.runtime.shell.CommandShellImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.command.Converter;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.threadio.ThreadIO;

public class OSGiShell extends CommandShellImpl
{
    Bundle bundle;
    OSGiCommands commands;

    public void start() throws Exception
    {
        commands = new OSGiCommands(bundle);
        addCommand("osgi", this.bundle);
        addCommand("osgi", commands);
        setConverter(commands);
        if (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING)
        {
            addCommand("osgi", commands.service(PackageAdmin.class.getName(), null),
                PackageAdmin.class);
            addCommand("osgi", commands.getContext(), BundleContext.class);

            try
            {
                // derek - dynamically load StartLevel to avoid import dependency
                String sl = "org.osgi.service.startlevel.StartLevel";
                Class<?> slClass = bundle.loadClass(sl);
                addCommand("osgi", commands.service(sl, null), slClass);
            }
            catch (ClassNotFoundException e)
            {
            }

            try
            {
                // derek - dynamically load PermissionAdmin to avoid import dependency
                String pa = "org.osgi.service.permissionadmin.PermissionAdmin";
                Class<?> paClass = bundle.loadClass(pa);
                addCommand("osgi", commands.service(pa, null), paClass);
            }
            catch (ClassNotFoundException e)
            {
            }
        }
        else
        {
            System.err.println("eek! bundle not active: " + bundle);
        }
    }

    public Object get(String name)
    {
        if (bundle.getBundleContext() != null)
        {
            BundleContext context = bundle.getBundleContext();
            try
            {
                Object cmd = super.get(name);
                if (cmd != null)
                {
                    return cmd;
                }

                int n = name.indexOf(':');
                if (n < 0)
                {
                    return null;
                }

                String service = name.substring(0, n);
                String function = name.substring(n + 1);

                // derek - fix org.osgi.framework.InvalidSyntaxException
                if (service.length() == 0 || function.length() == 0)
                {
                    return null;
                }

                String filter = String.format(
                    "(&(osgi.command.scope=%s)(osgi.command.function=%s))", service,
                    function);
                ServiceReference refs[] = context.getServiceReferences(null, filter);
                if (refs == null || refs.length == 0)
                {
                    return null;
                }

                if (refs.length > 1)
                {
                    throw new IllegalArgumentException(
                        "Command name is not unambiguous: " + name
                            + ", found multiple impls");
                }

                return new ServiceCommand(this, refs[0], function);
            }
            catch (InvalidSyntaxException ise)
            {
                ise.printStackTrace();
            }
        }
        return super.get(name);
    }

    public void setThreadio(Object t)
    {
        super.setThreadio((ThreadIO) t);
    }

    public void setBundle(Bundle bundle)
    {
        this.bundle = bundle;
    }

    public void setConverter(Converter c)
    {
        super.setConverter(c);
    }

}
