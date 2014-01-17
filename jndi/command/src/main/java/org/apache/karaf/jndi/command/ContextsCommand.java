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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import java.util.List;

@Command(scope = "jndi", name = "contexts", description = "List the JNDI sub-contexts.")
public class ContextsCommand extends JndiCommandSupport {

    @Argument(index = 0, name = "context", description = "The base JNDI context", required = false, multiValued = false)
    String context;

    public Object doExecute() throws Exception {

        List<String> contexts;
        if (context == null) {
            contexts = this.getJndiService().contexts();
        } else {
            contexts = this.getJndiService().contexts(context);
        }

        for (String c : contexts) {
            System.out.println(c);
        }

        return null;
    }

}
