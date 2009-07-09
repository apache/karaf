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
package org.apache.felix.scr.impl;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


/**
 * The <code>ReflectionHelper</code> class provides utility methods to find out
 * about binding and activation methods in components.
 */
final class ReflectionHelper
{

    // Method instance to implement tristate behaviour on method fields:
    // unchecked (SENTINEL), no method (null), valid method (method object)
    static final Method SENTINEL;

    // class references to simplify parameter checking
    static final Class COMPONENT_CONTEXT_CLASS = ComponentContext.class;
    static final Class BUNDLE_CONTEXT_CLASS = BundleContext.class;
    static final Class MAP_CLASS = Map.class;
    static final Class INTEGER_CLASS = Integer.class;

    // Helper used to find the best matching activate and modified methods
    static final ActivatorMethodTester ACTIVATE_ACCEPTED_PARAMETERS = new ActivatorMethodTester( new Class[]
        { COMPONENT_CONTEXT_CLASS, BUNDLE_CONTEXT_CLASS, MAP_CLASS } );

    // Helper used to find the best matching deactivate method
    static final ActivatorMethodTester DEACTIVATE_ACCEPTED_PARAMETERS = new ActivatorMethodTester( new Class[]
        { COMPONENT_CONTEXT_CLASS, BUNDLE_CONTEXT_CLASS, MAP_CLASS, Integer.TYPE, INTEGER_CLASS } );

    static
    {
        Method tmpSentinel = null;
        try
        {
            tmpSentinel = ReflectionHelper.class.getDeclaredMethod( "sentinel", null );
        }
        catch ( Throwable t )
        {
            // don't care for the reason
        }

        SENTINEL = tmpSentinel;
    }


    // sentinel method used to assign to the SENTINEL field
    private static final void sentinel()
    {
    }


    /**
     * Finds the named public or protected method in the given class or any
     * super class. If such a method is found, its accessibility is enfored by
     * calling the <code>Method.setAccessible</code> method if required and
     * the method is returned. Enforcing accessibility is required to support
     * invocation of protected methods.
     *
     * @param objectClass The <code>Class</code> at which to start looking for
     *      a matching method
     * @param name The name of the method.
     * @param parameterTypes The list of suitable parmameters. Each class is
     *      is asked for a declared method of the given name with parameters
     *      from this list.
     * @param only Whether to only look at the declared methods of the given
     *      class or also inspect the super classes.
     *
     * @return The named method with enforced accessibility
     *
     * @throws NoSuchMethodException If no public or protected method with
     *      the given name can be found in the class or any of its super classes.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to access the desired method.
     */
    static Method getMethod( final Class objectClass, final String name, final MethodTester tester )
        throws NoSuchMethodException, InvocationTargetException
    {
        // whether we accept package private methods
        boolean acceptPackage = true;
        final String packageName = getPackageName( objectClass );
        final Class[] parameterTypesList = tester.getParameterLists();

        for ( Class clazz = objectClass; clazz != null; clazz = clazz.getSuperclass() )
        {
            // turns false on first package not equal to the package of objectClass
            acceptPackage &= packageName.equals( getPackageName( clazz ) );
            final boolean acceptPrivate = clazz == objectClass;

            // check parameter types first
            for ( int i = 0; i < parameterTypesList.length; i++ )
            {
                Class[] parameterTypes = new Class[]
                    { parameterTypesList[i] };

                try
                {
                    // find the declared method in this class
                    return getMethod( clazz, name, parameterTypes, acceptPrivate, acceptPackage );
                }
                catch ( NoSuchMethodException nsme )
                {
                    // ignore for now
                }
                catch ( Throwable throwable )
                {
                    // unexpected problem accessing the method, don't let everything
                    // blow up in this situation, just throw a declared exception
                    throw new InvocationTargetException( throwable, "Unexpected problem trying to get method " + name );
                }
            }

            // check methods with MethodTester
            Method[] methods = clazz.getDeclaredMethods();
            for ( int i = 0; i < methods.length; i++ )
            {
                if ( methods[i].getName().equals( name ) && tester.isSuitable( methods[i] )
                    && accept( methods[i], acceptPrivate, acceptPackage ) )
                {
                    // check modifiers etc.
                    return methods[i];
                }
            }

            // finally check method with no arguments
            if ( tester.acceptEmpty() )
            {
                try
                {
                    // find the declared method in this class
                    return getMethod( clazz, name, null, clazz == objectClass, acceptPackage );
                }
                catch ( NoSuchMethodException nsme )
                {
                    // ignore for now
                }
                catch ( Throwable throwable )
                {
                    // unexpected problem accessing the method, don't let everything
                    // blow up in this situation, just throw a declared exception
                    throw new InvocationTargetException( throwable, "Unexpected problem trying to get method " + name );
                }
            }
        }

        // walked up the complete super class hierarchy and still not found
        // anything, sigh ...
        throw new NoSuchMethodException( name );
    }


    /**
     * Finds the named public or protected method in the given class or any
     * super class. If such a method is found, its accessibility is enfored by
     * calling the <code>Method.setAccessible</code> method if required and
     * the method is returned. Enforcing accessibility is required to support
     * invocation of protected methods.
     *
     * @param clazz The <code>Class</code> which provides the method.
     * @param name The name of the method.
     * @param parameterTypes The parameters to the method. Passing
     *      <code>null</code> is equivalent to using an empty array.
     *
     * @return The named method with enforced accessibility
     *
     * @throws NoSuchMethodException If no public or protected method with
     *      the given name can be found in the class or any of its super classes.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to access the desired method.
     */
    static Method getMethod( Class clazz, String name, Class[] parameterTypes, boolean acceptPrivate,
        boolean acceptPackage ) throws NoSuchMethodException, InvocationTargetException
    {
        try
        {
            // find the declared method in this class
            Method method = clazz.getDeclaredMethod( name, parameterTypes );

            // accept public and protected methods only and ensure accessibility
            if ( accept( method, acceptPrivate, acceptPackage ) )
            {
                return method;
            }
        }
        catch ( NoSuchMethodException nsme )
        {
            // forward to caller
            throw nsme;
        }
        catch ( Throwable throwable )
        {
            // unexpected problem accessing the method, don't let everything
            // blow up in this situation, just throw a declared exception
            throw new InvocationTargetException( throwable, "Unexpected problem trying to get method " + name );
        }

        // walked up the complete super class hierarchy and still not found
        // anything, sigh ...
        throw new NoSuchMethodException( name );
    }


    /**
     * Returns <code>true</code> if the method is acceptable to be returned from the
     * {@link #getMethod(Class, String, Class[], boolean, boolean)} and also
     * makes the method accessible.
     * <p>
     * This method returns <code>true</code> iff:
     * <ul>
     * <li>The method has <code>void</code> return type</li>
     * <li>Is not static</li>
     * <li>Is public or protected</li>
     * <li>Is private and <code>acceptPrivate</code> is <code>true</code></li>
     * <li>Is package private and <code>acceptPackage</code> is <code>true</code></li>
     * </ul>
     * <p>
     * This method is package private for unit testing purposes. It is not
     * meant to be called from client code.
     *
     * @param method The method to check
     * @param acceptPrivate Whether a private method is acceptable
     * @param acceptPackage Whether a package private method is acceptable
     * @return
     */
    static boolean accept( Method method, boolean acceptPrivate, boolean acceptPackage )
    {
        // method must be void
        if ( Void.TYPE != method.getReturnType() )
        {
            return false;
        }

        // check modifiers now
        int mod = method.getModifiers();

        // no static method
        if ( Modifier.isStatic( mod ) )
        {
            return false;
        }

        // accept public and protected methods
        if ( Modifier.isPublic( mod ) || Modifier.isProtected( mod ) )
        {
            method.setAccessible( true );
            return true;
        }

        // accept private if accepted
        if ( Modifier.isPrivate( mod ) )
        {
            if ( acceptPrivate )
            {
                method.setAccessible( acceptPrivate );
                return true;
            }

            return false;
        }

        // accept default (package)
        if ( acceptPackage )
        {
            method.setAccessible( true );
            return true;
        }

        // else don't accept
        return false;
    }


    /**
     * Returns the name of the package to which the class belongs or an
     * empty string if the class is in the default package.
     */
    static String getPackageName( Class clazz )
    {
        String name = clazz.getName();
        int dot = name.lastIndexOf( '.' );
        return ( dot > 0 ) ? name.substring( 0, dot ) : "";
    }

    //---------- inner classes

    static interface MethodTester
    {

        /**
         * Returns <code>true</code> if methods without arguments are acceptable.
         */
        boolean acceptEmpty();


        /**
         * Returns <code>true</code> if the method <code>m</code> is suitable for
         * this tester.
         */
        boolean isSuitable( Method m );


        /**
         * Returns an array of parameters which are acceptable as single parameter
         * arguments to methods.
         */
        Class[] getParameterLists();
    }

    static final class ActivatorMethodTester implements ReflectionHelper.MethodTester
    {
        private final Class[] parameterLists;
        private final Set methods;


        ActivatorMethodTester( Class[] acceptedParameters )
        {
            parameterLists = acceptedParameters;
            methods = new HashSet();
            methods.addAll( Arrays.asList( acceptedParameters ) );
        }


        public boolean acceptEmpty()
        {
            return true;
        }


        public boolean isSuitable( Method m )
        {
            Class[] types = m.getParameterTypes();

            // require two or more arguments
            if ( types.length < 2 )
            {
                return false;
            }

            // check for argument types
            for ( int i = 0; i < types.length; i++ )
            {
                if ( !methods.contains( types[i] ) )
                {
                    return false;
                }
            }

            return true;
        }


        public Class[] getParameterLists()
        {
            return parameterLists;
        }

    }

}
