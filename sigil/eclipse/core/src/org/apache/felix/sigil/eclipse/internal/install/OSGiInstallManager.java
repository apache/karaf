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

package org.apache.felix.sigil.eclipse.internal.install;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.install.IOSGiInstall;
import org.apache.felix.sigil.eclipse.install.IOSGiInstallBuilder;
import org.apache.felix.sigil.eclipse.install.IOSGiInstallManager;
import org.apache.felix.sigil.eclipse.install.IOSGiInstallType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;


public class OSGiInstallManager implements IOSGiInstallManager, IPropertyChangeListener
{
    private static final int NORMAL_PRIORITY = 0;

    private LinkedList<IOSGiInstallBuilder> builders = new LinkedList<IOSGiInstallBuilder>();

    private HashMap<IPath, IOSGiInstall> pathToinstall = new HashMap<IPath, IOSGiInstall>();
    private HashMap<String, IOSGiInstall> idToInstall = new HashMap<String, IOSGiInstall>();

    private String defaultId;

    private boolean initialised;


    public IOSGiInstall findInstall( String id )
    {
        init();
        return idToInstall.get( id );
    }


    public String[] getInstallIDs()
    {
        init();
        return idToInstall.keySet().toArray( new String[idToInstall.size()] );
    }


    public IOSGiInstall[] getInstalls()
    {
        init();
        return idToInstall.values().toArray( new IOSGiInstall[idToInstall.size()] );
    }


    public IOSGiInstall getDefaultInstall()
    {
        init();
        return findInstall( defaultId );
    }


    public IOSGiInstallType findInstallType( String location )
    {
        IOSGiInstallType type = null;

        try
        {
            IOSGiInstall install = buildInstall( "tmp", new Path( location ) );
            type = install == null ? null : install.getType();
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Failed to build install", e );
        }

        return type;
    }


    public void propertyChange( PropertyChangeEvent event )
    {
        synchronized ( this )
        {
            if ( event.getProperty().equals( SigilCore.OSGI_INSTALLS ) )
            {
                clearInstalls();
                String val = ( String ) event.getNewValue();
                addInstalls( val );
            }
            else if ( event.getProperty().equals( SigilCore.OSGI_DEFAULT_INSTALL_ID ) )
            {
                defaultId = ( String ) event.getNewValue();
            }
        }
    }


    private void init()
    {
        boolean show = false;

        IPreferenceStore prefs = getPreferenceStore();

        synchronized ( this )
        {
            if ( !initialised )
            {
                initialised = true;

                prefs.addPropertyChangeListener( this );

                String val = prefs.getString( SigilCore.OSGI_INSTALLS );

                boolean noAsk = prefs.getBoolean( SigilCore.PREFERENCES_NOASK_OSGI_INSTALL );
                if ( val == null || val.trim().length() == 0 )
                {
                    show = !noAsk;
                }
                else
                {
                    addInstalls( val );
                    defaultId = prefs.getString( SigilCore.OSGI_DEFAULT_INSTALL_ID );
                }
            }
        }

        if ( show )
        {
            showInstallPrefs( prefs );
        }
    }


    private void addInstalls( String prop )
    {
        if ( prop != null && prop.trim().length() > 0 )
        {
            IPreferenceStore prefs = getPreferenceStore();

            for ( String id : prop.split( "," ) )
            {
                String path = prefs.getString( SigilCore.OSGI_INSTALL_PREFIX + id );
                addInstall( id, new Path( path ) );
            }
        }
    }


    private IPreferenceStore getPreferenceStore()
    {
        return SigilCore.getDefault().getPreferenceStore();
    }


    private void showInstallPrefs( final IPreferenceStore prefs )
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                MessageDialogWithToggle questionDialog = MessageDialogWithToggle.openYesNoQuestion( PlatformUI
                    .getWorkbench().getActiveWorkbenchWindow().getShell(), "Sigil Configuration",
                    "Missing OSGi installation. Open preferences to configure it now?",
                    "Do not show this message again", false, null, null );
                prefs.setValue( SigilCore.PREFERENCES_NOASK_OSGI_INSTALL, questionDialog.getToggleState() );
                if ( questionDialog.getReturnCode() == IDialogConstants.YES_ID )
                {
                    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn( null,
                        SigilCore.OSGI_INSTALLS_PREFERENCES_ID, null, null );
                    dialog.open();
                }
            }
        };
        Display d = Display.getCurrent();
        if ( d == null )
        {
            d = Display.getDefault();
            d.asyncExec( r );
        }
        else
        {
            d.syncExec( r );
        }
    }


    private IOSGiInstall addInstall( String id, IPath path )
    {
        IOSGiInstall install = pathToinstall.get( path );

        if ( install == null )
        {
            try
            {
                install = buildInstall( id, path );
                if ( install != null )
                {
                    pathToinstall.put( path, install );
                    idToInstall.put( install.getId(), install );
                }
            }
            catch ( CoreException e )
            {
                SigilCore.error( "Failed to build install for " + path, e );
            }
        }

        return install;
    }


    private IOSGiInstall buildInstall( String id, IPath path ) throws CoreException
    {
        initBuilders();
        IOSGiInstall install = null;

        for ( IOSGiInstallBuilder b : builders )
        {
            install = b.build( id, path );

            if ( install != null )
            {
                break;
            }
        }

        return install;
    }


    private void clearInstalls()
    {
        idToInstall.clear();
        pathToinstall.clear();
    }


    private void initBuilders()
    {
        synchronized ( builders )
        {
            if ( builders.isEmpty() )
            {
                final HashMap<IOSGiInstallBuilder, Integer> tmp = new HashMap<IOSGiInstallBuilder, Integer>();

                IExtensionRegistry registry = Platform.getExtensionRegistry();
                IExtensionPoint p = registry.getExtensionPoint( SigilCore.INSTALL_BUILDER_EXTENSION_POINT_ID );
                for ( IExtension e : p.getExtensions() )
                {
                    for ( IConfigurationElement c : e.getConfigurationElements() )
                    {
                        createBuilderFromElement( c, tmp );
                    }
                }

                builders = new LinkedList<IOSGiInstallBuilder>( tmp.keySet() );
                Collections.sort( builders, new Comparator<IOSGiInstallBuilder>()
                {
                    public int compare( IOSGiInstallBuilder o1, IOSGiInstallBuilder o2 )
                    {
                        int p1 = tmp.get( o1 );
                        int p2 = tmp.get( o2 );

                        if ( p1 == p2 )
                        {
                            return 0;
                        }
                        else if ( p1 > p2 )
                        {
                            return -1;
                        }
                        else
                        {
                            return 1;
                        }
                    }
                } );
            }
        }
    }


    private void createBuilderFromElement( IConfigurationElement c, Map<IOSGiInstallBuilder, Integer> builder )
    {
        try
        {
            IOSGiInstallBuilder b = ( IOSGiInstallBuilder ) c.createExecutableExtension( "class" );
            int priority = parsePriority( c );
            builder.put( b, priority );
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Failed to create builder", e );
        }
    }


    private int parsePriority( IConfigurationElement c )
    {
        String str = c.getAttribute( "priority" );

        if ( str == null )
        {
            return NORMAL_PRIORITY;
        }
        else
        {
            return Integer.parseInt( str );
        }
    }
}
