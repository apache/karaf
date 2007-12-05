package org.apache.geronimo.gshell.whisper.transport.tcp;

import org.apache.geronimo.gshell.whisper.transport.base.SpringBaseTransportFactory;
import org.apache.geronimo.gshell.whisper.transport.base.BaseTransport;
import org.apache.geronimo.gshell.whisper.transport.base.BaseTransportServer;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 5, 2007
 * Time: 8:19:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpringTcpTransportFactory<T extends TcpTransport, S extends TcpTransportServer>
    extends SpringBaseTransportFactory {

    public SpringTcpTransportFactory() {
        super("tcp");
    }

    protected BaseTransport createTransport() {
        return new TcpTransport();
    }

    protected BaseTransportServer createTransportServer() {
        return new TcpTransportServer();
    }
}
