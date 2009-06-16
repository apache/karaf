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
package org.apache.felix.framework.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SmallSet implements Set
{
    private Object[] m_objs;
    private Set m_set;

    public synchronized int size()
    {
        if (m_objs != null)
        {
            return m_objs.length;
        }
        else if (m_set != null)
        {
            return m_set.size();
        }
        return 0;
    }

    public synchronized boolean isEmpty()
    {
        return (size() == 0);
    }

    public synchronized boolean contains(Object obj)
    {
        boolean found = false;
        if (obj != null)
        {
            if (m_objs != null)
            {
                for (int i = 0; !found && (i < m_objs.length); i++)
                {
                    found = m_objs[i].equals(obj);
                }
            }
            else
            {
                found = (m_set == null) ? false : m_set.contains(obj);
            }
        }
        return found;
    }

    public synchronized Iterator iterator()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized Object[] toArray()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized Object[] toArray(Object[] arg0)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized boolean add(Object obj)
    {
        boolean found = contains(obj);
        if (!found)
        {
            if (size() < 10)
            {
                if (m_objs == null)
                {
                    m_objs = new Object[] { obj };
                }
                else
                {
                    Object[] tobjs = new Object[m_objs.length + 1];
                    System.arraycopy(m_objs, 0, tobjs, 0, m_objs.length);
                    tobjs[m_objs.length] = obj;
                    m_objs = tobjs;
                }
            }
            else
            {
                if (m_set == null)
                {
                    m_set = new HashSet();
                    for (int i = 0; i < m_objs.length; i++)
                    {
                        m_set.add(m_objs[i]);
                    }
                    m_objs = null;
                }
                m_set.add(obj);
            }
        }
        return !found;
    }

    public synchronized boolean remove(Object obj)
    {
        boolean found = contains(obj);
        if (found)
        {
            if (size() < 10)
            {
                if (m_objs.length == 1)
                {
                    m_objs = null;
                }
                else
                {
                    int idx = -1;
                    for (int i = 0; (idx < 0) && (i < m_objs.length); i++)
                    {
                        if (m_objs[i].equals(obj))
                        {
                            idx = i;
                        }
                    }

                    if (idx >= 0)
                    {
                        Object[] tobjs = new Object[m_objs.length - 1];
                        System.arraycopy(m_objs, 0, tobjs, 0, idx);
                        if (idx < tobjs.length)
                        {
                            System.arraycopy(
                                m_objs, idx + 1, tobjs, idx, tobjs.length - idx);
                        }
                        m_objs = tobjs;
                    }
                }
            }
            else
            {
                m_set.remove(obj);
                if (m_set.size() < 10)
                {
                    m_objs = new Object[m_set.size()];
                    int i = 0;
                    for (Iterator it = m_set.iterator(); it.hasNext(); )
                    {
                        m_objs[i++] = it.next();
                    }
                    m_set = null;
                }
            }
        }
        return found;
    }

    public synchronized boolean containsAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized boolean addAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized boolean retainAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized boolean removeAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized void clear()
    {
        m_objs = null;
        m_set = null;
    }
}