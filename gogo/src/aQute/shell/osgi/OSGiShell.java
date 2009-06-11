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
package aQute.shell.osgi;

import org.osgi.framework.*;
import org.osgi.service.command.*;
import org.osgi.service.component.*;
import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.*;
import org.osgi.service.startlevel.*;
import org.osgi.service.threadio.*;

import aQute.shell.runtime.*;

public class OSGiShell extends CommandShellImpl {
    Bundle       bundle;
    OSGiCommands commands;

    protected void activate(ComponentContext context) throws Exception {
        this.bundle = context.getBundleContext().getBundle();
        if (threadIO == null)
            threadIO = (ThreadIO) context.locateService("x");
        start();
    }

    public void start() throws Exception {
        commands = new OSGiCommands(bundle);
        addCommand("osgi", this.bundle);
        addCommand("osgi", commands);
        setConverter(commands);
        if (bundle.getState() == Bundle.ACTIVE) {
            addCommand("osgi", commands.service(StartLevel.class.getName(),
                    null), StartLevel.class);
            addCommand("osgi", commands.service(PackageAdmin.class.getName(),
                    null), PackageAdmin.class);
            addCommand("osgi", commands.service(
                    PermissionAdmin.class.getName(), null),
                    PermissionAdmin.class);
            addCommand("osgi", commands.getContext(), BundleContext.class);
        }
    }

    protected void deactivate(ComponentContext context) {
        System.out.println("Deactivating");
    }

    public Object get(String name) {
        if (bundle.getBundleContext() != null) {
            BundleContext context = bundle.getBundleContext();
            try {
                Object cmd = super.get(name);
                if (cmd != null)
                    return cmd;

                int n = name.indexOf(':');
                if (n < 0)
                    return null;

                String service = name.substring(0, n);
                String function = name.substring(n + 1);

                String filter = String.format(
                        "(&(osgi.command.scope=%s)(osgi.command.function=%s))",
                        service, function);
                ServiceReference refs[] = context.getServiceReferences(null,
                        filter);
                if (refs == null || refs.length == 0)
                    return null;

                if (refs.length > 1)
                    throw new IllegalArgumentException(
                            "Command name is not unambiguous: " + name
                                    + ", found multiple impls");

                return new ServiceCommand(this, refs[0], function);
            } catch (InvalidSyntaxException ise) {
                ise.printStackTrace();
            }
        }
        return super.get(name);
    }

    public void setThreadio(Object t) {
        super.setThreadio((ThreadIO) t);
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setConverter(Converter c) {
        super.setConverter(c);
    }

}
