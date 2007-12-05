package org.apache.geronimo.gshell.whisper.transport.vm;

import org.apache.geronimo.gshell.whisper.transport.tcp.TcpTransport;
import org.apache.geronimo.gshell.whisper.transport.tcp.TcpTransportServer;
import org.apache.geronimo.gshell.whisper.transport.base.SpringBaseTransportFactory;
import org.apache.geronimo.gshell.whisper.transport.base.BaseTransport;
import org.apache.geronimo.gshell.whisper.transport.base.BaseTransportServer;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 5, 2007
 * Time: 8:23:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpringVmTransportFactory<T extends VmTransport, S extends VmTransportServer>
    extends SpringBaseTransportFactory {

    public SpringVmTransportFactory() {
        super("vm");
    }

    protected BaseTransport createTransport() {
        return new VmTransport();
    }

    protected BaseTransportServer createTransportServer() {
        return new VmTransportServer();
    }
}
