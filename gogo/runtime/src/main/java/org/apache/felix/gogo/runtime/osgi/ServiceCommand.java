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
package org.apache.felix.gogo.runtime.osgi;

import org.apache.felix.gogo.runtime.shell.CommandShellImpl;
import org.apache.felix.gogo.runtime.shell.Reflective;
import org.osgi.framework.ServiceReference;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;

import java.util.List;

public class ServiceCommand extends Reflective implements Function
{
    ServiceReference ref;
    OSGiShell shell;
    String name;

    public ServiceCommand(OSGiShell shell, ServiceReference ref, String name)
    {
        this.shell = shell;
        this.ref = ref;
        this.name = name;
    }

    public Object execute(CommandSession session, List<Object> arguments) throws Exception
    {
        try
        {
            Object target = shell.bundle.getBundleContext().getService(ref);
            if (target instanceof Function)
            {
                return ((Function) target).execute(session, arguments);
            }
            else
            {
                Object result = method(session, target, name, arguments);
                if (result != CommandShellImpl.NO_SUCH_COMMAND)
                {
                    return result;
                }
                throw new IllegalArgumentException("Service does not implement promised command " + ref + " " + name);
            }
        }
        finally
        {
            shell.bundle.getBundleContext().ungetService(ref);
        }
    }
}
