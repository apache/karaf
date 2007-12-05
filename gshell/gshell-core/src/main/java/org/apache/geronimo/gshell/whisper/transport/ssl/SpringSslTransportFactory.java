package org.apache.geronimo.gshell.whisper.transport.ssl;

import org.apache.geronimo.gshell.whisper.transport.tcp.TcpTransport;
import org.apache.geronimo.gshell.whisper.transport.tcp.TcpTransportServer;
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
