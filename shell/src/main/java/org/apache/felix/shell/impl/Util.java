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
package org.apache.felix.shell.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class Util
{
    public static String getBundleName(Bundle bundle)
    {
        if (bundle != null)
        {
            String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
            return (name == null)
                ? "Bundle " + Long.toString(bundle.getBundleId())
                : name + " (" + Long.toString(bundle.getBundleId()) + ")";
        }
        return "[STALE BUNDLE]";
    }

    private static StringBuffer m_sb = new StringBuffer();

    public static String getUnderlineString(String s)
    {
        synchronized (m_sb)
        {
            m_sb.delete(0, m_sb.length());
            for (int i = 0; i < s.length(); i++)
            {
                m_sb.append('-');
            }
            return m_sb.toString();
        }
    }

    public static String getValueString(Object obj)
    {
        synchronized (m_sb)
        {
            if (obj instanceof String)
            {
                return (String) obj;
            }
            else if (obj instanceof String[])
            {
                String[] array = (String[]) obj;
                m_sb.delete(0, m_sb.length());
                for (int i = 0; i < array.length; i++)
                {
                    if (i != 0)
                    {
                        m_sb.append(", ");
                    }
                    m_sb.append(array[i].toString());
                }
                return m_sb.toString();
            }
            else if (obj instanceof Boolean)
            {
                return ((Boolean) obj).toString();
            }
            else if (obj instanceof Long)
            {
                return ((Long) obj).toString();
            }
            else if (obj instanceof Integer)
            {
                return ((Integer) obj).toString();
            }
            else if (obj instanceof Short)
            {
                return ((Short) obj).toString();
            }
            else if (obj instanceof Double)
            {
                return ((Double) obj).toString();
            }
            else if (obj instanceof Float)
            {
                return ((Float) obj).toString();
            }

            return "<unknown value type>";
        }
    }
}