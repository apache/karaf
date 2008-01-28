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

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class CompoundEnumeration implements Enumeration
{
    private Enumeration[] m_enums = null;
    private int index = 0;

    public CompoundEnumeration(Enumeration[] enums)
    {
        m_enums = enums;
    }

    public boolean hasMoreElements()
    {
        // if the current enum is null that means this enum is finished
        if (currentEnumeration() == null)
        {
            // No next enum
            return false;
        }
        // If the current enum has more elements, lets go
        return currentEnumeration().hasMoreElements();
    }

    private Enumeration findNextEnumeration(boolean moveCursor)
    {
        return findNextEnumeration(index, moveCursor);
    }

    private Enumeration findNextEnumeration(int cursor, boolean moveCursor)
    {
        // next place in the array
        int next = cursor + 1;
        // If the cursor is still in the array
        if (next < m_enums.length)
        {

            // If there is something in that place
            // AND the enum is not empty
            if (m_enums[next] != null &&
                m_enums[next].hasMoreElements())
            {
                // OK
                if (moveCursor)
                {
                    index = next;
                }
                return m_enums[next];
            }
            // Try next element
            return findNextEnumeration(next, moveCursor);
        }
        // No more elements available
        return null;
    }

    public Object nextElement()
    {
        // ask for the next element of the current enum.
        if (currentEnumeration() != null)
        {
            return currentEnumeration().nextElement();
        }

        // no more elements in this Enum
        // We must throw a NoSuchElementException
        throw new NoSuchElementException("No more elements");
    }

    private Enumeration currentEnumeration()
    {
        if (m_enums != null)
        {
            if (index < m_enums.length)
            {
                Enumeration e = m_enums[index];
                if (e == null || !e.hasMoreElements())
                {
                    // the current enum is null or empty
                    // we probably want to switch to the next one
                    e = findNextEnumeration(true);
                }
                return e;
            }
        }
        return null;
    }
}