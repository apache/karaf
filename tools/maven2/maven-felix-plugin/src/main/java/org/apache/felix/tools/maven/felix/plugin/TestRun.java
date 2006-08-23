/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.tools.maven.felix.plugin;


public class TestRun
{
    private boolean isFailure;
    private boolean isError;
    private Throwable t;
    private long startTime;
    private long endTime;
    
    
    public void setFailure( boolean isFailure )
    {
        this.isFailure = isFailure;
    }
    
    
    public boolean isFailure()
    {
        return isFailure;
    }


    public void setError( boolean isError )
    {
        this.isError = isError;
    }


    public boolean isError()
    {
        return isError;
    }


    public void setThrowable( Throwable t )
    {
        this.t = t;
    }


    public Throwable getThrowable()
    {
        return t;
    }


    public void setStartTime( long startTime )
    {
        this.startTime = startTime;
    }


    public long getStartTime()
    {
        return startTime;
    }


    public void setEndTime( long endTime )
    {
        this.endTime = endTime;
    }


    public long getEndTime()
    {
        return endTime;
    }
}
