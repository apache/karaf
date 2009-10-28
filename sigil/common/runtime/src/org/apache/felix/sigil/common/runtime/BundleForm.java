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
    
    private String[] bundles;
    private Set<String> startMap = new HashSet<String>();

    public BundleForm() {
    }
    
    public static BundleForm resolve(URL formURL) throws IOException, URISyntaxException {
        InputStream in = formURL.openStream();
        try {
            BundleForm f = new BundleForm();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            LinkedList<String> locs = new LinkedList<String>();
            for(;;) {
                String l = r.readLine();
                if ( l == null ) break;
                URI uri = URI.create(l);
                String status = uri.getScheme();
                uri = URI.create(uri.getSchemeSpecificPart());
                String loc = uri.toString();
                locs.add( loc );
                f.setStarted(loc, "start".equalsIgnoreCase(status) );
            }
            f.setBundles(locs.toArray(new String[locs.size()]));
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

    public void setBundles(String[] bundles) {
        this.bundles = bundles;
    }
    
    public String[] getBundles()
    {
        return bundles;
    }

    public boolean isStarted(String url)
    {
        return startMap.contains(url);
    }
    
    public void setStarted(String url, boolean started) {
        if ( started ) {
            startMap.add(url);
        }
        else {
            startMap.remove(url);
        }
    }
    
    public BundleStatus[] toStatus() throws IOException  {
        ArrayList<BundleStatus> ret = new ArrayList<BundleStatus>(bundles.length);
        for ( String loc : bundles ) {
            URL url = new URL(loc);
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
                st.setLocation(loc);
                st.setStatus(isStarted(loc) ? Bundle.ACTIVE : Bundle.INSTALLED);
                ret.add(st);
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
        return ret.toArray(new BundleStatus[ret.size()] );
    }
}
