package org.apache.servicemix.kernel.gshell.core.remote;

import java.net.URI;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.geronimo.gshell.remote.server.RshServer;

public class RshServerFactory {

    private RshServer server;

    private String location;

    private boolean start;

    public RshServerFactory(RshServer server) {
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

    @PostConstruct
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

    @PreDestroy
    public void stop() throws Exception {
        if (start) {
            server.close();
        }
    }

}
