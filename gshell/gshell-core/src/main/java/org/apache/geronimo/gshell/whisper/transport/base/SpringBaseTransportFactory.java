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
package org.apache.geronimo.gshell.whisper.transport.base;

import java.net.URI;

import org.apache.geronimo.gshell.whisper.transport.base.BaseTransport;
import org.apache.geronimo.gshell.whisper.transport.base.BaseTransportServer;
import org.apache.geronimo.gshell.whisper.transport.Transport;
import org.apache.geronimo.gshell.whisper.transport.TransportServer;
import org.apache.geronimo.gshell.whisper.transport.TransportFactory;
import org.apache.mina.common.IoHandler;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 5, 2007
 * Time: 8:09:12 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SpringBaseTransportFactory<T extends BaseTransport, TC extends Transport.Configuration, S extends BaseTransportServer, SC extends TransportServer.Configuration>
    implements TransportFactory {

    private String scheme;

    protected SpringBaseTransportFactory(String scheme) {
        this.scheme = scheme;
    }

    public String getScheme() {
        return scheme;
    }

    //
    // Transport (Client) Connection
    //

    public T connect(final URI remote, final URI local, final TC config) throws Exception {
        assert remote != null;
        assert config != null;
        // local can be null

        T transport = createTransport();

        transport.setConfiguration(config);

        transport.connect(remote, local);

        return transport;
    }

    public T connect(final URI remote, final URI local, final IoHandler handler) throws Exception {
        assert remote != null;
        assert handler != null;
        // local can be null

        T transport = createTransport();

        transport.getConfiguration().setHandler(handler);

        transport.connect(remote, local);

        return transport;
    }

    //
    // TransportServer Binding
    //

    public S bind(final URI location, final SC config) throws Exception {
        assert location != null;
        assert config != null;

        S server = createTransportServer();

        server.setConfiguration(config);

        server.bind(location);

        return server;
    }

    public S bind(final URI location, final IoHandler handler) throws Exception {
        assert location != null;
        assert handler != null;

        S server = createTransportServer();

        server.getConfiguration().setHandler(handler);

        server.bind(location);

        return server;
    }

    protected abstract T createTransport();

    protected abstract S createTransportServer();

}
