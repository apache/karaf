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
package org.apache.felix.metatype.internal;


import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.xmlpull.v1.XmlPullParserException;


/**
 * The <code>MetaTypeServiceImpl</code> class is the implementation of the
 * <code>MetaTypeService</code> interface of the OSGi Metatype Service
 * Specification 1.1.  
 *
 * @author fmeschbe
 */
class MetaTypeServiceImpl implements MetaTypeService
{

    /** The <code>BundleContext</code> of the providing this service. */
    private final BundleContext bundleContext;


    /**
     * Creates an instance of this class.
     * 
     * @param bundleContext The <code>BundleContext</code> ultimately used to
     *      access services if there are no meta type documents. 
     */
    MetaTypeServiceImpl( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }


    /**
     * Looks for meta type documents in the given <code>bundle</code>. If no
     * such documents exist, a <code>MetaTypeInformation</code> object is
     * returned handling the services of the bundle.
     * <p>
     * According to the specification, the services of the bundle are ignored
     * if at least one meta type document exists.
     * 
     * @param bundle The <code>Bundle</code> for which a
     *      <code>MetaTypeInformation</code> is to be returned.
     */
    public MetaTypeInformation getMetaTypeInformation( Bundle bundle )
    {
        MetaTypeInformation mti = fromDocuments( bundle );
        if ( mti != null )
        {
            return mti;
        }

        return new ServiceMetaTypeInformation( bundleContext, bundle );
    }


    private MetaTypeInformation fromDocuments( Bundle bundle )
    {
        MetaDataReader reader = new MetaDataReader();

        // get the descriptors, return nothing if none
        Enumeration docs = bundle.findEntries( METATYPE_DOCUMENTS_LOCATION, "*.xml", false );
        if ( docs == null || !docs.hasMoreElements() )
        {
            return null;
        }

        MetaTypeInformationImpl cmti = new MetaTypeInformationImpl( bundle );
        while ( docs.hasMoreElements() )
        {
            URL doc = ( URL ) docs.nextElement();
            try
            {
                MetaData metaData = reader.parse( doc );
                if (metaData != null) {
                    cmti.addMetaData( metaData );
                }
            }
            catch ( XmlPullParserException xppe )
            {
                Activator.log( LogService.LOG_ERROR, "fromDocuments: Error parsing document " + doc, xppe );
            }
            catch ( IOException ioe )
            {
                Activator.log( LogService.LOG_ERROR, "fromDocuments: Error accessing document " + doc, ioe );
            }
        }
        return cmti;
    }
}
