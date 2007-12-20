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
package org.apache.geronimo.gshell.whisper.transport.ssl;

import org.apache.geronimo.gshell.whisper.transport.base.SpringBaseTransportFactory;
import org.apache.geronimo.gshell.whisper.transport.base.BaseTransport;
import org.apache.geronimo.gshell.whisper.transport.base.BaseTransportServer;
import org.apache.geronimo.gshell.whisper.ssl.SSLContextFactory;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 5, 2007
 * Time: 8:21:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpringSslTransportFactory<T extends SslTransport, S extends SslTransportServer>
    extends SpringBaseTransportFactory {

    private SSLContextFactory contextFactory;

    public SpringSslTransportFactory() {
        super("ssl");
    }

    public SSLContextFactory getContextFactory() {
        return contextFactory;
    }

    public void setContextFactory(SSLContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    protected BaseTransport createTransport() {
        return new SslTransport(contextFactory);
    }

    protected BaseTransportServer createTransportServer() {
        return new SslTransportServer(contextFactory);
    }
}
