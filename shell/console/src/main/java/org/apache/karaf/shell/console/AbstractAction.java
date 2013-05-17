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
package org.apache.karaf.shell.console;

import org.apache.karaf.shell.commands.Action;
import org.apache.felix.service.command.CommandSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAction implements Action {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected CommandSession session;

    public Object execute(CommandSession session) throws Exception {
        this.session = session;
        return doExecute();
    }

    protected abstract Object doExecute() throws Exception;
    
    /**
     * This is for long running commands to be interrupted by ctrl-c
     * 
     * @throws InterruptedException
     */
    public static void checkInterrupted() throws InterruptedException {
        Thread.yield(); 
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

}
