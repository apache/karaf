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
package org.apache.felix.scr.impl.manager;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.scr.impl.helper.ReflectionHelper;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


/**
 * Component method to be invoked on service (un)binding.
 */
class BindMethod
{

    private final String m_methodName;
    private final Class m_componentClass;
    private final String m_referenceName;
    private final String m_referenceClassName;
    private final Logger m_logger;

    private Method m_method = null;
    private State m_state;


    BindMethod( final String methodName, final Class componentClass, final String referenceName,
        final String referenceClassName, final Logger logger )
    {
        m_methodName = methodName;
        m_componentClass = componentClass;
        m_referenceName = referenceName;
        m_referenceClassName = referenceClassName;
        m_logger = logger;
        if ( m_methodName == null )
        {
            m_state = new NotApplicable();
        }
        else
        {
            m_state = new NotResolved();
        }
    }


    private Method findMethod( final Class targetClass ) throws InvocationTargetException
    {
        Class parameterClass = null;

        // 112.3.1 The method is searched for using the following priority
        // 1. The method's parameter type is org.osgi.framework.ServiceReference
        // 2. The method's parameter type is the type specified by the
        // reference's interface attribute
        // 3. The method's parameter type is assignable from the type specified
        // by the reference's interface attribute
        try
        {
            // Case 1 - ServiceReference parameter
            return ReflectionHelper.getMethod( targetClass, m_methodName, new Class[]
                { ReflectionHelper.SERVICE_REFERENCE_CLASS }, false, // do not accept private methods
                false // do not accept package methods
                );
        }
        catch ( NoSuchMethodException ex )
        {

            try
            {
                // Case2 - Service object parameter

                // need the class loader of the target class, which may be the
                // system classloader, which case getClassLoader may retur null
                ClassLoader loader = targetClass.getClassLoader();
                if ( loader == null )
                {
                    loader = ClassLoader.getSystemClassLoader();
                }

                parameterClass = loader.loadClass( m_referenceClassName );
                return ReflectionHelper.getMethod( targetClass, m_methodName, new Class[]
                    { parameterClass }, false, false );
            }
            catch ( NoSuchMethodException ex2 )
            {

                // Case 3 - Service interface assignement compatible methods

                // Get all potential bind methods
                Method candidateBindMethods[] = targetClass.getDeclaredMethods();

                // Iterate over them
                for ( int i = 0; i < candidateBindMethods.length; i++ )
                {
                    Method method = candidateBindMethods[i];

                    // Get the parameters for the current method
                    Class[] parameters = method.getParameterTypes();

                    // Select only the methods that receive a single
                    // parameter
                    // and a matching name
                    if ( parameters.length == 1 && method.getName().equals( m_methodName ) )
                    {

                        // Get the parameter type
                        Class theParameter = parameters[0];

                        // Check if the parameter type is ServiceReference
                        // or is assignable from the type specified by the
                        // reference's interface attribute
                        if ( theParameter.isAssignableFrom( parameterClass ) )
                        {

                            // Final check: it must be public or protected
                            if ( Modifier.isPublic( method.getModifiers() )
                                || Modifier.isProtected( method.getModifiers() ) )
                            {
                                if ( !method.isAccessible() )
                                {
                                    method.setAccessible( true );
                                }
                                return method;
                            }
                        }
                    }
                }

                // Case 4: same as case 2, but + Map param
                try
                {
                    // need the class loader of the target class, which may be the
                    // system classloader, which case getClassLoader may retur null
                    ClassLoader loader = targetClass.getClassLoader();
                    if ( loader == null )
                    {
                        loader = ClassLoader.getSystemClassLoader();
                    }

                    parameterClass = loader.loadClass( m_referenceClassName );
                    return ReflectionHelper.getMethod( targetClass, m_methodName, new Class[]
                        { parameterClass, Map.class }, false, false );
                }
                catch ( NoSuchMethodException ex3 )
                {

                }
                catch ( ClassNotFoundException ex3 )
                {
                    // if we can't load the class, perhaps the method is declared in a super class
                    // so we try this class next
                }
            }
            catch ( ClassNotFoundException ex2 )
            {
                // if we can't load the class, perhaps the method is declared in a super class
                // so we try this class next
            }

            // TODO: Case 5: same as case 3, but + Map param
        }

        // if we get here, we have no method, so check the super class
        Class superClass = targetClass.getSuperclass();
        return ( superClass != null ) ? findMethod( superClass ) : null;
    }


    private boolean invokeMethod( final Object componentInstance, final Service service )
    {
        final Class[] paramTypes = m_method.getParameterTypes();
        final Object[] params = new Object[paramTypes.length];
        Map properties = null;
        for ( int i = 0; i < params.length; i++ )
        {
            if ( paramTypes[i] == ReflectionHelper.SERVICE_REFERENCE_CLASS )
            {
                params[i] = service.getReference();
            }
            else if ( paramTypes[i] == ReflectionHelper.MAP_CLASS )
            {
                if ( properties == null )
                {
                    final ServiceReference serviceReference = service.getReference();
                    properties = new HashMap();
                    final String[] keys = serviceReference.getPropertyKeys();
                    if ( keys != null )
                    {
                        for ( int j = 0; j < keys.length; j++ )
                        {
                            final String key = keys[j];
                            properties.put( key, serviceReference.getProperty( key ) );
                        }
                    }
                }
                params[i] = properties;
            }
            else
            {
                params[i] = service.getInstance();
                if ( params[i] == null )
                {
                    m_logger.log( LogService.LOG_INFO, "Dependency Manager: Service " + service.getReference()
                        + " has already gone, not " + getMethodNamePrefix() + "binding" );
                    return false;
                }
            }
        }
        try
        {
            m_method.invoke( componentInstance, params );
            m_logger.log( LogService.LOG_DEBUG, getMethodNamePrefix() + "bound: " + m_referenceName + "/"
                + service.getReference().getProperty( Constants.SERVICE_ID ) );
        }
        catch ( IllegalAccessException ex )
        {
            // 112.3.1 If the method is not is not declared protected or
            // public, SCR must log an error message with the log service,
            // if present, and ignore the method
            m_logger.log( LogService.LOG_ERROR, getMethodNamePrefix() + "bind method " + m_methodName + "] cannot be called",
                ex );
        }
        catch ( InvocationTargetException ex )
        {
            // 112.5.7 If a bind method throws an exception, SCR must log an
            // error message containing the exception [...]
            m_logger.log( LogService.LOG_ERROR, "DependencyManager : exception while invoking " + m_methodName + "()",
                ex.getCause() );
        }
        return true;
    }


    protected String getMethodNamePrefix()
    {
        return "";
    }


    //---------- State management  ------------------------------------

    boolean invoke( final Object componentInstance, final Service service )
    {
        return m_state.invoke( componentInstance, service );
    }

    private static interface State
    {

        boolean invoke( final Object componentInstance, final Service service );
    }

    private static class NotApplicable implements State
    {

        public boolean invoke( final Object componentInstance, final Service service )
        {
            return true;
        }
    }

    private class NotResolved implements State
    {

        public boolean invoke( final Object componentInstance, final Service service )
        {
            m_logger.log( LogService.LOG_DEBUG, "getting " + getMethodNamePrefix() + "bind: " + m_methodName );
            try
            {
                m_method = findMethod( m_componentClass );
                if ( m_method == null )
                {
                    m_state = new NotFound();
                }
                else
                {
                    m_state = new Resolved();
                }
                return m_state.invoke( componentInstance, service );
            }
            catch ( InvocationTargetException ex )
            {
                m_state = new NotFound();
                // 112.5.7 If a bind method throws an exception, SCR must log an
                // error message containing the exception [...]
                m_logger.log( LogService.LOG_ERROR, "DependencyManager : exception while finding " + m_methodName
                    + "()", ex.getCause() );
            }
            return true;
        }
    }

    private class NotFound implements State
    {

        public boolean invoke( final Object componentInstance, final Service service )
        {
            // 112.3.1 If the method is not found , SCR must log an error
            // message with the log service, if present, and ignore the
            // method
            m_logger.log( LogService.LOG_ERROR, getMethodNamePrefix() + "bind method [" + m_methodName + "] not found" );
            return true;
        }
    }

    private class Resolved implements State
    {

        public boolean invoke( final Object componentInstance, final Service service )
        {
            m_logger.log( LogService.LOG_DEBUG, "invoking " + getMethodNamePrefix() + "bind: " + m_methodName );
            return invokeMethod( componentInstance, service );
        }
    }

    //---------- Service abstraction ------------------------------------

    static interface Service
    {

        ServiceReference getReference();


        Object getInstance();

    }

    //---------- Logger ------------------------------------

    static interface Logger
    {

        void log( int level, String message );


        void log( int level, String message, Throwable ex );

    }

}
