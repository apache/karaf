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

package org.apache.felix.sigil.ui.eclipse.ui.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;


@SuppressWarnings("unchecked")
public class SingletonSelection implements IStructuredSelection
{

    private final Object singleton;


    public SingletonSelection( Object singleton )
    {
        this.singleton = singleton;
    }


    public Object getFirstElement()
    {
        return singleton;
    }


    public Iterator iterator()
    {
        return Collections.singleton( singleton ).iterator();
    }


    public int size()
    {
        return 1;
    }


    public Object[] toArray()
    {
        return new Object[]
            { singleton };
    }


    public List toList()
    {
        ArrayList list = new ArrayList( 1 );
        list.add( singleton );
        return list;
    }


    public boolean isEmpty()
    {
        return false;
    }
}
