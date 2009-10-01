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

package org.apache.felix.sigil.ui.eclipse.ui;


import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class SigilUI extends AbstractUIPlugin
{

    // The plug-in ID
    public static final String PLUGIN_ID = "org.apache.felix.sigil.eclipse.ui";

    public static final String REPOSITORY_WIZARD_EXTENSION_POINT_ID = "org.apache.felix.sigil.ui.repositorywizard";

    public static final String PREF_NOPROMPT_INSTALL_COMPOSITE_WITH_ERRORS = "nopromptInstallCompositeError";
    public static final String PREF_INSTALL_COMPOSITE_WITH_ERRORS_ANSWER = "answerInstallCompositeError";

    public static final String ID_REPOSITORY_VIEW = "org.apache.felix.sigil.ui.repositoryBrowser";
    public static final String ID_DEPENDENCY_VIEW = "org.apache.felix.sigil.ui.bundleDependencyView";

    // The shared instance
    private static SigilUI plugin;


    /**
     * The constructor
     */
    public SigilUI()
    {
    }


    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start( BundleContext context ) throws Exception
    {
        super.start( context );
        SigilCore.getDefault();
        plugin = this;
    }


    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext context ) throws Exception
    {
        plugin = null;
        super.stop( context );
    }


    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static SigilUI getDefault()
    {
        return plugin;
    }


    public static ResourceBundle getResourceBundle()
    {
        return ResourceBundle.getBundle( "resources." + SigilUI.class.getName(), Locale.getDefault(), SigilUI.class
            .getClassLoader() );
    }


    public static void runWorkspaceOperation( IRunnableWithProgress op, Shell shell )
    {
        if ( shell == null )
        {
            shell = getActiveWorkbenchShell();
        }
        try
        {
            new ProgressMonitorDialog( shell ).run( true, true, op );
        }
        catch ( InvocationTargetException e )
        {
            SigilCore.error( "Workspace operation failed", e );
        }
        catch ( InterruptedException e1 )
        {
            SigilCore.log( "Workspace operation interrupted" );
        }
    }


    public static void runWorkspaceOperationSync( IRunnableWithProgress op, Shell shell ) throws Throwable
    {
        if ( shell == null )
        {
            shell = getActiveWorkbenchShell();
        }
        try
        {
            new ProgressMonitorDialog( shell ).run( false, true, op );
        }
        catch ( InvocationTargetException e )
        {
            throw e.getCause();
        }
        catch ( InterruptedException e1 )
        {
            SigilCore.log( "Workspace operation interrupted" );
        }
    }


    public static IWorkbenchWindow getActiveWorkbenchWindow()
    {
        return getDefault().getWorkbench().getActiveWorkbenchWindow();
    }


    public static Shell getActiveWorkbenchShell()
    {
        final Shell[] shell = new Shell[1];
        runInUISync( new Runnable()
        {
            public void run()
            {
                shell[0] = getActiveDisplay().getActiveShell();
            }
        } );
        return shell[0];
    }


    public static Display getActiveDisplay()
    {
        Display d = Display.getCurrent();

        if ( d == null )
        {
            d = Display.getDefault();
        }

        return d;
    }


    public static void runInUI( Runnable runnable )
    {
        getActiveDisplay().asyncExec( runnable );
    }


    public static void runInUISync( Runnable runnable )
    {
        getActiveDisplay().syncExec( runnable );
    }


    public static Image cacheImage( String path, ClassLoader classLoader )
    {
        ImageRegistry registry = SigilUI.getDefault().getImageRegistry();

        Image image = registry.get( path );

        if ( image == null )
        {
            image = loadImage( path, classLoader );
            // XXX-FIXME-XXX add null image
            if ( image != null )
            {
                registry.put( path, image );
            }
        }

        return image;
    }


    private static Image loadImage( String resource, ClassLoader loader )
    {
        InputStream in = loader.getResourceAsStream( resource );
        if ( in != null )
        {
            ImageData data = new ImageData( in );
            return new Image( SigilUI.getActiveDisplay(), data );
        }
        else
        {
            return null;
        }
    }
}
