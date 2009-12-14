/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.service.io.ConnectorService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>
 * The Connector Service should be called to create and open <code>javax.microedition.io.Connection</code> objects.
 * </p>
 * 
 * @see ConnectorService
 * @version $Rev$ $Date$
 */
public class ConnectorServiceImpl implements ConnectorService
{

    ServiceTracker m_connFactoryTracker;

    /**
     * Constructs new ConnectorService.
     * 
     * @param context
     *            bundleContext @see {@link BundleContext}.
     */
    public ConnectorServiceImpl(BundleContext context)
    {
        this.m_connFactoryTracker = new ServiceTracker(context, ConnectionFactory.class.getName(), null);
        m_connFactoryTracker.open();
    }

    /**
     * Stops ConnectorService. This method closes {@link ConnectionFactory} serviceTracker.
     * 
     * @see ServiceTracker#close()
     */
    public void stop()
    {
        m_connFactoryTracker.close();
    }

    /**
     * @see org.osgi.service.io.ConnectorService#openDataInputStream(java.lang.String)
     */
    public DataInputStream openDataInputStream(String name) throws IOException
    {
        Connection connection = open(name, READ, false);
        if (!(connection instanceof InputConnection))
        {
            try
            {
                connection.close();
            } catch (IOException ioex)
            {

            }

            throw new IOException("Connection doesn't implement InputConnection" + connection.getClass());
        }

        return ((InputConnection) connection).openDataInputStream();
    }

    /**
     * @see org.osgi.service.io.ConnectorService#openDataOutputStream(java.lang.String)
     */
    public DataOutputStream openDataOutputStream(String name) throws IOException
    {
        Connection connection = open(name, WRITE, false);
        if (!(connection instanceof OutputConnection))
        {
            try
            {
                connection.close();
            } catch (IOException ioex)
            {

            }

            throw new IOException("Connection doesn't implement OutputConnection" + connection.getClass());
        }

        return ((OutputConnection) connection).openDataOutputStream();
    }

    /**
     * @see org.osgi.service.io.ConnectorService#openInputStream(java.lang.String)
     */
    public InputStream openInputStream(String name) throws IOException
    {
        Connection connection = open(name, READ, false);
        if (!(connection instanceof InputConnection))
        {
            try
            {
                connection.close();
            } catch (IOException ioex)
            {

            }

            throw new IOException("Connection doesn't implement InputConnection" + connection.getClass());
        }

        return ((InputConnection) connection).openInputStream();
    }

    /**
     * @see org.osgi.service.io.ConnectorService#openOutputStream(java.lang.String)
     */
    public OutputStream openOutputStream(String name) throws IOException
    {
        Connection connection = open(name, WRITE, false);
        if (!(connection instanceof OutputConnection))
        {
            try
            {
                connection.close();
            } catch (IOException ioex)
            {

            }

            throw new IOException("Connection doesn't implement OutputConnection" + connection.getClass());
        }

        return ((OutputConnection) connection).openOutputStream();
    }

    /**
     * @see org.osgi.service.io.ConnectorService#open(String)
     */
    public Connection open(String name) throws IOException
    {
        return open(name, READ_WRITE, false);
    }

    /**
     * @see org.osgi.service.io.ConnectorService#open(String, int)
     */
    public Connection open(String name, int mode) throws IOException
    {
        return open(name, mode, false);
    }

    /**
     * @see org.osgi.service.io.ConnectorService#open(String, int, boolean)
     */
    public Connection open(String name, int mode, boolean timeouts) throws IOException
    {
        if (name == null)
        {
            throw new IllegalArgumentException("URI for the connection can't be null!");
        }

        // resolving scheme name
        int index = name.indexOf(":");
        if (index == -1)
        {
            throw new IllegalArgumentException("Can't resolve scheme name");
        }

        String scheme = name.substring(0, index);

        ConnectionFactory connFactory = resolveConnectionFactory(scheme);
        Connection connection = null;
        if (connFactory != null)
        {
            connection = connFactory.createConnection(name, mode, timeouts);
        }
        // if connection is not provided go to javax.microedition.io.Connector
        if (connection == null)
        {
            try
            {
                connection = Connector.open(name, mode, timeouts);
            } catch (Exception ex)
            {

            }

        } else
        {
            return connection;
        }

        throw new ConnectionNotFoundException("Failed to create connection " + name);
    }

    /**
     * <p>
     * Resolves {@link ConnectionFactory} based on IO scheme name. If multiple ConnectionFactory services register with
     * the same scheme, method select the ConnectionFactory with highest value for service.ranking service registration
     * property or if more than one ConnectionFactory service has the highest value, the ConnectionFactory service with
     * the lowest service.id is selected.
     * </p>
     * 
     * @param scheme
     *            name of IO scheme.
     * @return {@link ConnectionFactory} which matched provided scheme.
     */
    private ConnectionFactory resolveConnectionFactory(String scheme)
    {
        ServiceReference[] references = m_connFactoryTracker.getServiceReferences();
        if (references == null || references.length == 0)
        {
            return null;
        }

        ServiceReference matchingRef = null;
        for (int i = 0; i < references.length; i++)
        {
            if (containsScheme(references[i], scheme))
            {
                if (matchingRef != null)
                {
                    int matchRanking = getServiceRanking(matchingRef);
                    int foundRanking = getServiceRanking(references[i]);
                    if (foundRanking > matchRanking)
                    {
                        matchingRef = references[i];
                    } else if (foundRanking == matchRanking && compareServiceId(references[i], matchingRef))
                    {
                        matchingRef = references[i];
                    }
                } else
                {
                    matchingRef = references[i];
                }
            }
        }
        if (matchingRef != null)
        {
            return (ConnectionFactory) m_connFactoryTracker.getService(matchingRef);
        }
        return null;
    }

    /**
     * Checks if provided {@link ServiceReference} contains in its service properties provided scheme name.
     * 
     * @param ref
     *            {@link ServiceReference}.
     * @param scheme
     *            name of IO scheme.
     * @return true if contains scheme name, false if not.
     */
    private boolean containsScheme(ServiceReference ref, String scheme)
    {
        Object property = ref.getProperty(ConnectionFactory.IO_SCHEME);
        if (property != null && property.equals(scheme))
        {
            return true;
        } else
        {
            if (property != null && property instanceof String[])
            {
                String[] schemes = (String[]) property;
                for (int index = 0; index < schemes.length; index++)
                {
                    if (schemes[index].equals(scheme))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Compare service.id of two {@link ServiceReference}'s.
     * 
     * @param foundRef
     *            {@link ServiceReference}.
     * @param prevRef
     *            {@link ServiceReference}.
     * @return true if first reference has lowest service.id than second, false if not.
     */
    private boolean compareServiceId(ServiceReference foundRef, ServiceReference prevRef)
    {
        Long foundServiceId = (Long) foundRef.getProperty(Constants.SERVICE_ID);
        Long prevServiceId = (Long) prevRef.getProperty(Constants.SERVICE_ID);
        if (foundServiceId.longValue() < prevServiceId.longValue())
        {
            return true;
        }

        return false;
    }

    /**
     * Gets service.ranking from provided {@link ServiceReference}.
     * 
     * @param ref
     *            {@link ServiceReference}.
     * @return service.ranking property value.
     */
    private int getServiceRanking(ServiceReference ref)
    {
        Object property = ref.getProperty(Constants.SERVICE_RANKING);
        if (property == null || !(property instanceof Integer))
        {
            return 0;
        } else
        {
            return ((Integer) property).intValue();
        }
    }

}