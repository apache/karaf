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

package org.apache.felix.sigil.search.index;


import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.JavaClass;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.common.VersionRange;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.search.ISearchResult;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.osgi.framework.Version;


public class Index
{
    private HashMap<String, ClassData> primary = new HashMap<String, ClassData>();
    private HashMap<IBundleRepository, HashSet<String>> secondary = new HashMap<IBundleRepository, HashSet<String>>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    static class ClassData
    {
        HashMap<IBundleRepository, Set<ISearchResult>> provided = new HashMap<IBundleRepository, Set<ISearchResult>>();


        void add( IBundleRepository rep, ISearchResult export )
        {
            Set<ISearchResult> exports = provided.get( rep );

            if ( exports == null )
            {
                exports = new HashSet<ISearchResult>();
                provided.put( rep, exports );
            }

            exports.add( export );
        }


        List<ISearchResult> getResults()
        {
            LinkedList<ISearchResult> exports = new LinkedList<ISearchResult>();
            for ( Set<ISearchResult> p : provided.values() )
            {
                exports.addAll( p );
            }
            return exports;
        }


        void remove( IBundleRepository rep )
        {
            provided.remove( rep );
        }


        boolean isEmpty()
        {
            return provided.isEmpty();
        }
    }

    static class SearchResult implements ISearchResult
    {
        private final String className;
        private final String packageName;
        private final IBundleRepository rep;
        private final String bundleSymbolicName;
        private final Version version;
        private final boolean exported;

        private SoftReference<ISigilBundle> bundleReference;
        private SoftReference<IPackageExport> exportReference;


        public SearchResult( String className, IBundleRepository rep, ISigilBundle bundle, String packageName,
            boolean exported )
        {
            this.className = className;
            this.rep = rep;
            this.exported = exported;
            this.bundleSymbolicName = bundle.getBundleInfo().getSymbolicName();
            this.version = bundle.getVersion();
            this.packageName = packageName;
        }


        public String getClassName()
        {
            return className;
        }


        public String getPackageName()
        {
            return packageName;
        }


        public IPackageExport getExport()
        {
            IPackageExport ipe = null;
            if ( exported )
            {
                ipe = exportReference == null ? null : exportReference.get();
                if ( ipe == null )
                {
                    ipe = getProvider().findExport( packageName );
                    exportReference = new SoftReference<IPackageExport>( ipe );
                }
            }
            return ipe;
        }


        public ISigilBundle getProvider()
        {
            ISigilBundle b = bundleReference == null ? null : bundleReference.get();
            if ( b == null )
            {
                IRequiredBundle rb = ModelElementFactory.getInstance().newModelElement( IRequiredBundle.class );
                rb.setSymbolicName( bundleSymbolicName );
                VersionRange versions = new VersionRange( false, version, version, false );
                rb.setVersions( versions );
                b = rep.findProvider( rb, 0 );
                bundleReference = new SoftReference<ISigilBundle>( b );
            }
            return b;
        }

    }


    public void addEntry( JavaClass c, IBundleRepository rep, ISigilBundle bundle, boolean exported )
    {
        addEntry( c.getClassName(), rep, bundle, c.getPackageName(), exported );
    }


    public void addEntry( ICompilationUnit unit, IBundleRepository rep, ISigilBundle bundle, boolean exported )
    {
        String name = unit.getElementName();
        if ( name.endsWith( ".java" ) )
        {
            name = name.substring( 0, name.length() - 5 );
        }
        IPackageFragment p = ( IPackageFragment ) unit.getAncestor( IJavaElement.PACKAGE_FRAGMENT );
        addEntry( p.getElementName() + "." + name, rep, bundle, p.getElementName(), exported );
    }


    private void addEntry( String className, IBundleRepository rep, ISigilBundle bundle, String packageName,
        boolean exported )
    {
        List<String> keys = genKeys( className );
        lock.writeLock().lock();
        try
        {
            for ( String key : keys )
            {
                ClassData data = primary.get( key );

                if ( data == null )
                {
                    data = new ClassData();
                    primary.put( key, data );
                }

                SearchResult result = new SearchResult( className, rep, bundle, packageName, exported );
                data.add( rep, result );
            }

            HashSet<String> all = secondary.get( rep );
            if ( all == null )
            {
                all = new HashSet<String>();
                secondary.put( rep, all );
            }
            all.addAll( keys );
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }


    public List<ISearchResult> findProviders( String className, IProgressMonitor monitor )
    {
        lock.readLock().lock();
        try
        {
            ClassData data = primary.get( className );
            return data == null ? Collections.<ISearchResult> emptyList() : data.getResults();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }


    public List<ISearchResult> findProviders( Pattern className, IProgressMonitor monitor )
    {
        lock.readLock().lock();
        try
        {
            ClassData data = primary.get( className );
            return data == null ? Collections.<ISearchResult> emptyList() : data.getResults();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }


    public void delete( IBundleRepository rep )
    {
        lock.writeLock().lock();
        try
        {
            Set<String> keys = secondary.remove( rep );
            if ( keys != null )
            {
                for ( String key : keys )
                {
                    ClassData data = primary.get( key );
                    data.remove( rep );
                    if ( data.isEmpty() )
                    {
                        primary.remove( key );
                    }
                }
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }


    private List<String> genKeys( String className )
    {
        LinkedList<String> keys = new LinkedList<String>();
        keys.add( className );
        int i = className.lastIndexOf( '.' );
        if ( i != -1 )
        {
            String name = className.substring( i + 1 );
            keys.add( name );
        }
        return keys;
    }

}
