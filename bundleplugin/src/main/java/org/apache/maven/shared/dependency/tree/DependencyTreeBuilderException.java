package org.apache.maven.shared.dependency.tree;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Indicates that a Maven project's dependency tree cannot be resolved.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyTreeBuilderException.java 661727 2008-05-30 14:21:49Z bentmann $
 */
public class DependencyTreeBuilderException extends Exception
{
    // constants --------------------------------------------------------------

    /**
     * The serialisation unique ID.
     */
    private static final long serialVersionUID = -3525803081807951764L;

    // constructors -----------------------------------------------------------

    public DependencyTreeBuilderException( String message )
    {
        super( message );
    }

    public DependencyTreeBuilderException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
