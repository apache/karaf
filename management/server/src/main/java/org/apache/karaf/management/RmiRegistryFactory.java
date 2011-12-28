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
package org.apache.karaf.management;

import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

public class RmiRegistryFactory {

    private int port = Registry.REGISTRY_PORT;
    private String host;
    private Registry registry;
    private boolean locate;
    private boolean create = true;
    private boolean locallyCreated;

    private BundleContext bundleContext;
    
    /**
     * @return the create
     */
    public boolean isCreate() {
        return create;
    }

    /**
     * @param create the create to set
     */
    public void setCreate(boolean create) {
        this.create = create;
    }

    /**
     * @return the locate
     */
    public boolean isLocate() {
        return locate;
    }

    /**
     * @param locate the locate to set
     */
    public void setLocate(boolean locate) {
        this.locate = locate;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Object getObject() throws Exception {
        return registry;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() throws RemoteException, UnknownHostException {
        if (registry == null && locate) {
            try {
                Registry reg = LocateRegistry.getRegistry(host, getPort());
                reg.list();
                registry = reg;
            } catch (RemoteException e) {
                // ignore
            }
        }
        if (registry == null && create) {
            if (host != null && !host.isEmpty()) {
                RMIClientSocketFactory socketFactory = RMISocketFactory.getDefaultSocketFactory();
                InetAddress addr = InetAddress.getByName(host);
                RMIServerSocketFactory serverSocketFactory = new KarafServerSocketFactory(addr, port);

                registry = LocateRegistry.createRegistry(getPort(), socketFactory, serverSocketFactory);
            } else {
                registry = LocateRegistry.createRegistry(getPort());
            }
            locallyCreated = true;
        }
        if (registry != null) {
            // register the registry as an OSGi service
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("port", getPort());
            props.put("host", getHost());
            bundleContext.registerService(Registry.class, registry, props);
        }
    }

    public void destroy() throws RemoteException {
        if (registry != null && locallyCreated) {
            Registry reg = registry;
            registry = null;
            UnicastRemoteObject.unexportObject(reg, true);
        }
    }

    private static class KarafServerSocketFactory implements RMIServerSocketFactory {
        private final int port;
        private final InetAddress addr;

        private KarafServerSocketFactory(InetAddress addr, int port) {
            this.addr = addr;
            this.port = port;
        }

        @Override
        public ServerSocket createServerSocket(int i) throws IOException {
            return new ServerSocket(port, 0, addr);
        }
    }

}
