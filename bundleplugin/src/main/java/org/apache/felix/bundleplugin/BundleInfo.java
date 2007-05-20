/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.bundleplugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * Information result of the bundling process 
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class BundleInfo
{

    /**
     * {@link Map} &lt; {@link String}, {@link Set} &lt; {@link Artifact} > >
     * Used to check for duplicated exports. Key is package name and value list of artifacts where it's exported.
     */
    private Map exportedPackages = new HashMap();

    public void addExportedPackage( String packageName, Artifact artifact )
    {
        Set artifacts = (Set) getExportedPackages().get( packageName );
        if ( artifacts == null )
        {
            artifacts = new HashSet();
            exportedPackages.put( packageName, artifacts );
        }
        artifacts.add( artifact );
    }

    Map getExportedPackages()
    {
        return exportedPackages;
    }

    /**
     * Get a list of packages that are exported in more than one bundle.
     * Key is package name and value list of artifacts where it's exported.
     * @return {@link Map} &lt; {@link String}, {@link Set} &lt; {@link Artifact} > >
     */
    public Map getDuplicatedExports()
    {
        Map duplicatedExports = new HashMap();

        for ( Iterator it = getExportedPackages().entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            Set artifacts = (Set) entry.getValue();
            if ( artifacts.size() > 1 )
            {
                /* remove warnings caused by different versions of same artifact */
                Set artifactKeys = new HashSet();

                String packageName = (String) entry.getKey();
                for ( Iterator it2 = artifacts.iterator(); it2.hasNext(); )
                {
                    Artifact artifact = (Artifact) it2.next();
                    artifactKeys.add( artifact.getGroupId() + "." + artifact.getArtifactId() );
                }

                if ( artifactKeys.size() > 1 )
                {
                    duplicatedExports.put( packageName, artifacts );
                }
            }
        }

        return duplicatedExports;
    }

    public void merge( BundleInfo bundleInfo )
    {
        for ( Iterator it = bundleInfo.getExportedPackages().entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            String packageName = (String) entry.getKey();
            Collection artifacts = (Collection) entry.getValue();

            Collection artifactsWithPackage = (Collection) getExportedPackages().get( packageName );
            if ( artifactsWithPackage == null )
            {
                artifactsWithPackage = new HashSet();
                getExportedPackages().put( packageName, artifactsWithPackage );
            }
            artifactsWithPackage.addAll( artifacts );
        }
    }
}
