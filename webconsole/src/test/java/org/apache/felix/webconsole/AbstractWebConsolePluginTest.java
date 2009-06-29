/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.webconsole;


import java.lang.reflect.Method;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;


public class AbstractWebConsolePluginTest extends TestCase
{

    private Method getGetResourceMethod;


    protected void setUp() throws Exception
    {
        super.setUp();

        getGetResourceMethod = AbstractWebConsolePlugin.class.getDeclaredMethod( "getGetResourceMethod", null );
        getGetResourceMethod.setAccessible( true );
    }


    public void test_getGetResourceMethod_no_provider() throws Exception
    {

        // test without resource provider
        final TestPlugin test = new TestPlugin()
        {
            protected Object getResourceProvider()
            {
                return null;
            }
        };
        assertNull( test.getResourceProvider() );
        assertNull( getGetResourceMethod.invoke( test, null ) );

        // test with default resource provider but no method
        final TestPlugin test2 = new TestPlugin();
        assertEquals( test2, test2.getResourceProvider() );
        assertNull( getGetResourceMethod.invoke( test2, null ) );
    }


    public void test_getGetResourceMethod_base_class() throws Exception
    {
        // test with default resource provider, public method
        final TestPlugin testPublic = new PublicTestPlugin();
        assertEquals( testPublic, testPublic.getResourceProvider() );
        assertNotNull( getGetResourceMethod.invoke( testPublic, null ) );

        // test with default resource provider, package private method
        final TestPlugin testDefault = new PackageTestPlugin();
        assertEquals( testDefault, testDefault.getResourceProvider() );
        assertNull( getGetResourceMethod.invoke( testDefault, null ) );

        // test with default resource provider, protected method
        final TestPlugin testProtected = new ProtectedTestPlugin();
        assertEquals( testProtected, testProtected.getResourceProvider() );
        assertNotNull( getGetResourceMethod.invoke( testProtected, null ) );

        // test with default resource provider, private method
        final TestPlugin testPrivate = new PrivateTestPlugin();
        assertEquals( testPrivate, testPrivate.getResourceProvider() );
        assertNotNull( getGetResourceMethod.invoke( testPrivate, null ) );
    }


    public void test_getGetResourceMethod_extension_class() throws Exception
    {
        // test with default resource provider, public method
        final TestPlugin testPublic = new PublicTestPlugin()
        {
        };
        assertEquals( testPublic, testPublic.getResourceProvider() );
        assertNotNull( getGetResourceMethod.invoke( testPublic, null ) );

        // test with default resource provider, package private method
        final TestPlugin testDefault = new PackageTestPlugin()
        {
        };
        assertEquals( testDefault, testDefault.getResourceProvider() );
        assertNull( getGetResourceMethod.invoke( testDefault, null ) );

        // test with default resource provider, protected method
        final TestPlugin testProtected = new ProtectedTestPlugin()
        {
        };
        assertEquals( testProtected, testProtected.getResourceProvider() );
        assertNotNull( getGetResourceMethod.invoke( testProtected, null ) );

        // test with default resource provider, private method
        final TestPlugin testPrivate = new PrivateTestPlugin()
        {
        };
        assertEquals( testPrivate, testPrivate.getResourceProvider() );
        assertNull( getGetResourceMethod.invoke( testPrivate, null ) );
    }

    private static class PrivateTestPlugin extends TestPlugin
    {
        private URL getResource( String name )
        {
            return null;
        }
    }

    private static class ProtectedTestPlugin extends TestPlugin
    {
        protected URL getResource( String name )
        {
            return null;
        }
    }

    private static class PackageTestPlugin extends TestPlugin
    {
        URL getResource( String name )
        {
            return null;
        }
    }

    private static class PublicTestPlugin extends TestPlugin
    {
        public URL getResource( String name )
        {
            return null;
        }
    }

    private static class TestPlugin extends AbstractWebConsolePlugin
    {
        public String getLabel()
        {
            return "test";
        }


        public String getTitle()
        {
            return "Test";
        }


        protected void renderContent( HttpServletRequest req, HttpServletResponse res )
        {
        }

    }
}
