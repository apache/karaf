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
package org.apache.felix.scrplugin.mojo;


import org.apache.felix.scrplugin.Log;


/**
 * The <code>MavenLog</code> class implements the {@link Log} interface using
 * the Maven logger created on instantiation.
 */
public class MavenLog implements Log
{

    private final org.apache.maven.plugin.logging.Log mavenLog;


    MavenLog( org.apache.maven.plugin.logging.Log mavenLog )
    {
        this.mavenLog = mavenLog;
    }


    public void debug( String content, Throwable error )
    {
        mavenLog.debug( content, error );
    }


    public void debug( String content )
    {
        mavenLog.debug( content );
    }


    public void debug( Throwable error )
    {
        mavenLog.debug( error );
    }


    public void error( String content, Throwable error )
    {
        mavenLog.error( content, error );
    }


    public void error( String content, String location, int lineNumber )
    {
        if ( isErrorEnabled() )
        {
            final String message = formatMessage( content, location, lineNumber );
            mavenLog.error( message );
        }
    }


    public void error( String content )
    {
        mavenLog.error( content );
    }


    public void error( Throwable error )
    {
        mavenLog.error( error );
    }


    public void info( String content, Throwable error )
    {
        mavenLog.info( content, error );
    }


    public void info( String content )
    {
        mavenLog.info( content );
    }


    public void info( Throwable error )
    {
        mavenLog.info( error );
    }


    public boolean isDebugEnabled()
    {
        return mavenLog.isDebugEnabled();
    }


    public boolean isErrorEnabled()
    {
        return mavenLog.isErrorEnabled();
    }


    public boolean isInfoEnabled()
    {
        return mavenLog.isInfoEnabled();
    }


    public boolean isWarnEnabled()
    {
        return mavenLog.isWarnEnabled();
    }


    public void warn( String content, Throwable error )
    {
        mavenLog.warn( content, error );
    }


    public void warn( String content, String location, int lineNumber )
    {
        if ( isWarnEnabled() )
        {
            final String message = formatMessage( content, location, lineNumber );
            mavenLog.warn( message );
        }
    }


    public void warn( String content )
    {
        mavenLog.warn( content );
    }


    public void warn( Throwable error )
    {
        mavenLog.warn( error );
    }


    private String formatMessage( String content, String location, int lineNumber )
    {
        return content + " at " + location + ":" + lineNumber;
    }
}
