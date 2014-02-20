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
package org.apache.karaf.shell.commands.basic;

import java.util.Hashtable;

import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleContext;

/**
 * A very simple {@link Function} which creates {@link Action} based on a class name.
 */
public class SimpleCommand extends AbstractCommand {

    private Class<? extends Action> actionClass;

    public SimpleCommand()
    {
    }

    public SimpleCommand(Class<? extends Action> actionClass)
    {
        this.actionClass = actionClass;
    }

    public Class<? extends Action> getActionClass()
    {
        return actionClass;
    }

    public void setActionClass(Class<? extends Action> actionClass)
    {
        this.actionClass = actionClass;
    }

    public Action createNewAction() {
        try {
            return actionClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ServiceRegistration export(BundleContext context, Class<? extends Action> actionClass)
    {
        Command cmd = actionClass.getAnnotation(Command.class);
        if (cmd == null)
        {
            throw new IllegalArgumentException("Action class is not annotated with @Command");
        }
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("osgi.command.scope", cmd.scope());
        props.put("osgi.command.function", cmd.name());
        SimpleCommand command = new SimpleCommand(actionClass);
        return context.registerService(
                new String[] { Function.class.getName(), CommandWithAction.class.getName() },
                command, props);
    }

}
