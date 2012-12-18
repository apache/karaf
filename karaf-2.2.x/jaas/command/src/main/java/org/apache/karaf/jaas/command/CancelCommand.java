/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.command;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.modules.BackingEngine;

import java.util.LinkedList;

@Command(scope = "jaas", name = "cancel", description = "Cancel the modification of a JAAS realm")
public class CancelCommand extends JaasCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        // cleanup the session
        session.put(JAAS_REALM, null);
        session.put(JAAS_ENTRY, null);
        session.put(JAAS_CMDS, new LinkedList<JaasCommandSupport>());
        return null;
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }
}
