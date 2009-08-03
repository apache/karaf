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
import org.apache.felix.scr.impl.helper.ReadOnlyDictionary;
import org.apache.felix.scr.impl.helper.ReflectionHelper;
import org.apache.felix.scr.impl.helper.SuitableMethodNotAccessibleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


/**
 * Component method to be invoked on service (un)binding.
 */
class BindMethod
{

    private final boolean m_isDS11;
    private final String m_methodName;
    private final Class m_componentClass;
    private final String m_referenceName;
    private final String m_referenceClassName;
    private final Logger m_logger;

    private Method m_method = null;
    private State m_state;


    BindMethod( final boolean isDS11, final String methodName, final Class componentClass, final String referenceName,
        final String referenceClassName, final Logger logger )
    {
        m_isDS11 = isDS11;
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
    private Method findMethod( final Class targetClass, final boolean acceptPrivate, final boolean acceptPackage )
        throws InvocationTargetException
    {
        // 112.3.1 The method is searched for using the following priority
        // 1 - Service reference parameter
        // 2 - Service object parameter
        // 3 - Service interface assignement compatible methods
        // 4 - same as 2, but with Map param (DS 1.1 only)
        // 5 - same as 3, but with Map param (DS 1.1 only)

        // flag indicating a suitable but inaccessible method has been found
        boolean suitableMethodNotAccessible = false;

        // Case 1 - Service reference parameter
        Method method;
        try
        {
            method = getServiceReferenceMethod( targetClass, acceptPrivate, acceptPackage );
            if ( method != null )
            {
                return method;
            }
        }
        catch ( SuitableMethodNotAccessibleException ex )
        {
            suitableMethodNotAccessible = true;
        }

        // for further methods we need the class of the service object
        final Class parameterClass = getParameterClass( targetClass );
        if ( parameterClass != null )
        {

            // Case 2 - Service object parameter
            try
            {
                method = getServiceObjectMethod( targetClass, parameterClass, acceptPrivate, acceptPackage );
                if ( method != null )
                {
                    return method;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // Case 3 - Service interface assignement compatible methods
            try
            {
                method = getServiceObjectAssignableMethod( targetClass, parameterClass, acceptPrivate, acceptPackage );
                if ( method != null )
                {
                    return method;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // signatures taking a map are only supported starting with DS 1.1
            if ( m_isDS11 )
            {

                // Case 4 - same as case 2, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectWithMapMethod( targetClass, parameterClass, acceptPrivate, acceptPackage );
                    if ( method != null )
                    {
                        return method;
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

                // Case 5 - same as case 3, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectAssignableWithMapMethod( targetClass, parameterClass, acceptPrivate,
                        acceptPackage );
                    if ( method != null )
                    {
                        return method;
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

            }

        }

        // if at least one suitable method could be found but none of
        // the suitable methods are accessible, we have to terminate
        if (suitableMethodNotAccessible )
        {
            m_logger.log( LogService.LOG_ERROR,
                "DependencyManager : Suitable but non-accessible method found in class " + targetClass.getName() );
            return null;
        }

        // if we get here, we have no method, so check the super class
        final Class superClass = targetClass.getSuperclass();
        if (superClass == null) {
            return null;
        }

        // super class method check ignores private methods and accepts
        // package methods only if in the same package and package
        // methods are (still) allowed
        final boolean withPackage = acceptPackage && targetClass.getClassLoader() == superClass.getClassLoader()
            && ReflectionHelper.getPackageName( targetClass ).equals( ReflectionHelper.getPackageName( superClass ) );
        return findMethod( superClass, false, withPackage);
    }


    /**
     * Returns the class object representing the class of the service reference
     * named by the {@link #m_referenceClassName} field. The class loader of
     * the <code>targetClass</code> is used to load the service class.
     * <p>
     * It may well be possible, that the classloader of the target class cannot
     * see the service object class, for example if the service reference is
     * inherited from a component class of another bundle.
     *
     * @return The class object for the referred to service or <code>null</code>
     *      if the class loader of the <code>targetClass</code> cannot see that
     *      class.
     */
    private Class getParameterClass( final Class targetClass )
    {
        try
        {
            // need the class loader of the target class, which may be the
            // system classloader, which case getClassLoader may retur null
            ClassLoader loader = targetClass.getClassLoader();
            if ( loader == null )
            {
                loader = ClassLoader.getSystemClassLoader();
            }

            return loader.loadClass( m_referenceClassName );

        }
        catch ( ClassNotFoundException cnfe )
        {
            // if we can't load the class, perhaps the method is declared in a
            // super class so we try this class next
        }

        return null;
    }


    /**
     * Returns a method taking a single <code>ServiceReference</code> object
     * as a parameter or <code>null</code> if no such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceReferenceMethod( final Class targetClass, boolean acceptPrivate, boolean acceptPackage )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        try
        {
            return ReflectionHelper.getMethod( targetClass, m_methodName, new Class[]
                { ReflectionHelper.SERVICE_REFERENCE_CLASS }, acceptPrivate, acceptPackage );
        }
        catch ( NoSuchMethodException e )
        {
            // the named method could not be found
        }

        // no method taking service reference
        return null;
    }


    /**
     * Returns a method taking a single parameter of the exact type declared
     * for the service reference or <code>null</code> if no such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectMethod( final Class targetClass, final Class parameterClass, boolean acceptPrivate,
        boolean acceptPackage ) throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        try
        {
            return ReflectionHelper.getMethod( targetClass, m_methodName, new Class[]
                { parameterClass }, acceptPrivate, acceptPackage );
        }
        catch ( NoSuchMethodException nsme )
        {
            // no method taking service object
        }

        // no method taking service object
        return null;
    }


    /**
     * Returns a method taking a single object whose type is assignment
     * compatible with the declared service type or <code>null</code> if no
     * such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     */
    private Method getServiceObjectAssignableMethod( final Class targetClass, final Class parameterClass,
        boolean acceptPrivate, boolean acceptPackage ) throws SuitableMethodNotAccessibleException
    {
        // Get all potential bind methods
        Method candidateBindMethods[] = targetClass.getDeclaredMethods();
        boolean suitableNotAccessible = false;

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
                final Class theParameter = parameters[0];

                // Check if the parameter type is ServiceReference
                // or is assignable from the type specified by the
                // reference's interface attribute
                if ( theParameter.isAssignableFrom( parameterClass ) )
                {
                    if ( ReflectionHelper.accept( method, acceptPrivate, acceptPackage ) )
                    {
                        return method;
                    }

                    // suitable method is not accessible, flag for exception
                    suitableNotAccessible = true;
                }
            }
        }

        // if one or more suitable methods which are not accessible is/are
        // found an exception is thrown
        if ( suitableNotAccessible )
        {
            throw new SuitableMethodNotAccessibleException();
        }

        // no method with assignment compatible argument found
        return null;
    }


    /**
     * Returns a method taking two parameters, the first being of the exact
     * type declared for the service reference and the second being a
     * <code>Map</code> or <code>null</code> if no such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectWithMapMethod( final Class targetClass, final Class parameterClass,
        boolean acceptPrivate, boolean acceptPackage ) throws SuitableMethodNotAccessibleException,
        InvocationTargetException
    {
        try
        {
            return ReflectionHelper.getMethod( targetClass, m_methodName, new Class[]
                { parameterClass, ReflectionHelper.MAP_CLASS }, acceptPrivate, acceptPackage );
        }
        catch ( NoSuchMethodException nsme )
        {
            // no method taking service object
        }

        // no method taking service object
        return null;
    }


    /**
     * Returns a method taking two parameters, the first being an object
     * whose type is assignment compatible with the declared service type and
     * the second being a <code>Map</code> or <code>null</code> if no such
     * method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     */
    private Method getServiceObjectAssignableWithMapMethod( final Class targetClass, final Class parameterClass,
        boolean acceptPrivate, boolean acceptPackage ) throws SuitableMethodNotAccessibleException
    {
        // Get all potential bind methods
        Method candidateBindMethods[] = targetClass.getDeclaredMethods();
        boolean suitableNotAccessible = false;

        // Iterate over them
        for ( int i = 0; i < candidateBindMethods.length; i++ )
        {
            final Method method = candidateBindMethods[i];
            final Class[] parameters = method.getParameterTypes();
            if ( parameters.length == 2 && method.getName().equals( m_methodName ) )
            {

                // parameters must be refclass,map
                if ( parameters[0].isAssignableFrom( parameterClass ) && parameters[1] == ReflectionHelper.MAP_CLASS )
                {
                    if ( ReflectionHelper.accept( method, acceptPrivate, acceptPackage ) )
                    {
                        return method;
                    }

                    // suitable method is not accessible, flag for exception
                    suitableNotAccessible = true;
                }
            }
        }

        // if one or more suitable methods which are not accessible is/are
        // found an exception is thrown
        if ( suitableNotAccessible )
        {
            throw new SuitableMethodNotAccessibleException();
        }

        // no method with assignment compatible argument found
        return null;
    }


    private boolean invokeMethod( final Object componentInstance, final Service service )
    {
        final Class[] paramTypes = m_method.getParameterTypes();
        final Object[] params = new Object[paramTypes.length];
        for ( int i = 0; i < params.length; i++ )
        {
            if ( paramTypes[i] == ReflectionHelper.SERVICE_REFERENCE_CLASS )
            {
                params[i] = service.getReference();
            }
            else if ( paramTypes[i] == ReflectionHelper.MAP_CLASS )
            {
                params[i] = new ReadOnlyDictionary( service.getReference() );
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
                // if the owning component is declared with the DS 1.1 namespace
                // (or newer), private and package private methods are accepted
                m_method = findMethod( m_componentClass, m_isDS11, m_isDS11 );
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
