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

package org.apache.karaf.tooling.exam.container.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.ops4j.net.FreePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graceful RMI registry creation/reuse. Tries to reuse an existing one but is fine with creating one on another port.
 */
public class RMIRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(RMIRegistry.class);

    @SuppressWarnings("unused")
    private final Integer m_defaultPort;

    private static final int UNSELECTED = -1;

    final private String m_host;
    private Integer m_port = UNSELECTED;
    private Integer m_altMin;
    private Integer m_altTo;
    private static final int TREASURE = 30;

    public RMIRegistry(Integer defaultPort, Integer alternativeRangeFrom, Integer alternativeRangeTo)

    {
        try {
            m_host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Cannot select localhost. That usually not a good sign for networking..");
        }
        m_defaultPort = defaultPort;
        m_altMin = alternativeRangeFrom;
        m_altTo = alternativeRangeTo;
    }

    /**
     * This will make sure a registry exists and is valid m_port. If its not available or does not work for some reason,
     * it will select another port. This should really not happen usually. But it can.
     * 
     * @return this for fluent API. Or IllegalStateException if a port has not been detected successfully.
     */
    public synchronized RMIRegistry selectGracefully()
    {
        // if( ( m_port = select( m_defaultPort ) ) == UNSELECTED ) {
        int alternativePort = new FreePort(m_altMin, m_altTo).getPort();
        if ((m_port = select(alternativePort)) == UNSELECTED) {
            throw new IllegalStateException("No port found for RMI at all. Even though " + alternativePort
                    + " should have worked. Thats.. not. good. at. all.");
        }
        printTakenStatus();
        // }

        return this;
    }

    private void printTakenStatus()
    {

        int in_use = m_port - m_altMin + 1; // the one we just took
        int max = m_altTo - m_altMin;
        String info =
            "Currently " + in_use + " out of " + max + " ports are in use. Port range is from " + m_altMin + " up to "
                    + m_altTo;

        if (in_use + TREASURE > max) {
            LOG.warn("--------------");
            LOG.warn("BEWARE !!! " + info);
            LOG.warn("--------------");
        }
        else {
            LOG.debug(info);
        }
    }

    /**
     * This contains basically two paths: 1. check if the given port already is valid rmi registry. Use that one if
     * possible 2. make a new one at that port otherwise. Must also be validated.
     * 
     * @param port to select.
     * 
     * @return input port if successful or UNSELECTED
     */
    private Integer select(int port)
    {
        if (reuseRegistry(port)) {
            LOG.debug("Reuse Registry on " + port);
            return port;

        }
        else if (createNewRegistry(port)) {
            LOG.debug("Created Registry on " + port);
            return port;
        }
        // fail
        return UNSELECTED;

    }

    private boolean createNewRegistry(int port)
    {
        try {
            Registry registry = LocateRegistry.createRegistry(port);

            return verifyRegistry(registry);

        } catch (Exception e) {
            //
        }

        return false;
    }

    private boolean reuseRegistry(int port)
    {
        Registry reg = null;
        try {
            reg = LocateRegistry.getRegistry(port);
            return verifyRegistry(reg);

        } catch (Exception e) {
            // exception? then its not a fine registry.
        }
        return false;

    }

    private boolean verifyRegistry(Registry reg)
    {
        if (reg != null) {
            // test:
            try {
                String[] objectsRemote = reg.list();

                for (String r : objectsRemote) {
                    LOG.info("-- Remotely available already: " + r);
                }
                return true;

            } catch (Exception ex) {
                // exception? then its not a fine registry.
            }
        }
        return false;
    }

    public String getHost()
    {
        return m_host;
    }

    public int getPort()
    {
        return m_port;
    }
}
