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
package org.apache.karaf.jndi.command.completers;

import org.apache.karaf.jndi.JndiService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;

import java.util.List;

/**
 * Completers on the JNDI contexts.
 */
@Service
public class ContextsCompleter implements Completer {

    @Reference
    private JndiService jndiService;

    public int complete(String buffer, int cursor, List candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            List<String> contexts = jndiService.contexts();
            for (String context : contexts) {
                delegate.getStrings().add(context);
            }
        } catch (Exception e) {
            // nothing to do
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public JndiService getJndiService() {
        return jndiService;
    }

    public void setJndiService(JndiService jndiService) {
        this.jndiService = jndiService;
    }

}
