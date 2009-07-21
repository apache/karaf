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

package org.apache.felix.sigil.eclipse.install;


import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.IPath;


public class OSGiInstall implements IOSGiInstall
{

    private String id;
    private IPath installLocation;
    private String[] launchArgs;
    private Map<String, String> properties;
    private IPath varDirectory;
    private IOSGiInstallType type;


    public IPath getVarDirectory()
    {
        return varDirectory;
    }


    public void setVarDirectory( IPath varDirectory )
    {
        this.varDirectory = varDirectory;
    }


    public OSGiInstall( String id )
    {
        this.id = id;
    }


    public String getId()
    {
        return id;
    }


    public IPath getInstallLocation()
    {
        return installLocation;
    }


    public void setInstallLocation( IPath installLocation )
    {
        this.installLocation = installLocation;
    }


    public String[] getLaunchArguments()
    {
        return launchArgs;
    }


    public void setLaunchArguments( String[] launchArgs )
    {
        this.launchArgs = launchArgs;
    }


    public Map<String, String> getProperties()
    {
        return properties;
    }


    public void setProperties( Map<String, String> properties )
    {
        this.properties = properties;
    }


    public String toString()
    {
        return "OSGiInstall[\n" + "id=" + id + "\n" + "type=" + type + "\n" + "installLocation=" + installLocation
            + "\n" + "launchArgs=" + Arrays.asList( launchArgs ) + "\n" + "properties=" + properties + "\n" + "]";
    }


    public IOSGiInstallType getType()
    {
        return type;
    }


    public void setType( IOSGiInstallType type )
    {
        this.type = type;
    }
}
