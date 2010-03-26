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
package org.apache.felix.utils.manifest;

public class Clause
{

    private final String name;
    private final Directive[] directives;
    private final Attribute[] attributes;

    public Clause(String name, Directive[] directives, Attribute[] attributes)
    {
        this.name = name;
        this.directives = directives;
        this.attributes = attributes;
    }

    public String getName()
    {
        return name;
    }

    public Directive[] getDirectives()
    {
        return directives;
    }

    public Attribute[] getAttributes()
    {
        return attributes;
    }

    public String getDirective(String name)
    {
        for (int i = 0; i < directives.length; i++)
        {
            if (name.equals(directives[i].getName()))
            {
                return directives[i].getValue();
            }
        }
        return null;
    }

    public String getAttribute(String name)
    {
        for (int i = 0; i < attributes.length; i++)
        {
            if (name.equals(attributes[i].getName()))
            {
                return attributes[i].getValue();
            }
        }
        return null;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        for (int i = 0; directives != null && i < directives.length; i++)
        {
            sb.append(";").append(directives[i].getName()).append(":=");
            if (directives[i].getValue().contains(","))
            {
                sb.append("\"").append(directives[i].getValue()).append("\"");
            }
            else
            {
                sb.append(directives[i].getValue());
            }
        }
        for (int i = 0; attributes != null && i < attributes.length; i++)
        {
            sb.append(";").append(attributes[i].getName()).append("=");
            if (attributes[i].getValue().contains(","))
            {
                sb.append("\"").append(attributes[i].getValue()).append("\"");
            }
            else
            {
                sb.append(attributes[i].getValue());
            }
        }
        return sb.toString();
    }
}
