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
package org.apache.felix.scrplugin;


import org.apache.felix.scrplugin.tags.JavaTag;


public class SCRDescriptorException extends Exception
{

    private static final long serialVersionUID = 1L;

    private final String m_sourceLocation;

    private final int m_lineNumber;


    public SCRDescriptorException( final String message, final JavaTag tag )
    {
        this( message, tag.getSourceLocation(), tag.getLineNumber() );
    }


    public SCRDescriptorException( final String message, final String sourceLocation, final int lineNumber )
    {
        super( message );
        this.m_sourceLocation = sourceLocation;
        this.m_lineNumber = lineNumber;
    }


    public SCRDescriptorException( final String message, final JavaTag tag, final Throwable cause )
    {
        this( message, tag.getSourceLocation(), tag.getLineNumber(), cause );
    }


    public SCRDescriptorException( final String message, final String sourceLocation, final int lineNumber,
        final Throwable cause )
    {
        super( message, cause );
        this.m_sourceLocation = sourceLocation;
        this.m_lineNumber = lineNumber;
    }


    public String getSourceLocation()
    {
        return m_sourceLocation;
    }


    public int getLineNumber()
    {
        return m_lineNumber;
    }
}
