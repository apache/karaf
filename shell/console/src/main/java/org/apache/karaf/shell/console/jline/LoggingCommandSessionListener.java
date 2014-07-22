/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.console.jline;

import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.service.command.CommandSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCommandSessionListener implements CommandSessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCommandSessionListener.class);

    public void beforeExecute(CommandSession session, CharSequence command) {
        LOGGER.debug("Executing command: '" + command + "'");
    }

    public void afterExecute(CommandSession session, CharSequence command, Exception exception) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.debug("Command: '" + command + "' failed", exception);
        } else {
            LOGGER.debug("Command: '" + command + "' failed: " + exception);
        }
    }

    public void afterExecute(CommandSession session, CharSequence command, Object result) {
        LOGGER.debug("Command: '" + command + "' returned '" + result + "'");
    }
}
