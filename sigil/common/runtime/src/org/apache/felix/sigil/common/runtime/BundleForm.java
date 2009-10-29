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
package org.apache.felix.sigil.common.runtime;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class BundleForm
{    
    public interface Resolver {
        URI[] resolve(URI base) throws URISyntaxException;
    }
    
    public interface ResolutionContext {
        Resolver findResolver(URI uri);
    }
    
    private static final Resolver NULL_RESOLVER = new Resolver() {

        public URI[] resolve(URI base)
        {
            return new URI[] { base };
        }
        
    };
    private static final ResolutionContext NULL_CONTEXT = new ResolutionContext()
    {      
        public Resolver findResolver(URI uri)
        {
            return NULL_RESOLVER;
        }
    };
    
    public static class BundleStatus
    {
        private String location;
        private String bundleSymbolicName;
        private String version;
        private long id;
        private int status;
        
        public String getLocation()
        {
            return location;
        }
        
        public void setLocation(String location)
        {
            this.location = location;
        }
        
        public String getBundleSymbolicName()
        {
            return bundleSymbolicName;
        }
        
        public void setBundleSymbolicName(String bundleSymbolicName)
        {
            this.bundleSymbolicName = bundleSymbolicName;
        }
        
        public String getVersion()
        {
            return version;
        }
        
        public void setVersion(String version)
        {
            this.version = version;
        }
        
        public long getId()
        {
            return id;
        }
        
        public void setId(long id)
        {
            this.id = id;
        }
        
        public void setStatus(int status)
        {
            this.status = status;
        }    
        
        public int getStatus() {
            return status;
        }

        public boolean isMatch(BundleStatus n)
        {
            return bundleSymbolicName.equals( n.bundleSymbolicName ) && version.equals(n.version);
        }
    }
    
    private URI[] bundles;
    private Set<URI> startMap = new HashSet<URI>();

    public BundleForm() {
    }
    
    public BundleStatus[] resolve(ResolutionContext ctx) throws IOException, URISyntaxException {
        if ( ctx == null ) {
            ctx = NULL_CONTEXT;
        }
        
        ArrayList<BundleStatus> ret = new ArrayList<BundleStatus>(bundles.length);
        
        for ( int i = 0; i < bundles.length; i++ ) {
            Resolver resolver = ctx.findResolver(bundles[i]);
            if ( resolver == null ) {
                resolver = NULL_RESOLVER;
            }
            URI[] resolved = resolver.resolve(bundles[i]);
            for ( URI uri : resolved ) {
                ret.add( toBundle(uri, isStarted(bundles[i])) );
            }
        }
        
        return ret.toArray(new BundleStatus[ret.size()] );

    }
    
    private BundleStatus toBundle(URI uri, boolean started) throws IOException
    {
        URL url = uri.toURL();
        InputStream in = url.openStream();
        try {
            JarInputStream jin = new JarInputStream(in);
            Manifest mf = jin.getManifest();
            Attributes attr = mf.getMainAttributes();
            String bsn = attr.getValue(Constants.BUNDLE_SYMBOLICNAME);
            String ver = attr.getValue(Constants.BUNDLE_VERSION);
            BundleStatus st = new BundleStatus();
            st.setBundleSymbolicName(bsn);
            st.setVersion(ver);
            st.setLocation(url.toExternalForm());
            st.setStatus(started ? Bundle.ACTIVE : Bundle.INSTALLED);
            return st;
        }
        finally {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static BundleForm create(URL formURL) throws IOException, URISyntaxException {
        InputStream in = formURL.openStream();
        try {
            BundleForm f = new BundleForm();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            LinkedList<URI> locs = new LinkedList<URI>();
            for(;;) {
                String l = r.readLine();
                if ( l == null ) break;
                l = l.trim();
                if ( !l.startsWith( "#" ) ) {
                    URI uri = URI.create(l);
                    String status = uri.getScheme();
                    uri = URI.create(uri.getSchemeSpecificPart());
                    locs.add( uri );
                    f.setStarted(uri, "start".equalsIgnoreCase(status) );
                }
            }
            f.setBundles(locs.toArray(new URI[locs.size()]));
            return f;
        }
        finally {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void setBundles(URI[] bundles) {
        this.bundles = bundles;
    }
    
    public URI[] getBundles()
    {
        return bundles;
    }

    public boolean isStarted(URI uri)
    {
        return startMap.contains(uri);
    }
    
    public void setStarted(URI uri, boolean started) {
        if ( started ) {
            startMap.add(uri);
        }
        else {
            startMap.remove(uri);
        }
    }    
}
