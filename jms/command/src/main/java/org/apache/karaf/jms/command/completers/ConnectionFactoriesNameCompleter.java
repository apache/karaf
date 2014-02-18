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
package org.apache.karaf.jms.command.completers;

import org.apache.karaf.jms.JmsService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;

import java.util.List;

/**
 * Completer on the JMS connection factories name.
 */
@Service
public class ConnectionFactoriesNameCompleter implements Completer {

    @Reference
    private JmsService jmsService;

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (String connectionFactory : jmsService.connectionFactories()) {
                delegate.getStrings().add(connectionFactory);
            }
        } catch (Exception e) {
            // nothing to do
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public JmsService getJmsService() {
        return jmsService;
    }

    public void setJmsService(JmsService jmsService) {
        this.jmsService = jmsService;
    }

}
