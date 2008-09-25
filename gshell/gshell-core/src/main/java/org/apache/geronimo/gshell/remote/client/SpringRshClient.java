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
package org.apache.geronimo.gshell.remote.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

import org.apache.geronimo.gshell.remote.client.auth.RemoteLoginModule;
import org.apache.geronimo.gshell.remote.client.handler.ClientMessageHandler;
import org.apache.geronimo.gshell.remote.client.handler.ClientSessionContext;
import org.apache.geronimo.gshell.remote.client.RemoteExecuteException;
import org.apache.geronimo.gshell.remote.client.RshClient;
import org.apache.geronimo.gshell.remote.crypto.CryptoContext;
import org.apache.geronimo.gshell.remote.jaas.JaasConfigurationHelper;
import org.apache.geronimo.gshell.remote.jaas.UsernamePasswordCallbackHandler;
import org.apache.geronimo.gshell.remote.message.CloseShellMessage;
import org.apache.geronimo.gshell.remote.message.ConnectMessage;
import org.apache.geronimo.gshell.remote.message.EchoMessage;
import org.apache.geronimo.gshell.remote.message.ExecuteMessage;
import org.apache.geronimo.gshell.remote.message.OpenShellMessage;
import org.apache.geronimo.gshell.whisper.message.Message;
import org.apache.geronimo.gshell.whisper.message.MessageHandler;
import org.apache.geronimo.gshell.whisper.transport.Session;
import org.apache.geronimo.gshell.whisper.transport.Transport;
import org.apache.geronimo.gshell.whisper.transport.TransportFactory;
import org.apache.geronimo.gshell.whisper.transport.TransportFactoryLocator;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.DemuxingIoHandler;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides support for the client-side of the remote shell protocol.
 *
 * @version $Rev: 638824 $ $Date: 2008-03-19 14:30:40 +0100 (Wed, 19 Mar 2008) $
 */
@Component(role=SpringRshClient.class, instantiationStrategy="per-lookup")
public class SpringRshClient extends RshClient
    implements Initializable
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Requirement
    private CryptoContext crypto;

    @Requirement
    private TransportFactoryLocator locator;

    private Transport transport;

    private Session session;

    @Requirement(role=ClientMessageHandler.class)
    private List<ClientMessageHandler> handlers;

    public SpringRshClient() {
    }

    public SpringRshClient(final CryptoContext crypto, final TransportFactoryLocator locator, final List<ClientMessageHandler> handlers) {
        this.crypto = crypto;
        this.locator = locator;
        this.handlers = handlers;
    }

    public void initialize() throws InitializationException {
        new JaasConfigurationHelper("client.login.conf").initialize();
    }

    public void connect(final URI remote, final URI local) throws Exception {
        TransportFactory factory = locator.locate(remote);

        transport = factory.connect(remote, local, new Handler());
        session = transport.getSession();

        log.debug("Connected to: {}", remote);
    }

    public InputStream getInputStream() {
        return session.getInputStream();
    }

    public OutputStream getOutputStream() {
        return session.getOutputStream();
    }

    public Transport getTransport() {
        return transport;
    }

    public void close() {
        transport.close();
    }

    public void login(final String username, final String password) throws Exception {
        doHandshake();
        doLogin(username, password);
    }

    private void doHandshake() throws Exception {
        log.debug("Handshaking");

        ClientSessionContext context = ClientSessionContext.BINDER.lookup(session.getSession());

        Message response = session.request(new ConnectMessage(crypto.getPublicKey()));

        if (response instanceof ConnectMessage.Result) {
            ConnectMessage.Result result = (ConnectMessage.Result)response;
            context.pk = result.getPublicKey();
        }
        else {
            throw new InternalError("Unexpected handshake response: " + response);
        }
    }

    private void doLogin(final String username, final String password) throws Exception {
        log.debug("Logging in: {}", username);

        ClientSessionContext context = ClientSessionContext.BINDER.lookup(session.getSession());

        CallbackHandler callbackHandler = new UsernamePasswordCallbackHandler(username, password);
        LoginContext loginContext = new LoginContext("RshClient", callbackHandler);

        RemoteLoginModule.setTransport(transport);
        try {
            loginContext.login();
        }
        finally {
            RemoteLoginModule.unsetTransport();
        }

        context.subject = loginContext.getSubject();
        log.debug("Subject: {}", context.subject);
    }

    public void echo(final String text) throws Exception {
        assert text != null;

        log.debug("Echoing: {}", text);

        session.send(new EchoMessage(text)).join();
    }

    public void openShell() throws Exception {
        log.debug("Opening remote shell");

        Message resp = session.request(new OpenShellMessage());

        //
        // TODO: Need some context from the response
        //

        // log.debug("Response: {}", resp);
    }

    public void closeShell() throws Exception {
        log.debug("Closing remote shell");

        Message resp = session.request(new CloseShellMessage());

        //
        // TODO: Need some context from the response
        //

        // log.debug("Response: {}", resp);
    }

    private Object doExecute(final ExecuteMessage msg) throws Exception {
        assert msg != null;

        ExecuteMessage.Result result = (ExecuteMessage.Result) session.request(msg);

        // Handle result notifications
        if (result instanceof ExecuteMessage.Notification) {
            ExecuteMessage.Notification n = (ExecuteMessage.Notification)result;

            throw n.getNotification();
        }

        // Handle result faults
        if (result instanceof ExecuteMessage.Fault) {
            ExecuteMessage.Fault fault = (ExecuteMessage.Fault)result;

            throw new RemoteExecuteException(fault.getCause());
        }

        Object rv = result.getResult();

        log.debug("Command result: {}", rv);

        return rv;
    }

    public Object execute(final String line) throws Exception {
        assert line != null;

        return doExecute(new ExecuteMessage(line));
    }

    public Object execute(final Object... args) throws Exception {
        assert args != null;

        return doExecute(new ExecuteMessage(args));
    }

    public Object execute(final String path, final Object[] args) throws Exception {
        assert path != null;
        assert args != null;

        return doExecute(new ExecuteMessage(path, args));
    }

    public Object execute(final Object[][] cmds) throws Exception {
        assert cmds != null;

        return doExecute(new ExecuteMessage(cmds));
    }

    protected void onSessionClosed() {
        // nothing
    }

    //
    // IO Handler
    //

    private class Handler
        extends DemuxingIoHandler
    {
        public Handler() throws Exception {
            // Complain if we don't have any handlers
            if (handlers.isEmpty()) {
                throw new Error("No message handlers were discovered");
            }

            for (ClientMessageHandler handler : handlers) {
                register(handler);
            }
        }

        public void register(final MessageHandler handler) {
            assert handler != null;

            Class<?> type = handler.getType();

            log.debug("Registering handler: {} -> {}", type.getSimpleName(), handler);

            // noinspection unchecked
            addMessageHandler(type, handler);
        }

        @Override
        public void sessionOpened(final IoSession session) throws Exception {
            assert session != null;

            // Install the session context
            ClientSessionContext context = ClientSessionContext.BINDER.bind(session, new ClientSessionContext());
            log.debug("Created session context: {}", context);
        }

        @Override
        public void sessionClosed(final IoSession session) throws Exception {
            assert session != null;

            ClientSessionContext context = ClientSessionContext.BINDER.unbind(session);
            log.debug("Removed session context: {}", context);
            onSessionClosed();
        }
    }
}