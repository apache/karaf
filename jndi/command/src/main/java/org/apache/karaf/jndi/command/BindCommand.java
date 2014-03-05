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
package org.apache.karaf.jndi.command;

import org.apache.karaf.jndi.JndiService;
import org.apache.karaf.jndi.command.completers.ContextsCompleter;
import org.apache.karaf.jndi.command.completers.ServicesIdCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jndi", name = "bind", description = "Bind an OSGi service in the JNDI context")
@Service
public class BindCommand implements Action {

    @Argument(index = 0, name = "service", description = "The ID of the OSGi service to bind", required = true, multiValued = false)
    @Completion(ServicesIdCompleter.class)
    Long serviceId;

    @Argument(index = 1, name = "name", description = "The JNDI name to bind the OSGi service", required = true, multiValued = false)
    @Completion(ContextsCompleter.class)
    String name;

    @Reference
    JndiService jndiService;

    @Override
    public Object execute() throws Exception {
        jndiService.bind(serviceId, name);
        return null;
    }

}
