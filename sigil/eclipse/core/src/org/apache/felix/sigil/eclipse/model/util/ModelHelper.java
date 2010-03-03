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

package org.apache.felix.sigil.eclipse.model.util;


import java.util.LinkedList;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.model.ICapabilityModelElement;
import org.apache.felix.sigil.model.ICompoundModelElement;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.IRequirementModelElement;


public class ModelHelper
{
    public static List<IModelElement> findUsers( IModelElement e )
    {
        LinkedList<IModelElement> users = new LinkedList<IModelElement>();

        findUsers( e, users );

        return users;
    }


    private static void findUsers( IModelElement e, final LinkedList<IModelElement> users )
    {
        if ( e instanceof ICapabilityModelElement )
        {
            final ICapabilityModelElement cap = ( ICapabilityModelElement ) e;
            SigilCore.getGlobalRepositoryManager().visit( new IModelWalker()
            {
                public boolean visit( IModelElement element )
                {
                    if ( element instanceof IRequirementModelElement )
                    {
                        IRequirementModelElement req = ( IRequirementModelElement ) element;
                        if ( req.accepts( cap ) )
                        {
                            users.add( req );
                        }
                        return false;
                    }

                    return true;
                }
            } );
        }
        
        if ( e instanceof ICompoundModelElement )
        {
            ICompoundModelElement c = ( ICompoundModelElement ) e;
            IModelElement[] ch = c.children();
            for ( IModelElement ee : ch )
            {
                findUsers( ee, users );
            }
        }
    }

}
