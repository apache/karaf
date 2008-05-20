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
package org.apache.geronimo.gshell.remote.server.handler;

import org.apache.geronimo.gshell.common.Notification;
import org.apache.geronimo.gshell.remote.message.LoginMessage;
import org.apache.geronimo.gshell.whisper.transport.Session;
import org.apache.geronimo.gshell.remote.server.timeout.TimeoutManager;

public class SpringLoginHandler extends LoginHandler {

    private String defaultRealm;

    public SpringLoginHandler(final TimeoutManager timeoutManager, String defaultRealm) {
        super(timeoutManager);
        this.defaultRealm = defaultRealm;
    }

    public void handle(Session session, ServerSessionContext context, LoginMessage message) throws Exception {
        if (message.getRealm() == null) {
            message = new LoginMessage(message.getUsername(), message.getPassword(), defaultRealm);
        }
        super.handle(session, context, message);
    }

}
