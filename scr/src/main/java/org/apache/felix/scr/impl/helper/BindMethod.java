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

import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


/**
 * Component method to be invoked on service (un)binding.
 */
public class BindMethod extends BaseMethod
{

    private final String m_referenceName;
    private final String m_referenceClassName;


    public BindMethod( final AbstractComponentManager componentManager, final String methodName,
        final Class componentClass, final String referenceName, final String referenceClassName )
    {
        super( componentManager, methodName, componentClass );
        m_referenceName = referenceName;
        m_referenceClassName = referenceClassName;
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
    protected Method doFindMethod( Class targetClass, boolean acceptPrivate, boolean acceptPackage )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
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
            if ( isDS11() )
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
        if ( suitableMethodNotAccessible )
        {
            getComponentManager().log( LogService.LOG_ERROR,
                "DependencyManager : Suitable but non-accessible method found in class {0}", new Object[]
                    { targetClass.getName() }, null );
            throw new SuitableMethodNotAccessibleException();
        }

        // no method found
        return null;
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
        return getMethod( targetClass, getMethodName(), new Class[]
            { SERVICE_REFERENCE_CLASS }, acceptPrivate, acceptPackage );
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
        return getMethod( targetClass, getMethodName(), new Class[]
            { parameterClass }, acceptPrivate, acceptPackage );
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
            if ( parameters.length == 1 && method.getName().equals( getMethodName() ) )
            {

                // Get the parameter type
                final Class theParameter = parameters[0];

                // Check if the parameter type is ServiceReference
                // or is assignable from the type specified by the
                // reference's interface attribute
                if ( theParameter.isAssignableFrom( parameterClass ) )
                {
                    if ( accept( method, acceptPrivate, acceptPackage ) )
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
        return getMethod( targetClass, getMethodName(), new Class[]
            { parameterClass, MAP_CLASS }, acceptPrivate, acceptPackage );
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
            if ( parameters.length == 2 && method.getName().equals( getMethodName() ) )
            {

                // parameters must be refclass,map
                if ( parameters[0].isAssignableFrom( parameterClass ) && parameters[1] == MAP_CLASS )
                {
                    if ( accept( method, acceptPrivate, acceptPackage ) )
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


    protected Object[] getParameters( Method method, Object rawParameter )
    {
        final Service service = ( Service ) rawParameter;
        final Class[] paramTypes = method.getParameterTypes();
        final Object[] params = new Object[paramTypes.length];
        for ( int i = 0; i < params.length; i++ )
        {
            if ( paramTypes[i] == SERVICE_REFERENCE_CLASS )
            {
                params[i] = service.getReference();
            }
            else if ( paramTypes[i] == MAP_CLASS )
            {
                params[i] = new ReadOnlyDictionary( service.getReference() );
            }
            else
            {
                params[i] = service.getInstance();
                if ( params[i] == null )
                {
                    throw new IllegalStateException( "Dependency Manager: Service " + service.getReference()
                        + " has already gone, will not " + getMethodNamePrefix() );
                }
            }
        }

        return params;
    }


    protected String getMethodNamePrefix()
    {
        return "bind";
    }

    //---------- Service abstraction ------------------------------------

    public static interface Service
    {

        ServiceReference getReference();


        Object getInstance();

    }

}
