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

package org.apache.servicemix.kernel.gshell.core.remote;

import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.io.IO;
import org.apache.geronimo.gshell.remote.message.OpenShellMessage;
import org.apache.geronimo.gshell.remote.server.RemoteIO;
import org.apache.geronimo.gshell.remote.server.RemoteShellContextHolder;
import org.apache.geronimo.gshell.remote.server.handler.ServerMessageHandlerSupport;
import org.apache.geronimo.gshell.remote.server.handler.ServerSessionContext;
import org.apache.geronimo.gshell.shell.ShellContext;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.spring.BeanContainer;
import org.apache.geronimo.gshell.spring.BeanContainerAware;
import org.apache.geronimo.gshell.spring.BeanContainerImpl;
import org.apache.geronimo.gshell.whisper.transport.Session;

import java.util.UUID;

/**
 * Server handler for {@link OpenShellMessage} messages.
 *
 * @version $Rev: 701226 $ $Date: 2008-10-02 21:51:51 +0200 (Thu, 02 Oct 2008) $
 */
public class OpenShellHandler
    extends ServerMessageHandlerSupport<OpenShellMessage>
    implements BeanContainerAware
{
    private BeanContainer container;

    public OpenShellHandler() {
        super(OpenShellMessage.class);
    }

    public void setBeanContainer(final BeanContainer container) {
        this.container = container;
    }

    public void handle(final Session session, final ServerSessionContext context, final OpenShellMessage message) throws Exception {
        assert session != null;
        assert context != null;
        assert message != null;

        // Create a new container which will be the parent for our remote shells
        context.container = container;

        // Setup the shell context and related components
        context.io = new RemoteIO(session);
        context.variables = new Variables();

        // HACK: Need a shell context, but currently that muck is not exposed, so make a new one
        ShellContext shellContext = new ShellContext() {
            public IO getIo() {
                return context.io;
            }

            public Variables getVariables() {
                return context.variables;
            }
        };

        RemoteShellContextHolder.setContext(shellContext);

        try {
            // Create a new shell instance
            context.shell = context.container.getBean("remoteShell", Shell.class);
        }
        finally {
            RemoteShellContextHolder.clearContext();
        }

        OpenShellMessage.Result reply = new OpenShellMessage.Result();
        reply.setCorrelationId(message.getId());
        session.send(reply);
    }
}
