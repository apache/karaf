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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.service.io.ConnectorService;

/**
 * <tt>ConnectorServiceTest</tt> represents test class for {@link ConnectorService}.
 * 
 * @version $Rev$ $Date$
 */
public class ConnectorServiceTest extends TestCase
{

    private BundleContext m_bundleContext;
    private ConnectorService m_service;
    private ServiceListener m_serviceListener;
    private long m_serviceId;

    private ServiceRegistration registerConnectionFactory(ConnectionFactoryMock connFactory, Dictionary props)
    {
        // service reference for ConnectionFactory service
        ServiceReference reference = (ServiceReference) Mockito.mock(ServiceReference.class);
        Mockito.when(reference.getProperty(ConnectionFactory.IO_SCHEME)).thenReturn(
                props.get(ConnectionFactory.IO_SCHEME));
        Mockito.when(reference.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(++m_serviceId));
        Mockito.when(reference.getProperty(Constants.SERVICE_RANKING)).thenReturn(props.get(Constants.SERVICE_RANKING));
        // service registration for ConnectionFactory service
        ServiceRegistration registration = (ServiceRegistration) Mockito.mock(ServiceRegistration.class);
        Mockito.when(registration.getReference()).thenReturn(reference);
        // service event
        ServiceEvent registeredEvent = (ServiceEvent) Mockito.mock(ServiceEvent.class);
        Mockito.when(registeredEvent.getServiceReference()).thenReturn(reference);
        Mockito.when(new Integer(registeredEvent.getType())).thenReturn(new Integer(ServiceEvent.REGISTERED));
        Mockito.when(m_bundleContext.getService(reference)).thenReturn(connFactory);
        // sending registration event
        // service tracker for ConnectionFactory service used by ConnectorService
        // will be informed about service registration
        m_serviceListener.serviceChanged(registeredEvent);

        return registration;
    }

    private ConnectorService getConnectorService()
    {
        return m_service;
    }

    public void setUp()
    {
        m_bundleContext = (BundleContext) Mockito.mock(BundleContext.class);
        ArgumentCaptor argument = ArgumentCaptor.forClass(ServiceListener.class);
        m_service = new ConnectorServiceImpl(m_bundleContext);
        try
        {
            ((BundleContext) Mockito.verify(m_bundleContext)).addServiceListener((ServiceListener) argument.capture(),
                    Mockito.anyString());
        } catch (InvalidSyntaxException e)
        {
            fail();
        }
        // getting from captor serviceListener which listen to ConnectionFactory service
        m_serviceListener = (ServiceListener) argument.getValue();
    }

    public void tearDown()
    {

    }

    /**
     * Tests all methods provided by {@link ConnectorService}.
     * 
     * @throws Exception
     */
    public void testOpen() throws Exception
    {
        ConnectionFactoryMock connFactory = new ConnectionFactoryMock();
        Dictionary props = new Hashtable();
        props.put(ConnectionFactory.IO_SCHEME, "file");
        ServiceRegistration registration = registerConnectionFactory(connFactory, props);
        ConnectorService service = getConnectorService();

        Connection connection = service.open("file://test.txt");
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ_WRITE, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned Connection", connection);

        connection = service.open("file://test.txt", ConnectorService.READ);
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned Connection", connection);

        connection = service.open("file://test.txt", ConnectorService.WRITE);
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.WRITE, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned Connection", connection);

        connection = service.open("file://test.txt", ConnectorService.READ, true);
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ, connFactory.getMode());
        assertEquals(true, connFactory.isTimeout());
        assertNotNull("checks returned Connection", connection);

        try
        {
            connection = service.open("http://test.txt", ConnectorService.READ);
            fail("Connection shouldn't be created");
        } catch (ConnectionNotFoundException e)
        {
            // "expected"
        }

        try
        {
            service.open("file.txt");
            fail("Illegal format of uri");
        } catch (IllegalArgumentException e)
        {
            // expected
        }

        DataInputStream dataInStream = service.openDataInputStream("file://test.txt");
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned DataInputStream", dataInStream);

        DataOutputStream dataOutStream = service.openDataOutputStream("file://test.txt");
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.WRITE, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned DataOutputStream", dataOutStream);

        InputStream inStream = service.openInputStream("file://test.txt");
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned InputStream", inStream);

        OutputStream outStream = service.openOutputStream("file://test.txt");
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.WRITE, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned OutputStream", outStream);

        registration.unregister();
    }

    /**
     * Registers two ConnectionFactory services with same IO_SCHEME. One with higher service.ranking Connector Service
     * should pickup service with highest service.ranking.
     * 
     * @throws Exception
     */
    public void testHighestRanking() throws Exception
    {
        ConnectionFactoryMock connFactory = new ConnectionFactoryMock();
        Dictionary props = new Hashtable();
        props.put(ConnectionFactory.IO_SCHEME, "file");
        registerConnectionFactory(connFactory, props);

        ConnectionFactoryMock connFactory2 = new ConnectionFactoryMock();
        props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
        registerConnectionFactory(connFactory2, props);

        ConnectorService service = getConnectorService();

        Connection connection = service.open("file://test.txt");
        assertEquals("uri checks for lowest ranking service", null, connFactory.getName());
        assertEquals("file://test.txt", connFactory2.getName());
        assertEquals(ConnectorService.READ_WRITE, connFactory2.getMode());
        assertEquals(false, connFactory2.isTimeout());
        assertNotNull("Connection should be created", connection);
    }

    /**
     * Registers two ConnectionFactory services with same IO_SCHEME. Connector Service should pickup service with lowest
     * service.id.
     * 
     * @throws Exception
     */
    public void testLowestServiceId() throws Exception
    {
        ConnectionFactoryMock connFactory = new ConnectionFactoryMock();
        Dictionary props = new Hashtable();
        props.put(ConnectionFactory.IO_SCHEME, "file");
        ServiceRegistration registration = registerConnectionFactory(connFactory, props);

        ConnectionFactoryMock connFactory2 = new ConnectionFactoryMock();
        ServiceRegistration registration2 = registerConnectionFactory(connFactory2, props);

        ConnectorService service = getConnectorService();

        Connection connection = service.open("file://test.txt");
        assertEquals("uri checks for highest service.id", null, connFactory2.getName());
        assertEquals("uri checks for lowest service.id", "file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ_WRITE, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned Connection", connection);

        Long serviceId1 = (Long) registration.getReference().getProperty(Constants.SERVICE_ID);
        Long serviceId2 = (Long) registration2.getReference().getProperty(Constants.SERVICE_ID);

        assertTrue(serviceId1.longValue() < serviceId2.longValue());

        registration.unregister();
        registration2.unregister();
    }

    /**
     * Tests ConnectionFactory service which support 3 different schemes.
     * 
     * @throws Exception
     */
    public void testMultipleScheme() throws Exception
    {
        ConnectionFactoryMock connFactory = new ConnectionFactoryMock();
        Dictionary props = new Hashtable();
        props.put(ConnectionFactory.IO_SCHEME, new String[]
        { "file", "http", "sms" });
        ServiceRegistration registration = registerConnectionFactory(connFactory, props);
        ConnectorService service = getConnectorService();

        Connection connection = service.open("file://test.txt");
        assertEquals("file://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ_WRITE, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned connection", connection);

        connection = service.open("http://test.txt", ConnectorService.READ);
        assertEquals("http://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned connection", connection);

        connection = service.open("sms://test.txt", ConnectorService.READ);
        assertEquals("sms://test.txt", connFactory.getName());
        assertEquals(ConnectorService.READ, connFactory.getMode());
        assertEquals(false, connFactory.isTimeout());
        assertNotNull("checks returned connection", connection);

        try
        {
            connection = service.open("ftp://test.txt", ConnectorService.READ);
            fail("Connection shouldn't be created");
        } catch (ConnectionNotFoundException e)
        {
            // "expected"
        }

        registration.unregister();
    }

}