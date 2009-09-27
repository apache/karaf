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

package org.apache.felix.sigil.eclipse.internal.model.repository;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryConfiguration;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositorySet;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;
import org.apache.felix.sigil.eclipse.model.repository.RepositorySet;
import org.apache.felix.sigil.eclipse.preferences.PrefsUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;


public class RepositoryConfiguration implements IRepositoryConfiguration
{

    private static final String REPOSITORY = "repository.";
    private static final String REPOSITORY_SET = REPOSITORY + "set.";
    private static final String REPOSITORY_SETS = REPOSITORY + "sets";
    private static final String REPOSITORY_TIMESTAMP = REPOSITORY + "timestamp";
    private static final String INSTANCES = ".instances";
    private static final String NAME = ".name";
    private static final String LOC = ".loc";
    private static final String TIMESTAMP = ".timestamp";

    public static final String REPOSITORY_DEFAULT_SET = REPOSITORY + "default.set";


    public List<IRepositoryModel> loadRepositories()
    {
        IPreferenceStore prefs = SigilCore.getDefault().getPreferenceStore();

        ArrayList<IRepositoryModel> repositories = new ArrayList<IRepositoryModel>();

        for ( RepositoryType type : loadRepositoryTypes() )
        {
            String typeID = type.getId();

            if ( type.isDynamic() )
            {
                String instances = prefs.getString( REPOSITORY + typeID + INSTANCES );
                if ( instances.trim().length() > 0 )
                {
                    for ( String instance : instances.split( "," ) )
                    {
                        String key = REPOSITORY + typeID + "." + instance;
                        repositories.add( loadRepository( instance, key, type, prefs ) );
                    }
                }
            }
            else
            {
                String key = REPOSITORY + typeID;
                repositories.add( loadRepository( typeID, key, type, prefs ) );
            }

        }

        return repositories;
    }


    public IRepositoryModel findRepository( String id )
    {
        for ( IRepositoryModel model : loadRepositories() )
        {
            if ( model.getId().equals( id ) )
            {
                return model;
            }
        }
        return null;
    }


    public void saveRepositories( List<IRepositoryModel> repositories ) throws CoreException
    {
        IPreferenceStore prefs = getPreferences();

        HashMap<IRepositoryType, List<IRepositoryModel>> mapped = new HashMap<IRepositoryType, List<IRepositoryModel>>(
            repositories.size() );

        saveRepositoryPreferences( repositories, mapped );
        createNewEntries( mapped, prefs );
        deleteOldEntries( repositories, prefs );
        // time stamp is used as a signal to the manager
        // to update its view of the stored repositories
        timeStamp( prefs );
    }


    public List<RepositoryType> loadRepositoryTypes()
    {
        List<RepositoryType> repositories = new ArrayList<RepositoryType>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();

        IExtensionPoint p = registry.getExtensionPoint( SigilCore.REPOSITORY_PROVIDER_EXTENSION_POINT_ID );

        for ( IExtension e : p.getExtensions() )
        {
            for ( IConfigurationElement c : e.getConfigurationElements() )
            {
                String id = c.getAttribute( "id" );
                String type = c.getAttribute( "type" );
                boolean dynamic = Boolean.valueOf( c.getAttribute( "dynamic" ) );
                String icon = c.getAttribute( "icon" );
                Image image = ( icon == null || icon.trim().length() == 0 ) ? null : loadImage( e, icon );
                repositories.add( new RepositoryType( id, type, dynamic, image ) );
            }
        }

        return repositories;
    }


    public IRepositoryModel newRepositoryElement( IRepositoryType type )
    {
        String id = UUID.randomUUID().toString();
        PreferenceStore prefs = new PreferenceStore();
        RepositoryModel element = new RepositoryModel( id, "", type, prefs );
        prefs.setFilename( makeFileName( element ) );
        prefs.setValue( "id", id );
        return element;
    }


    public IRepositorySet getDefaultRepositorySet()
    {
        //int level = findLevel( key + LEVEL, type, prefs );
        ArrayList<IRepositoryModel> reps = new ArrayList<IRepositoryModel>();
        for ( String s : PrefsUtils.stringToArray( getPreferences().getString( REPOSITORY_DEFAULT_SET ) ) )
        {   
            IRepositoryModel rep = findRepository( s );
            if ( rep == null ) {
                SigilCore.error( "Missing repository for " + s );
            }
            else {
                reps.add( rep );
            }
        }
        return new RepositorySet( reps );
    }


    public IRepositorySet getRepositorySet( String name )
    {
        String key = REPOSITORY_SET + name;
        if ( getPreferences().contains( key ) )
        {
            ArrayList<IRepositoryModel> reps = new ArrayList<IRepositoryModel>();
            for ( String s : PrefsUtils.stringToArray( getPreferences().getString( key ) ) )
            {
                IRepositoryModel rep = findRepository( s );
                if ( rep == null ) {
                    throw new IllegalStateException( "Missing repository for " + s );
                }
                reps.add( rep );
            }
            return new RepositorySet( reps );
        }
        else
        {
            return null;
        }
    }


    public Map<String, IRepositorySet> loadRepositorySets()
    {
        IPreferenceStore store = getPreferences();

        HashMap<String, IRepositorySet> sets = new HashMap<String, IRepositorySet>();

        for ( String name : PrefsUtils.stringToArray( store.getString( REPOSITORY_SETS ) ) )
        {
            String key = REPOSITORY_SET + name;
            ArrayList<IRepositoryModel> reps = new ArrayList<IRepositoryModel>();
            for ( String s : PrefsUtils.stringToArray( getPreferences().getString( key ) ) )
            {
                reps.add( findRepository( s ) );
            }
            sets.put( name, new RepositorySet( reps ) );
        }

        return sets;
    }


    public void saveRepositorySets( Map<String, IRepositorySet> sets )
    {
        IPreferenceStore store = getPreferences();

        ArrayList<String> names = new ArrayList<String>();

        for ( Map.Entry<String, IRepositorySet> set : sets.entrySet() )
        {
            String name = set.getKey();
            String key = REPOSITORY_SET + name;
            ArrayList<String> ids = new ArrayList<String>();
            for ( IRepositoryModel m : set.getValue().getRepositories() )
            {
                ids.add( m.getId() );
            }
            store.setValue( key, PrefsUtils.listToString( ids ) );
            names.add( name );
        }

        for ( String name : PrefsUtils.stringToArray( store.getString( REPOSITORY_SETS ) ) )
        {
            if ( !names.contains( name ) )
            {
                String key = REPOSITORY_SET + name;
                store.setToDefault( key );
            }
        }

        store.setValue( REPOSITORY_SETS, PrefsUtils.listToString( names ) );
        timeStamp( store );
    }


    public void setDefaultRepositorySet( IRepositorySet defaultSet )
    {
        ArrayList<String> ids = new ArrayList<String>();
        for ( IRepositoryModel m : defaultSet.getRepositories() )
        {
            ids.add( m.getId() );
        }
        IPreferenceStore prefs = getPreferences();
        prefs.setValue( REPOSITORY_DEFAULT_SET, PrefsUtils.listToString( ids ) );
        timeStamp( prefs );
    }


    private void timeStamp( IPreferenceStore prefs )
    {
        prefs.setValue( REPOSITORY_TIMESTAMP, System.currentTimeMillis() );
    }


    private IPreferenceStore getPreferences()
    {
        return SigilCore.getDefault().getPreferenceStore();
    }


    private void deleteOldEntries( List<IRepositoryModel> repositories, IPreferenceStore prefs )
    {
        for ( IRepositoryModel e : loadRepositories() )
        {
            if ( !repositories.contains( e ) )
            {
                new File( makeFileName( e ) ).delete();
                String key = makeKey( e );
                prefs.setToDefault( key + LOC );
                prefs.setToDefault( key + NAME );
            }
        }

        for ( IRepositoryType type : loadRepositoryTypes() )
        {
            boolean found = false;
            for ( IRepositoryModel e : repositories )
            {
                if ( e.getType().equals( type ) )
                {
                    found = true;
                    break;
                }
            }

            if ( !found )
            {
                prefs.setToDefault( REPOSITORY + type.getId() + INSTANCES );
            }
        }
    }


    private static void createNewEntries( HashMap<IRepositoryType, List<IRepositoryModel>> mapped,
        IPreferenceStore prefs )
    {
        for ( Map.Entry<IRepositoryType, List<IRepositoryModel>> entry : mapped.entrySet() )
        {
            IRepositoryType type = entry.getKey();
            if ( type.isDynamic() )
            {
                StringBuffer buf = new StringBuffer();

                for ( IRepositoryModel element : entry.getValue() )
                {
                    if ( buf.length() > 0 )
                    {
                        buf.append( "," );
                    }
                    buf.append( element.getId() );
                    saveRepository( element, prefs );
                }

                prefs.setValue( REPOSITORY + type.getId() + INSTANCES, buf.toString() );
            }
            else
            {
                IRepositoryModel element = entry.getValue().get( 0 );
                saveRepository( element, prefs );
            }
        }
    }


    private static void saveRepositoryPreferences( List<IRepositoryModel> repositories,
        HashMap<IRepositoryType, List<IRepositoryModel>> mapped ) throws CoreException
    {
        for ( IRepositoryModel rep : repositories )
        {
            try
            {
                createDir( makeFileName( rep ) );
                rep.getPreferences().save();
                List<IRepositoryModel> list = mapped.get( rep.getType() );
                if ( list == null )
                {
                    list = new ArrayList<IRepositoryModel>( 1 );
                    mapped.put( rep.getType(), list );
                }
                list.add( rep );
            }
            catch ( IOException e )
            {
                throw SigilCore.newCoreException( "Failed to save repository preferences", e );
            }
        }
    }


    private static void createDir( String fileName )
    {
        File file = new File( fileName );
        file.getParentFile().mkdirs();
    }


    private static void saveRepository( IRepositoryModel element, IPreferenceStore prefs )
    {
        String key = makeKey( element );
        prefs.setValue( key + LOC, makeFileName( element ) );
        if ( element.getType().isDynamic() )
        {
            prefs.setValue( key + NAME, element.getName() );
        }
        prefs.setValue( key + TIMESTAMP, now() );
    }


    private static long now()
    {
        return System.currentTimeMillis();
    }


    private static String makeKey( IRepositoryModel element )
    {
        IRepositoryType type = element.getType();

        String key = REPOSITORY + type.getId();
        if ( type.isDynamic() )
            key = key + "." + element.getId();

        return key;
    }


    private static String makeFileName( IRepositoryModel element )
    {
        IPath path = SigilCore.getDefault().getStateLocation();
        path = path.append( "repository" );
        path = path.append( element.getType().getId() );
        path = path.append( element.getId() );
        return path.toOSString();
    }


    private static RepositoryModel loadRepository( String id, String key, RepositoryType type, IPreferenceStore prefs )
    {
        String name = type.isDynamic() ? prefs.getString( key + NAME ) : type.getType();

        PreferenceStore repPrefs = new PreferenceStore();
        RepositoryModel element = new RepositoryModel( id, name, type, repPrefs );

        String loc = prefs.getString( key + LOC );

        if ( loc == null || loc.trim().length() == 0 )
        {
            loc = makeFileName( element );
        }

        repPrefs.setFilename( loc );

        if ( new File( loc ).exists() )
        {
            try
            {
                repPrefs.load();
            }
            catch ( IOException e )
            {
                SigilCore.error( "Failed to load properties for repository " + key, e );
            }
        }

        repPrefs.setValue( "id", id );

        return element;
    }


    @SuppressWarnings("unchecked")
    private static Image loadImage( IExtension ext, String icon )
    {
        int i = icon.lastIndexOf( "/" );
        String path = i == -1 ? "/" : icon.substring( 0, i );
        String name = i == -1 ? icon : icon.substring( i + 1 );

        Bundle b = Platform.getBundle( ext.getContributor().getName() );

        Enumeration<URL> en = b.findEntries( path, name, false );
        Image image = null;

        if ( en.hasMoreElements() )
        {
            try
            {
                image = SigilCore.loadImage( en.nextElement() );
            }
            catch ( IOException e )
            {
                SigilCore.error( "Failed to load image", e );
            }
        }
        else
        {
            SigilCore.error( "No such image " + icon + " in bundle " + b.getSymbolicName() );
        }

        return image;
    }

}
