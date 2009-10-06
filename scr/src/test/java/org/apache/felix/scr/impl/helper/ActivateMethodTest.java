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
package org.apache.felix.scr.impl.helper;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.instances.AcceptMethod;
import org.apache.felix.scr.impl.metadata.instances.BaseObject;
import org.apache.felix.scr.impl.metadata.instances.Level1Object;
import org.apache.felix.scr.impl.metadata.instances.Level3Object;
import org.apache.felix.scr.impl.metadata.instances2.Level2Object;
import org.easymock.EasyMock;
import org.osgi.service.component.ComponentContext;


public class ActivateMethodTest extends TestCase
{

    private static final Class ACCEPT_METHOD_CLASS = AcceptMethod.class;

    private ComponentContext m_ctx;

    BaseObject base = new BaseObject();

    Level1Object level1 = new Level1Object();

    Level2Object level2 = new Level2Object();

    Level3Object level3 = new Level3Object();

    protected void setUp() throws Exception
    {
        super.setUp();

        m_ctx = (ComponentContext) EasyMock.createNiceMock( ComponentContext.class );
        EasyMock.expect( m_ctx.getProperties() ).andReturn( new Hashtable() ).anyTimes();
        EasyMock.replay( new Object[]
            { m_ctx } );

    }


    public void test_private_no_arg() throws Exception
    {
        checkMethod( base, "activate_no_arg" );

        // activate_no_arg is private to BaseObject and must not be
        // accessible from extensions
        ensureMethodNotFoundMethod( level1, "activate_no_arg" );
        ensureMethodNotFoundMethod( level2, "activate_no_arg" );
        ensureMethodNotFoundMethod( level3, "activate_no_arg" );
    }


    public void test_protected_activate_comp() throws Exception
    {
        // activate_comp is protected in BaseObject and must be accessible
        // in all instances
        checkMethod( base, "activate_comp" );
        checkMethod( level1, "activate_comp" );
        checkMethod( level2, "activate_comp" );
        checkMethod( level3, "activate_comp" );
    }


    public void test_private_activate_level1_bundle() throws Exception
    {
        // activate_level1_bundle is private in Level1Object and must be
        // accessible in Level1Object only
        ensureMethodNotFoundMethod( base, "activate_level1_bundle" );
        checkMethod( level1, "activate_level1_bundle" );
        ensureMethodNotFoundMethod( level2, "activate_level1_bundle" );
        ensureMethodNotFoundMethod( level3, "activate_level1_bundle" );
    }


    public void test_protected_activate_level1_map() throws Exception
    {
        // activate_level1_map is protected in Level1Object and must be
        // accessible in Level1Object and extensions but not in BaseObject
        ensureMethodNotFoundMethod( base, "activate_level1_map" );
        checkMethod( level1, "activate_level1_map" );
        checkMethod( level2, "activate_level1_map" );
        checkMethod( level3, "activate_level1_map" );
    }


    public void test_private_activate_comp_map() throws Exception
    {
        // private_activate_comp_map is private in Level2Object and must be
        // accessible in Level2Object only
        ensureMethodNotFoundMethod( base, "activate_comp_map" );
        ensureMethodNotFoundMethod( level1, "activate_comp_map" );
        checkMethod( level2, "activate_comp_map" );
        checkMethod( level3, "activate_comp_map" );
    }


    public void test_public_activate_collision() throws Exception
    {
        // activate_collision is private in Level2Object and must be
        // accessible in Level2Object only.
        // also the method is available taking no arguments and a single
        // map argument which takes precedence and which we expect
        ensureMethodNotFoundMethod( base, "activate_collision" );
        ensureMethodNotFoundMethod( level1, "activate_collision" );
        checkMethod( level2, "activate_collision" );
        checkMethod( level3, "activate_collision" );
    }


    public void test_package_activate_comp_bundle() throws Exception
    {
        // activate_comp_bundle is package private and thus visible in
        // base and level1 but not in level 2 (different package) and
        // level 3 (inheritance through different package)

        checkMethod( base, "activate_comp_bundle" );
        checkMethod( level1, "activate_comp_bundle" );
        ensureMethodNotFoundMethod( level2, "activate_comp_bundle" );
        ensureMethodNotFoundMethod( level3, "activate_comp_bundle" );
    }


    public void test_getPackage() throws Exception
    {
        Class dpc = getClass().getClassLoader().loadClass( "DefaultPackageClass" );
        assertEquals( "", BaseMethod.getPackageName( dpc ) );

        assertEquals( "org.apache.felix.scr.impl.metadata.instances", BaseMethod.getPackageName( base.getClass() ) );
    }


    public void test_accept() throws Exception
    {
        // public visible unless returning non-void
        assertMethod( true, "public_void", false, false );
        assertMethod( false, "public_string", false, false );

        // protected visible unless returning non-void
        assertMethod( true, "protected_void", false, false );
        assertMethod( false, "protected_string", false, false );

        // private not visible
        assertMethod( false, "private_void", false, false );
        assertMethod( false, "private_string", false, false );

        // private visible unless returning non-void
        assertMethod( true, "private_void", true, false );
        assertMethod( false, "private_string", true, false );
        assertMethod( true, "private_void", true, true );
        assertMethod( false, "private_string", true, true );

        // private not visible, accept package is ignored
        assertMethod( false, "private_void", false, true );
        assertMethod( false, "private_string", false, true );

        // package not visible
        assertMethod( false, "package_void", false, false );
        assertMethod( false, "package_string", false, false );

        // package visible unless returning non-void
        assertMethod( true, "package_void", false, true );
        assertMethod( false, "package_string", false, true );
        assertMethod( true, "package_void", true, true );
        assertMethod( false, "package_string", true, true );

        // package not visible, accept private is ignored
        assertMethod( false, "package_void", true, false );
        assertMethod( false, "package_string", true, false );
    }


    public void test_suitable_method_selection() throws Exception
    {
        // this would be the protected BaseObject.activate_suitable
        checkMethod( base, "activate_suitable" );
        checkMethod( level1, "activate_suitable" );

        // this would be the private Level2Object.activate_suitable
        checkMethod( level2, "activate_suitable" );

        // this must fail to find a method, since Level2Object's activate_suitable
        // is private and terminates the search for Level3Object
        ensureMethodNotFoundMethod( level3, "activate_suitable" );
    }


    //---------- internal

    /**
     * Checks whether a method with the given name can be found for the
     * activate/deactivate method parameter list and whether the method returns
     * its name when called.
     *
     * @param obj
     * @param methodName
     */
    private void checkMethod( BaseObject obj, String methodName )
    {
        ComponentMetadata metadata = new ComponentMetadata( 0 )
        {
            public boolean isDS11()
            {
                return true;
            }
        };
        ImmediateComponentManager icm = new ImmediateComponentManager( null, null, metadata );
        ActivateMethod am = new ActivateMethod( icm, methodName, methodName != null, obj.getClass() );
        am.invoke( obj, new ActivateMethod.ActivatorParameter( m_ctx, -1 ) );
        Method m = get(am, "m_method");
        assertNotNull( m );
        assertEquals( methodName, m.getName() );
        assertEquals( methodName, obj.getCalledMethod() );
    }


    /**
     * Ensures no method with the given name accepting any of the
     * activate/deactive method parameters can be found.
     *
     * @param obj
     * @param methodName
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void ensureMethodNotFoundMethod( BaseObject obj, String methodName )
    {
        ComponentMetadata metadata = new ComponentMetadata( 0 )
        {
            public boolean isDS11()
            {
                return true;
            }
        };
        ImmediateComponentManager icm = new ImmediateComponentManager( null, null, metadata );
        ActivateMethod am = new ActivateMethod( icm, methodName, methodName != null, obj.getClass() );
        am.invoke( obj, new ActivateMethod.ActivatorParameter( m_ctx, -1 ) );
        assertNull( get( am, "m_method" ) );
        assertNull( obj.getCalledMethod() );
    }


    private void assertMethod( boolean expected, String methodName, boolean acceptPrivate, boolean acceptPackage )
        throws NoSuchMethodException
    {
        Method method = ACCEPT_METHOD_CLASS.getDeclaredMethod( methodName, null );
        boolean accepted = BaseMethod.accept( method, acceptPrivate, acceptPackage );
        assertEquals( expected, accepted );
    }


    private static Method get( final BaseMethod baseMethod, final String fieldName )
    {
        try
        {
            Field field = BaseMethod.class.getDeclaredField( fieldName );
            field.setAccessible( true );
            Object value = field.get( baseMethod );
            if ( value == null || value instanceof Method )
            {
                return ( Method ) value;
            }
            fail( "Field " + field + " is not of type Method" );
        }
        catch ( Throwable t )
        {
            fail( "Failure accessing field " + fieldName + " in " + baseMethod + ": " + t );
        }

        // Compiler does not know about fail()
        return null;
    }
}
