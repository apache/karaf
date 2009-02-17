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
package org.apache.maven.shared.osgi;

import org.apache.maven.artifact.Artifact;

/**
 * Converter from Maven groupId,artifactId and versions to OSGi Bundle-SymbolicName and version
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id: Maven2OsgiConverter.java 661727 2008-05-30 14:21:49Z bentmann $
 */
public interface Maven2OsgiConverter
{

    /**
     * Get the OSGi symbolic name for the artifact
     * 
     * @param artifact
     * @return the Bundle-SymbolicName manifest property
     */
    String getBundleSymbolicName( Artifact artifact );

    String getBundleFileName( Artifact artifact );

    /**
     * Convert a Maven version into an OSGi compliant version
     * 
     * @param artifact Maven artifact
     * @return the OSGi version
     */
    String getVersion( Artifact artifact );

    /**
     * Convert a Maven version into an OSGi compliant version
     * 
     * @param version Maven version
     * @return the OSGi version
     */
    String getVersion( String version );

}