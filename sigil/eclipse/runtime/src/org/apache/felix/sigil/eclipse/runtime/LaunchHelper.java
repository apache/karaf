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

package org.apache.felix.sigil.eclipse.runtime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.felix.sigil.common.runtime.BundleForm;
import org.apache.felix.sigil.common.runtime.Runtime;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.install.IOSGiInstall;
import org.apache.felix.sigil.eclipse.runtime.config.OSGiLaunchConfigurationConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;

public class LaunchHelper {

	public static IOSGiInstall getInstall(ILaunchConfiguration config) {
	    return null;
	}

    public static int getRetries( ILaunchConfiguration config )
    {
        return 5;
    }

    public static Properties buildClientProps( ILaunchConfiguration config )
    {
        Properties props = new Properties();
        props.put( Runtime.ADDRESS_PROPERTY, "localhost" );
        props.put( Runtime.PORT_PROPERTY, "9090" );
        return props;
    }

    public static String[] getProgramArgs( ILaunchConfiguration config )
    {
        return new String[] { "-p", "9090", "-a", "localhost", "-c" };
    }

    public static long getBackoff( ILaunchConfiguration config )
    {
        return 1000;
    }
    
    public static URL toURL(String loc) throws MalformedURLException
    {
        URL url = null;
        try
        {
            url = new URL(loc);
        }
        catch (MalformedURLException e)
        {
            IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(loc));
            if ( f.exists() ) {
                url = f.getLocation().toFile().toURL();
            }
            else {
                throw new MalformedURLException("Unknown file " + loc );
            }
        }
        return url;
    }

    public static BundleForm getBundleForm(ILaunchConfiguration config) throws CoreException
    {
        String loc = config.getAttribute(OSGiLaunchConfigurationConstants.FORM_FILE_LOCATION, (String) null);
        try
        {
            URL url = toURL(loc);
            SigilCore.log("Resolving " + url);
            return BundleForm.create(url);
        }
        catch (Exception e)
        {
            throw SigilCore.newCoreException("Failed to parse bundle form file", e);
        }
    }

    public static String getRepositoryManagerName(ILaunchConfiguration config)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
