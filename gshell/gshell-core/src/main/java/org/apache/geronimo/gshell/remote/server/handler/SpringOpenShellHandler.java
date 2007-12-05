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

import org.apache.geronimo.gshell.DefaultEnvironment;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.shell.ShellInfo;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.remote.message.EchoMessage;
import org.apache.geronimo.gshell.remote.message.OpenShellMessage;
import org.apache.geronimo.gshell.remote.server.DefaultRemoteShell;
import org.apache.geronimo.gshell.remote.server.RemoteIO;
import org.apache.geronimo.gshell.whisper.transport.Session;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 5, 2007
 * Time: 4:36:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpringOpenShellHandler extends ServerMessageHandlerSupport<OpenShellMessage> {

    private ShellInfo shellInfo;
    private CommandExecutor executor;

    public SpringOpenShellHandler() {
        super(OpenShellMessage.class);
    }

    public SpringOpenShellHandler(ShellInfo shellInfo, CommandExecutor executor) {
        this();
        this.shellInfo = shellInfo;
        this.executor = executor;
    }

    public void handle(Session session, ServerSessionContext context, OpenShellMessage message) throws Exception {
        // Setup the I/O context (w/o auto-flushing)
        context.io = new RemoteIO(session);
        // Setup shell environemnt
        context.env = new DefaultEnvironment(context.io);

        // Create a new shell instance
        context.shell = new DefaultRemoteShell(shellInfo, executor, context.env);

        //
        // TODO: Send a meaningful response
        //

        EchoMessage reply = new EchoMessage("OPEN SHELL SUCCESS");
        reply.setCorrelationId(message.getId());
        session.send(reply);
    }
}
