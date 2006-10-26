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
package org.apache.felix.framework.searchpolicy;

import org.osgi.framework.Version;

public class R4Package
{
    private String m_name = "";
    protected R4Directive[] m_directives = null;
    protected R4Attribute[] m_attrs = null;
    protected Version m_version = new Version("0.0.0");

    public R4Package(String name, R4Directive[] directives, R4Attribute[] attrs)
    {
        m_name = name;
        m_directives = (directives == null)
            ? new R4Directive[0] : (R4Directive[]) directives.clone();
        m_attrs = (attrs == null)
            ? new R4Attribute[0] : (R4Attribute[]) attrs.clone();
    }

    public String getName()
    {
        return m_name;
    }

    public R4Directive[] getDirectives()
    {
        return m_directives;
    }

    public R4Attribute[] getAttributes()
    {
        return m_attrs;
    }

    public Version getVersion()
    {
        return m_version;
    }


    public String toString()
    {
        String msg = getName();
        for (int i = 0; (m_directives != null) && (i < m_directives.length); i++)
        {
            msg = msg + " [" + m_directives[i].getName() + ":="+ m_directives[i].getValue() + "]";
        }
        for (int i = 0; (m_attrs != null) && (i < m_attrs.length); i++)
        {
            msg = msg + " [" + m_attrs[i].getName() + "="+ m_attrs[i].getValue() + "]";
        }
        return msg;
    }
}