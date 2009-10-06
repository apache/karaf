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


import org.apache.felix.scr.impl.manager.AbstractComponentManager;


public class DeactivateMethod extends ActivateMethod
{

    public DeactivateMethod( final AbstractComponentManager componentManager, final String methodName,
        final boolean methodRequired, final Class componentClass )
    {
        super( componentManager, methodName, methodRequired, componentClass );
    }


    protected Class[] getAcceptedParameterTypes()
    {
        if ( isDS11() )
        {
            return new Class[]
                { COMPONENT_CONTEXT_CLASS, BUNDLE_CONTEXT_CLASS, MAP_CLASS, Integer.TYPE, INTEGER_CLASS };
        }

        return new Class[]
            { COMPONENT_CONTEXT_CLASS };
    }


    protected String getMethodNamePrefix()
    {
        return "deactivate";
    }
}
