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
package org.apache.felix.bundlerepository.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class XmlWriter
{
    private final Writer w;
    private final List elements = new ArrayList();
    private boolean empty;
    private boolean endAttr = true;
    private boolean indent;

    public XmlWriter(Writer w)
    {
        this(w, true);
    }

    public XmlWriter(Writer w, boolean indent)
    {
        this.w = w;
        this.indent = indent;
    }

    public XmlWriter indent(int nb) throws IOException
    {
        if (indent)
        {
            while (nb-- > 0)
            {
                w.append("  ");
            }
        }
        return this;
    }

    public XmlWriter newLine() throws IOException
    {
        if (indent)
        {
            w.append("\n");
        }
        return this;
    }

    public XmlWriter element(String name) throws IOException
    {
        if (!endAttr)
        {
            endAttr = true;
            w.append(">");
        }
        if (!elements.isEmpty())
        {
            newLine();
            indent(elements.size());
        }
        w.append("<").append(name);
        elements.add(name);
        empty = true;
        endAttr = false;
        return this;
    }

    public XmlWriter attribute(String name, Object value) throws IOException
    {
        if (value != null)
        {
            w.append(" ").append(name).append("='").append(encode(value.toString())).append("'");
        }
        return this;
    }

    public XmlWriter end() throws IOException
    {
        return end(true);
    }

    public XmlWriter end(boolean indent) throws IOException
    {
        String name = (String) elements.remove(elements.size() - 1);
        if (!endAttr)
        {
            endAttr = true;
            w.append("/>");
        }
        else
        {
            if (indent && !empty)
            {
                newLine();
                indent(elements.size());
            }
            w.append("</").append(name).append(">");
        }
        empty = false;
        return this;
    }

    public XmlWriter text(Object value) throws IOException
    {
        if (!endAttr)
        {
            endAttr = true;
            w.append(">");
        }
        w.append(encode(value.toString()));
        return this;
    }

    public XmlWriter textElement(String name, Object value) throws IOException
    {
        if (value != null)
        {
            element(name).text(value).end(false);
        }
        return this;
    }

    private static String encode(Object o) {
        String s = o != null ? o.toString() : "";
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("'", "&apos;");
    }

}
