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
package org.apache.felix.bundleplugin;


import java.util.Collection;
import java.util.HashSet;

import org.apache.maven.plugin.MojoExecutionException;

import aQute.libg.header.OSGiHeader;


/**
 * Exclude selected dependencies from the classpath passed to BND.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class DependencyExcluder extends AbstractDependencyFilter
{
    /**
     * Excluded artifacts.
     */
    private final Collection m_excludedArtifacts;


    public DependencyExcluder( Collection dependencyArtifacts )
    {
        super( dependencyArtifacts );

        m_excludedArtifacts = new HashSet();
    }


    public void processHeaders( String excludeDependencies ) throws MojoExecutionException
    {
        m_excludedArtifacts.clear();

        if ( null != excludeDependencies && excludeDependencies.length() > 0 )
        {
            processInstructions( OSGiHeader.parseHeader( excludeDependencies ) );
        }
    }


    @Override
    protected void processDependencies( String tag, String inline, Collection dependencies )
    {
        m_excludedArtifacts.addAll( dependencies );
    }


    public Collection getExcludedArtifacts()
    {
        return m_excludedArtifacts;
    }
}
