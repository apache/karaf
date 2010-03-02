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

package org.apache.felix.sigil.repository;


public class ResolutionConfig
{
    private int options;

    public static final int INCLUDE_DEPENDENTS = 1;
    public static final int INCLUDE_OPTIONAL = 2;
    public static final int IGNORE_ERRORS = 4;
    /** Return only bundles that are indexed locally */
    public static final int INDEXED_ONLY = 8;
    /** Return only bundles that are stored or cached locally */
    public static final int LOCAL_ONLY = 16;
    public static final int COMPILE_TIME = 32;


    public ResolutionConfig()
    {
        this( INCLUDE_DEPENDENTS );
    }


    public ResolutionConfig( int options )
    {
        this.options = options;
    }


    public int getOptions()
    {
        return options;
    }


    public boolean isDependents()
    {
        return ( options & INCLUDE_DEPENDENTS ) != 0;
    }


    public boolean isIgnoreErrors()
    {
        return ( options & IGNORE_ERRORS ) != 0;
    }


    public boolean isOptional()
    {
        return ( options & INCLUDE_OPTIONAL ) != 0;
    }


    public boolean isCalculateLocalDependencies()
    {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean isCompileTime()
    {
        return ( options & COMPILE_TIME ) != 0;
    }
}
