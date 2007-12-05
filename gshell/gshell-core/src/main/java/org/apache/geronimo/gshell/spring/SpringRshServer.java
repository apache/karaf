package org.apache.geronimo.gshell.spring;

import java.net.URI;

import org.apache.geronimo.gshell.remote.server.RshServer;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 5, 2007
 * Time: 8:34:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpringRshServer {

    private RshServer server;
    private String location;
    private boolean start;

    public RshServer getServer() {
        return server;
    }

    public void setServer(RshServer server) {
        this.server = server;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public void start() throws Exception {
        if (start) {
            try {
                server.bind(URI.create(location));
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void stop() throws Exception {
        if (start) {
            server.close();
        }
    }
}
