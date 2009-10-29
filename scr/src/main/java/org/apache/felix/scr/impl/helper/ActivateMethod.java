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
import org.osgi.service.component.ComponentContext;


public class ActivateMethod extends BaseMethod
{
    public ActivateMethod( final AbstractComponentManager componentManager, final String methodName,
        final boolean methodRequired, final Class componentClass )
    {
        super( componentManager, methodName, methodRequired, componentClass );
    }


    protected Method doFindMethod( Class targetClass, boolean acceptPrivate, boolean acceptPackage )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {

        boolean suitableMethodNotAccessible = false;

        try
        {
            final Method method = getSingleParameterMethod( targetClass, acceptPrivate, acceptPackage );
            if ( method != null )
            {
                return method;
            }
        }
        catch ( SuitableMethodNotAccessibleException smnae )
        {
            suitableMethodNotAccessible = true;
        }

        if ( isDS11() )
        {
            // check methods with MethodTester
            Method[] methods = targetClass.getDeclaredMethods();
            for ( int i = 0; i < methods.length; i++ )
            {
                if ( methods[i].getName().equals( getMethodName() ) && isSuitable( methods[i] ) )
                {
                    if ( accept( methods[i], acceptPrivate, acceptPackage ) )
                    {
                        // check modifiers etc.
                        return methods[i];
                    }

                    // method is suitable but not accessible, flag it
                    suitableMethodNotAccessible = true;
                }
            }

            // finally check method with no arguments
            if ( acceptEmpty() )
            {
                try
                {
                    // find the declared method in this class
                    return getMethod( targetClass, getMethodName(), null, acceptPrivate, acceptPackage );
                }
                catch ( SuitableMethodNotAccessibleException smnae )
                {
                    suitableMethodNotAccessible = true;
                }
            }
        }

        if ( suitableMethodNotAccessible )
        {
            throw new SuitableMethodNotAccessibleException();
        }

        return null;
    }


    protected Object[] getParameters( Method method, Object rawParameter )
    {
        final Class[] parameterTypes = method.getParameterTypes();
        final ActivatorParameter ap = ( ActivatorParameter ) rawParameter;
        final Object[] param = new Object[parameterTypes.length];
        for ( int i = 0; i < param.length; i++ )
        {
            if ( parameterTypes[i] == COMPONENT_CONTEXT_CLASS )
            {
                param[i] = ap.getComponentContext();
            }
            else if ( parameterTypes[i] == BUNDLE_CONTEXT_CLASS )
            {
                param[i] = ap.getComponentContext().getBundleContext();
            }
            else if ( parameterTypes[i] == MAP_CLASS )
            {
                // note: getProperties() returns a ReadOnlyDictionary which is a Map
                param[i] = ap.getComponentContext().getProperties();
            }
            else if ( parameterTypes[i] == INTEGER_CLASS || parameterTypes[i] == Integer.TYPE )
            {
                param[i] = new Integer( ap.getReason() );
            }
        }

        return param;
    }


    protected String getMethodNamePrefix()
    {
        return "activate";
    }


    public boolean invoke( Object componentInstance, Object rawParameter )
    {
        return methodExists() && super.invoke( componentInstance, rawParameter );
    }


    /**
     * Returns a method taking a single parameter of one of the
     * {@link #getAcceptedParameterTypes()} or <code>null</code> if no such
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
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getSingleParameterMethod( final Class targetClass, final boolean acceptPrivate,
        final boolean acceptPackage ) throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        SuitableMethodNotAccessibleException ex = null;
        Method singleParameterMethod = null;
        final Class[] acceptedTypes = getAcceptedParameterTypes();
        for ( int i = 0; singleParameterMethod == null && i < acceptedTypes.length; i++ )
        {
            try
            {
                // find the declared method in this class
                singleParameterMethod = getMethod( targetClass, getMethodName(), new Class[]
                    { acceptedTypes[i] }, acceptPrivate, acceptPackage );
            }
            catch ( SuitableMethodNotAccessibleException thrown )
            {
                ex = thrown;
            }

        }

        // rethrow if we looked for all method signatures and only found
        // one or more which would be suitable but not accessible
        if ( ex != null )
        {
            throw ex;
        }

        // no method with a matching single parameter has been found
        return singleParameterMethod;
    }


    private boolean isSuitable( Method method )
    {
        // require two or more arguments
        final Class[] types = method.getParameterTypes();
        if ( types.length < 2 )
        {
            return false;
        }

        // check for argument types
        final Class[] acceptedTypes = getAcceptedParameterTypes();
        OUTER: for ( int i = 0; i < types.length; i++ )
        {
            final Class type = types[i];
            for ( int j = 0; j < acceptedTypes.length; j++ )
            {
                if ( type == acceptedTypes[j] )
                {
                    continue OUTER;
                }
            }

            // get here if type is not contained in the array
            return false;
        }

        // all parameters are acceptable
        return true;
    }


    protected Class[] getAcceptedParameterTypes()
    {
        if ( isDS11() )
        {
            return new Class[]
                { COMPONENT_CONTEXT_CLASS, BUNDLE_CONTEXT_CLASS, MAP_CLASS };
        }

        return new Class[]
            { COMPONENT_CONTEXT_CLASS };
    }


    protected boolean acceptEmpty()
    {
        return isDS11();
    }

    //---------- Helper class for method call parameters

    public static final class ActivatorParameter
    {
        private final ComponentContext m_componentContext;
        private final int m_reason;


        public ActivatorParameter( ComponentContext componentContext, int reason )
        {
            this.m_componentContext = componentContext;
            this.m_reason = reason;
        }


        public ComponentContext getComponentContext()
        {
            return m_componentContext;
        }


        public int getReason()
        {
            return m_reason;
        }
    }
}
