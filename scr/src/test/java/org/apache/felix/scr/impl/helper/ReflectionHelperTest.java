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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.apache.felix.scr.impl.helper.ReflectionHelper;
import org.apache.felix.scr.impl.metadata.instances.AcceptMethod;
import org.apache.felix.scr.impl.metadata.instances.BaseObject;
import org.apache.felix.scr.impl.metadata.instances.Level1Object;
import org.apache.felix.scr.impl.metadata.instances.Level3Object;
import org.apache.felix.scr.impl.metadata.instances.MethodNameException;
import org.apache.felix.scr.impl.metadata.instances2.Level2Object;


public class ReflectionHelperTest extends TestCase
{

    private static final Class ACCEPT_METHOD_CLASS = AcceptMethod.class;

    BaseObject base = new BaseObject();

    Level1Object level1 = new Level1Object();

    Level2Object level2 = new Level2Object();

    Level3Object level3 = new Level3Object();


    public void test_sentinel() throws Exception
    {
        assertNotNull( "Sentinel is null", ReflectionHelper.SENTINEL );
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
        assertEquals( "", ReflectionHelper.getPackageName( dpc ) );

        assertEquals( "org.apache.felix.scr.impl.metadata.instances", ReflectionHelper.getPackageName( base.getClass() ) );
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
        try {
            checkMethod( level3, "activate_suitable" );
            fail("Level3Object must not find activate_suitable method");
        } catch (NoSuchMethodException nsme) {
            // expecting method lookup abort on suitable private
            // method in Level2Object class
        }
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
    private void checkMethod( Object obj, String methodName ) throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException
    {
        Method method = ReflectionHelper.getMethod( obj.getClass(), methodName,
            ReflectionHelper.ACTIVATE_ACCEPTED_PARAMETERS );
        try
        {
            method.invoke( obj, new Object[method.getParameterTypes().length] );
            fail( "Expected MethodNameException being thrown" );
        }
        catch ( InvocationTargetException ite )
        {
            Throwable target = ite.getTargetException();
            assertTrue( "Expected MethodNameException", target instanceof MethodNameException );
            assertEquals( methodName, target.getMessage() );
        }
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
    private void ensureMethodNotFoundMethod( Object obj, String methodName ) throws InvocationTargetException,
        IllegalAccessException
    {
        try
        {
            checkMethod( obj, methodName );
            fail( "Expected to not find method " + methodName + " for " + obj.getClass() );
        }
        catch ( NoSuchMethodException nsme )
        {
            // expected not to find a method
        }
    }


    private void assertMethod( boolean expected, String methodName, boolean acceptPrivate, boolean acceptPackage )
        throws NoSuchMethodException
    {
        Method method = ACCEPT_METHOD_CLASS.getDeclaredMethod( methodName, null );
        boolean accepted = ReflectionHelper.accept( method, acceptPrivate, acceptPackage );
        assertEquals( expected, accepted );
    }
}
