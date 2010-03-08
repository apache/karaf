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

package org.apache.felix.sigil.eclipse.internal.repository.eclipse;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.internal.repository.eclipse.RepositoryMap.RepositoryCache;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositorySet;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;
import org.apache.felix.sigil.repository.AbstractRepositoryManager;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryManager;
import org.apache.felix.sigil.repository.IRepositoryProvider;
import org.apache.felix.sigil.repository.RepositoryException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;


public class SigilRepositoryManager extends AbstractRepositoryManager implements IRepositoryManager,
    IPropertyChangeListener
{

    private final String repositorySet;

    private RepositoryMap cachedRepositories;

    public SigilRepositoryManager( String repositorySet, RepositoryMap cachedRepositories )
    {
        this.repositorySet = repositorySet;
        this.cachedRepositories = cachedRepositories;
    }


    @Override
    public void initialise()
    {
        super.initialise();
        SigilCore.getDefault().getPreferenceStore().addPropertyChangeListener( this );
    }


    public void destroy()
    {
        IPreferenceStore prefs = SigilCore.getDefault().getPreferenceStore();
        if ( prefs != null )
        {
            prefs.removePropertyChangeListener( this );
        }
    }


    @Override
    protected void loadRepositories()
    {
        IPreferenceStore prefs = SigilCore.getDefault().getPreferenceStore();

        ArrayList<IBundleRepository> repos = new ArrayList<IBundleRepository>();
        HashSet<String> ids = new HashSet<String>();

        IRepositoryModel[] models = findRepositories();
        for ( IRepositoryModel repo : models )
        {
            try
            {
                IRepositoryProvider provider = findProvider( repo.getType() );
                String id = repo.getId();
                IBundleRepository repoImpl = null;
                if ( repo.getType().isDynamic() )
                {
                    String instance = "repository." + repo.getType().getId() + "." + id;
                    String loc = prefs.getString( instance + ".loc" );
                    Properties pref = loadPreferences( loc );
                    repoImpl = loadRepository( id, pref, provider );
                }
                else
                {
                    repoImpl = loadRepository( id, null, provider );
                }

                repos.add( repoImpl );
                ids.add( id );
            }
            catch ( Exception e )
            {
                SigilCore.error( "Failed to load repository for " + repo, e );
            }
        }

        setRepositories( repos.toArray( new IBundleRepository[repos.size()] ) );

        cachedRepositories.retainAll( ids );
    }


    private IRepositoryProvider findProvider( IRepositoryType repositoryType ) throws CoreException
    {
        String id = repositoryType.getId();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint p = registry.getExtensionPoint( SigilCore.REPOSITORY_PROVIDER_EXTENSION_POINT_ID );

        for ( IExtension e : p.getExtensions() )
        {
            for ( IConfigurationElement c : e.getConfigurationElements() )
            {
                if ( id.equals( c.getAttribute( "id" ) ) )
                {
                    IRepositoryProvider provider = ( IRepositoryProvider ) c.createExecutableExtension( "class" );
                    return provider;
                }
            }
        }

        return null;
    }


    protected IRepositoryModel[] findRepositories()
    {
        if ( repositorySet == null )
        {
            return SigilCore.getRepositoryConfiguration().getDefaultRepositorySet().getRepositories();
        }
        else
        {
            IRepositorySet set = SigilCore.getRepositoryConfiguration().getRepositorySet( repositorySet );
            return set.getRepositories();
        }
    }


    private IBundleRepository loadRepository( String id, Properties pref, IRepositoryProvider provider )
        throws RepositoryException
    {
        try
        {
            if ( pref == null )
            {
                pref = new Properties();
            }

            RepositoryCache cache = cachedRepositories.get( id );

            if ( cache == null || !cache.pref.equals( pref ) )
            {
                IBundleRepository repo = provider.createRepository( id, pref );
                cache = new RepositoryCache( pref, repo );
                cachedRepositories.put( id, cache );
            }

            return cache.repo;
        }
        catch ( RuntimeException e )
        {
            throw new RepositoryException( "Failed to build repositories", e );
        }
    }


    private Properties loadPreferences( String loc ) throws FileNotFoundException, IOException
    {
        FileInputStream in = null;
        try
        {
            Properties pref = new Properties();
            pref.load( new FileInputStream( loc ) );
            return pref;
        }
        finally
        {
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( IOException e )
                {
                    SigilCore.error( "Failed to close file", e );
                }
            }
        }
    }


    public void propertyChange( PropertyChangeEvent event )
    {
        if ( event.getProperty().equals( "repository.timestamp" ) )
        {
            loadRepositories();
        }
    }
}
