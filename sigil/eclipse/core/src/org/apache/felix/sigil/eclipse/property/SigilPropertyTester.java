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

package org.apache.felix.sigil.eclipse.property;


import org.apache.felix.sigil.eclipse.SigilCore;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;


public class SigilPropertyTester extends PropertyTester
{

    public SigilPropertyTester()
    {
    }


    public boolean test( Object receiver, String property, Object[] args, Object expectedValue )
    {
        IResource resource = ( IResource ) receiver;
        if ( "isSigilProject".equals( property ) )
        {
            return expectedValue.equals( isSigilProjectLikeResource( resource ) );
        }
        return false;
    }


    /**
     * @param resource
     * @return
     */
    private static boolean isSigilProjectLikeResource( IResource resource )
    {
        if ( resource instanceof IProject )
        {
            IProject p = ( IProject ) resource;
            return SigilCore.isSigilProject( p );
        }
        else
        {
            return resource.getName().equals( SigilCore.SIGIL_PROJECT_FILE );
        }
    }

}
