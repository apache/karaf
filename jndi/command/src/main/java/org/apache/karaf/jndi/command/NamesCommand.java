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

import java.util.Map;

@Command(scope = "jndi", name = "names", description = "List the JNDI names.")
public class NamesCommand extends JndiCommandSupport {

    private static final String NAMES_STRING_FORMAT = "%-20s %-60s";

    @Argument(index = 0, name = "context", description = "The JNDI context to display the names", required = false, multiValued = false)
    String context;

    public Object doExecute() throws Exception {

        System.out.println(String.format(NAMES_STRING_FORMAT, "JNDI Name", "Class Name"));

        Map<String, String> names;
        if (context == null) {
            names = this.getJndiService().names();
        } else {
            names = this.getJndiService().names(context);
        }

        for (String name : names.keySet()) {
            System.out.println(String.format(NAMES_STRING_FORMAT, name, names.get(name)));
        }

        return null;
    }

}
