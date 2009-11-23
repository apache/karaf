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


import java.io.IOException;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.felix.sigil.common.runtime.BundleForm;
import org.apache.felix.sigil.common.runtime.Client;
import org.apache.felix.sigil.common.runtime.Main;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.install.IOSGiInstall;
import org.apache.felix.sigil.repository.IRepositoryManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;


public class OSGiLauncher extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate,
    ILaunchConfigurationDelegate2
{

    public void launch( ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor )
        throws CoreException
    {
        IOSGiInstall osgi = LaunchHelper.getInstall( config );

        VMRunnerConfiguration vmconfig = new VMRunnerConfiguration( Main.class.getName(), buildClasspath( osgi, config ) );

        String vm = getVMArguments( config );
        if ( vm != null && vm.trim().length() > 0 )
            vmconfig.setVMArguments( vm.split( " " ) );
                
        IPath path = getWorkingDirectoryPath( config );
        vmconfig.setWorkingDirectory( path == null ? null : path.toOSString() );
        
        vmconfig.setBootClassPath( getBootpath( config ) );
        vmconfig.setEnvironment( getEnvironment( config ) );
        vmconfig.setProgramArguments( LaunchHelper.getProgramArgs( config ) );

        IVMInstall install = getVMInstall( config );

        IVMRunner runner = install.getVMRunner( mode );

        setDefaultSourceLocator( launch, config );

        SigilCore.log( "VM=" + install.getName() );
        SigilCore.log( "Main=" + vmconfig.getClassToLaunch() );
        SigilCore.log( "VMArgs=" + Arrays.asList( vmconfig.getVMArguments() ) );
        SigilCore.log( "Boot Classpath=" + Arrays.asList( vmconfig.getBootClassPath() ) );
        SigilCore.log( "Classpath=" + Arrays.asList( vmconfig.getClassPath() ) );
        SigilCore.log( "Args=" + Arrays.asList( vmconfig.getProgramArguments() ) );
        SigilCore.log( "Working Dir=" + vmconfig.getWorkingDirectory() );

        runner.run( vmconfig, launch, monitor );

        Client client = connect( config );
        
        BundleForm form = LaunchHelper.getBundleForm(config);
        
        try
        {
            String name = LaunchHelper.getRepositoryManagerName(config);
            IRepositoryManager manager = SigilCore.getRepositoryManager(name);
            client.apply(form.resolve(new RuntimeBundleResolver(manager, config)));
        }
        catch (Exception e)
        {
            throw SigilCore.newCoreException("Failed to apply bundle form", e);
        }

        SigilCore.log( "Connected " + client.isConnected() );
    }


    private Client connect( ILaunchConfiguration config ) throws CoreException
    {
        Properties props = LaunchHelper.buildClientProps( config );

        int retry = LaunchHelper.getRetries( config );

        Client client = null;

        for ( int i = 0; i < retry; i++ )
        {
            client = new Client();
            try
            {
                client.connect( props );
                break;
            }
            catch ( ConnectException e )
            {
                SigilCore.log( "Failed to connect to client: " + e.getMessage() );
            }
            catch ( IOException e )
            {
                throw SigilCore.newCoreException( "Failed to connect client", e );
            }

            try
            {
                Thread.sleep( LaunchHelper.getBackoff( config ) );
            }
            catch ( InterruptedException e )
            {
                SigilCore.log( "Interrupted during backoff" );
            }
        }

        if ( client == null )
        {
            throw SigilCore.newCoreException( "Failed to connect client after retries, check error log for details",
                null );
        }

        return client;
    }


    public String[] getBootpath( ILaunchConfiguration configuration ) throws CoreException
    {
        String[] bootpath = super.getBootpath( configuration );

        ArrayList<String> filtered = new ArrayList<String>();

        if ( bootpath != null )
        {
            for ( String bp : bootpath )
            {
                if ( !SigilCore.isBundledPath( bp ) )
                {
                    filtered.add( bp );
                }
            }
        }

        return filtered.toArray( new String[filtered.size()] );
    }


    private String[] buildClasspath( IOSGiInstall osgi, ILaunchConfiguration config ) throws CoreException
    {
        ArrayList<String> cp = new ArrayList<String>();

        cp.add( Main.class.getProtectionDomain().getCodeSource().getLocation().getFile() );

        for ( String c : getClasspath( config ) )
        {
            cp.add( c );
        }

        if ( osgi != null )
        {
            for ( String c : osgi.getType().getClassPath() )
            {
                cp.add( c );
            }
        }

        return cp.toArray( new String[cp.size()] );
    }

}
