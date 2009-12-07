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


/**
 * Component method to be invoked on service property update of a bound service.
 */
public class UpdatedMethod extends BindMethod
{

    public UpdatedMethod( final AbstractComponentManager componentManager, final String methodName,
        final Class componentClass, final String referenceName, final String referenceClassName )
    {
        super( componentManager, methodName, componentClass, referenceName, referenceClassName );
    }


    protected String getMethodNamePrefix()
    {
        return "update";
    }

}