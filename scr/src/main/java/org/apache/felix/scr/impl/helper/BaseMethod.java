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
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.felix.scr.impl.helper.SuitableMethodNotAccessibleException;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;


/**
 * Component method to be invoked on service (un)binding.
 */
abstract class BaseMethod
{

    // class references to simplify parameter checking
    protected static final Class COMPONENT_CONTEXT_CLASS = ComponentContext.class;
    protected static final Class BUNDLE_CONTEXT_CLASS = BundleContext.class;
    protected static final Class SERVICE_REFERENCE_CLASS = ServiceReference.class;
    protected static final Class MAP_CLASS = Map.class;
    protected static final Class INTEGER_CLASS = Integer.class;

    private final AbstractComponentManager m_componentManager;

    private final String m_methodName;
    private final Class m_componentClass;

    private Method m_method = null;

    private State m_state;


    protected BaseMethod( final AbstractComponentManager componentManager, final String methodName,
        final Class componentClass )
    {
        m_componentManager = componentManager;
        m_methodName = methodName;
        m_componentClass = componentClass;
        if ( m_methodName == null )
        {
            m_state = new NotApplicable();
        }
        else
        {
            m_state = new NotResolved();
        }
    }


    protected final AbstractComponentManager getComponentManager()
    {
        return m_componentManager;
    }


    protected final boolean isDS11()
    {
        return getComponentManager().getComponentMetadata().isDS11();
    }


    protected final String getMethodName()
    {
        return m_methodName;
    }


    protected final Class getComponentClass()
    {
        return m_componentClass;
    }


    /**
     * Finds the method named in the {@link #m_methodName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable method
     * the class hierarchy is traversed until a method is found or the root
     * of the class hierarchy is reached without finding a method.
     *
     * @param targetClass The class in which to look for the method
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method findMethod() throws InvocationTargetException
    {
        boolean acceptPrivate = isDS11();
        boolean acceptPackage = isDS11();

        final Class targetClass = getComponentClass();
        final ClassLoader targetClasslLoader = targetClass.getClassLoader();
        final String targetPackage = getPackageName( targetClass );

        for ( Class theClass = targetClass; theClass != null; )
        {

            try
            {
                Method method = doFindMethod( theClass, acceptPrivate, acceptPackage );
                if ( method != null )
                {
                    return method;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                // log and return null
                getComponentManager().log( LogService.LOG_ERROR,
                    "DependencyManager : Suitable but non-accessible method found in class " + targetClass.getName(),
                    null, null );
                break;
            }

            // if we get here, we have no method, so check the super class
            theClass = theClass.getSuperclass();
            if ( theClass == null )
            {
                break;
            }

            // super class method check ignores private methods and accepts
            // package methods only if in the same package and package
            // methods are (still) allowed
            acceptPackage &= targetClasslLoader == theClass.getClassLoader()
                && targetPackage.equals( getPackageName( theClass ) );

            // private methods will not be accepted any more in super classes
            acceptPrivate = false;
        }

        // nothing found after all these years ...
        return null;
    }


    protected abstract Method doFindMethod( final Class targetClass, final boolean acceptPrivate,
        final boolean acceptPackage ) throws SuitableMethodNotAccessibleException, InvocationTargetException;


    private boolean invokeMethod( final Object componentInstance, final Object rawParameter )
    {
        try
        {
            final Object[] params = getParameters( m_method, rawParameter );
            m_method.invoke( componentInstance, params );
        }
        catch ( IllegalStateException ise )
        {
            getComponentManager().log( LogService.LOG_INFO, ise.getMessage(), null, null );
            return false;
        }
        catch ( IllegalAccessException ex )
        {
            // 112.3.1 If the method is not is not declared protected or
            // public, SCR must log an error message with the log service,
            // if present, and ignore the method
            getComponentManager().log( LogService.LOG_DEBUG, "Method " + m_methodName + " cannot be called", null, ex );
        }
        catch ( InvocationTargetException ex )
        {
            // 112.5.7 If a bind method throws an exception, SCR must log an
            // error message containing the exception [...]
            getComponentManager().log( LogService.LOG_ERROR,
                "The " + getMethodName() + " method has thrown an exception", null, ex.getCause() );
            return false;
        }
        catch ( Throwable t )
        {
            // anything else went wrong, log the message and fail the invocation
            getComponentManager().log( LogService.LOG_ERROR, "The " + getMethodName() + " method could not be called",
                null, t );

            // method invocation threw, so it was a failure
            return false;
        }

        // assume success (also if the mehotd is not available or accessible)
        return true;
    }


    /**
     * Returns the parameter array created from the <code>rawParameter</code>
     * using the actual parameter type list of the <code>method</code>.
     * @param method
     * @param rawParameter
     * @return
     * @throws IllegalStateException If the required parameters cannot be
     *      extracted from the <code>rawParameter</code>
     */
    protected abstract Object[] getParameters( Method method, Object rawParameter );


    protected String getMethodNamePrefix()
    {
        return "";
    }


    //---------- Helpers

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
     * @throws SuitableMethodNotAccessibleException If method with the given
     *      name taking the parameters is found in the class but the method
     *      is not accessible.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to access the desired method.
     */
    public static Method getMethod( Class clazz, String name, Class[] parameterTypes, boolean acceptPrivate,
        boolean acceptPackage ) throws NoSuchMethodException, SuitableMethodNotAccessibleException,
        InvocationTargetException
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

        // suitable method found which is not accessible
        throw new SuitableMethodNotAccessibleException();
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
    protected static boolean accept( Method method, boolean acceptPrivate, boolean acceptPackage )
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
    public static String getPackageName( Class clazz )
    {
        String name = clazz.getName();
        int dot = name.lastIndexOf( '.' );
        return ( dot > 0 ) ? name.substring( 0, dot ) : "";
    }


    //---------- State management  ------------------------------------

    public boolean invoke( final Object componentInstance, final Object rawParameter )
    {
        return m_state.invoke( componentInstance, rawParameter );
    }

    private static interface State
    {

        boolean invoke( final Object componentInstance, final Object rawParameter );
    }

    private static class NotApplicable implements State
    {

        public boolean invoke( final Object componentInstance, final Object rawParameter )
        {
            return true;
        }
    }

    private class NotResolved implements State
    {

        public boolean invoke( final Object componentInstance, final Object rawParameter )
        {
            getComponentManager().log( LogService.LOG_DEBUG,
                "getting " + getMethodNamePrefix() + "bind: " + m_methodName, null, null );
            try
            {
                m_method = findMethod();
                m_state = ( m_method == null ) ? ( State ) new NotFound() : new Resolved();
                return m_state.invoke( componentInstance, rawParameter );
            }
            catch ( InvocationTargetException ex )
            {
                m_state = new NotFound();
                // We can safely ignore this one
                getComponentManager().log( LogService.LOG_WARNING, getMethodName() + " cannot be found", null,
                    ex.getTargetException() );
            }
            return true;
        }
    }

    private class NotFound implements State
    {

        public boolean invoke( final Object componentInstance, final Object rawParameter )
        {
            // 112.3.1 If the method is not found , SCR must log an error
            // message with the log service, if present, and ignore the
            // method
            getComponentManager().log( LogService.LOG_ERROR,
                getMethodNamePrefix() + "bind method [" + m_methodName + "] not found", null, null );
            return true;
        }
    }

    private class Resolved implements State
    {

        public boolean invoke( final Object componentInstance, final Object rawParameter )
        {
            getComponentManager().log( LogService.LOG_DEBUG,
                "invoking " + getMethodNamePrefix() + "bind: " + m_methodName, null, null );
            return invokeMethod( componentInstance, rawParameter );
        }
    }

}
