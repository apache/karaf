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

import java.io.IOException;
import java.net.BindException;

import javax.management.remote.JMXConnectorServer;
import javax.naming.NotContextException;

import junit.framework.TestCase;
import org.easymock.EasyMock;

public class ConnectorServerFactoryTest extends TestCase {

    private static final int TEST_MAX_RETRIES = 3;
    private static final long TEST_RETRY_DELAY_MS = 10;

    public void testHasCauseDirectMatch() {
        NotContextException nce = new NotContextException("test");
        assertTrue(ConnectorServerFactory.hasCause(nce, NotContextException.class));
    }

    public void testHasCauseNestedMatch() {
        NotContextException nce = new NotContextException("context not found");
        IOException ioe = new IOException("Cannot bind", nce);
        assertTrue(ConnectorServerFactory.hasCause(ioe, NotContextException.class));
    }

    public void testHasCauseDeeplyNested() {
        NotContextException nce = new NotContextException("context not found");
        IOException ioe = new IOException("Cannot bind", nce);
        RuntimeException rte = new RuntimeException("wrapper", ioe);
        assertTrue(ConnectorServerFactory.hasCause(rte, NotContextException.class));
    }

    public void testHasCauseNoMatch() {
        IOException ioe = new IOException("some error");
        assertFalse(ConnectorServerFactory.hasCause(ioe, NotContextException.class));
    }

    public void testHasCauseNullThrowable() {
        assertFalse(ConnectorServerFactory.hasCause(null, NotContextException.class));
    }

    public void testStartWithRetrySucceedsFirstAttempt() throws Exception {
        JMXConnectorServer mockServer = EasyMock.createMock(JMXConnectorServer.class);
        mockServer.start();
        EasyMock.expectLastCall().once();
        EasyMock.replay(mockServer);

        ConnectorServerFactory.startWithRetry(mockServer, TEST_MAX_RETRIES, TEST_RETRY_DELAY_MS);

        EasyMock.verify(mockServer);
    }

    public void testStartWithRetrySucceedsAfterNotContextException() throws Exception {
        JMXConnectorServer mockServer = EasyMock.createMock(JMXConnectorServer.class);
        // First call throws IOException caused by NotContextException
        mockServer.start();
        EasyMock.expectLastCall().andThrow(
                new IOException("Cannot bind", new NotContextException("context not found")));
        // Second call succeeds
        mockServer.start();
        EasyMock.expectLastCall().once();
        EasyMock.replay(mockServer);

        ConnectorServerFactory.startWithRetry(mockServer, TEST_MAX_RETRIES, TEST_RETRY_DELAY_MS);

        EasyMock.verify(mockServer);
    }

    public void testStartWithRetryThrowsImmediatelyOnBindException() throws Exception {
        JMXConnectorServer mockServer = EasyMock.createMock(JMXConnectorServer.class);
        BindException be = new BindException("Address already in use");
        IOException ioe = new IOException("Cannot bind", be);
        mockServer.start();
        EasyMock.expectLastCall().andThrow(ioe);
        EasyMock.replay(mockServer);

        try {
            ConnectorServerFactory.startWithRetry(mockServer, TEST_MAX_RETRIES, TEST_RETRY_DELAY_MS);
            fail("Expected IOException to be thrown");
        } catch (IOException ex) {
            assertSame(ioe, ex);
        }

        // Verify start() was only called once (no retry for BindException)
        EasyMock.verify(mockServer);
    }

    public void testStartWithRetryThrowsImmediatelyOnPlainIOException() throws Exception {
        JMXConnectorServer mockServer = EasyMock.createMock(JMXConnectorServer.class);
        IOException ioe = new IOException("some other error");
        mockServer.start();
        EasyMock.expectLastCall().andThrow(ioe);
        EasyMock.replay(mockServer);

        try {
            ConnectorServerFactory.startWithRetry(mockServer, TEST_MAX_RETRIES, TEST_RETRY_DELAY_MS);
            fail("Expected IOException to be thrown");
        } catch (IOException ex) {
            assertSame(ioe, ex);
        }

        EasyMock.verify(mockServer);
    }

    public void testStartWithRetryExhaustsRetries() throws Exception {
        JMXConnectorServer mockServer = EasyMock.createMock(JMXConnectorServer.class);
        IOException ioe = new IOException("Cannot bind", new NotContextException("context not found"));
        for (int i = 0; i < TEST_MAX_RETRIES; i++) {
            mockServer.start();
            EasyMock.expectLastCall().andThrow(ioe);
        }
        EasyMock.replay(mockServer);

        try {
            ConnectorServerFactory.startWithRetry(mockServer, TEST_MAX_RETRIES, TEST_RETRY_DELAY_MS);
            fail("Expected IOException to be thrown after exhausting retries");
        } catch (IOException ex) {
            assertSame(ioe, ex);
        }

        EasyMock.verify(mockServer);
    }

    public void testStartWithRetryRespectsInterrupt() throws Exception {
        JMXConnectorServer mockServer = EasyMock.createMock(JMXConnectorServer.class);
        IOException ioe = new IOException("Cannot bind", new NotContextException("context not found"));
        mockServer.start();
        EasyMock.expectLastCall().andThrow(ioe);
        EasyMock.replay(mockServer);

        Thread.currentThread().interrupt();
        try {
            ConnectorServerFactory.startWithRetry(mockServer, TEST_MAX_RETRIES, TEST_RETRY_DELAY_MS);
            fail("Expected IOException to be thrown on interrupt");
        } catch (IOException ex) {
            assertSame(ioe, ex);
            assertTrue("Thread interrupt flag should be set", Thread.currentThread().isInterrupted());
        } finally {
            // Clear interrupt flag for other tests
            Thread.interrupted();
        }

        EasyMock.verify(mockServer);
    }
}
