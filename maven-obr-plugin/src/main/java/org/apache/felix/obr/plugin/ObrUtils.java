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
package org.apache.felix.obr.plugin;


import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.model.Resource;


/**
 * Various OBR utility methods
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrUtils
{
    private static final String REPO_XML = "repository.xml";
    private static final String OBR_XML = "obr.xml";


    /**
     * @param baseDir current base directory
     * @param localRepository path to local maven repository
     * @param obrRepository path to specific repository.xml
     * @return URI pointing to correct repository.xml
     */
    public static URI findRepositoryXml( File baseDir, String localRepository, String obrRepository )
    {
        // Combine location settings into a single repository location
        if ( null == obrRepository || obrRepository.trim().length() == 0 )
        {
            obrRepository = localRepository + '/' + REPO_XML;
        }
        else if ( !obrRepository.endsWith( ".xml" ) )
        {
            obrRepository = obrRepository + '/' + REPO_XML;
        }

        URI uri;
        try
        {
            uri = new URI( obrRepository );
            uri.toURL(); // check protocol
        }
        catch ( Exception e )
        {
            uri = null;
        }

        // fall-back to file-system approach
        if ( null == uri || !uri.isAbsolute() )
        {
            File file = new File( obrRepository );
            if ( !file.isAbsolute() )
            {
                file = new File( baseDir, obrRepository );
            }

            uri = file.toURI();
        }

        return uri;
    }


    /**
     * @param resources collection of resource locations
     * @return URI pointing to correct obr.xml, null if not found
     */
    public static URI findObrXml( Collection resources )
    {
        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = ( Resource ) i.next();
            File obrFile = new File( resource.getDirectory(), OBR_XML );
            if ( obrFile.exists() )
            {
                return obrFile.toURI();
            }
        }
        return null;
    }
}
