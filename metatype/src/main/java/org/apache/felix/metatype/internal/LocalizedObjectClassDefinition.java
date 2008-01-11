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
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.apache.felix.metatype.AD;
import org.apache.felix.metatype.OCD;
import org.apache.felix.metatype.internal.l10n.Resources;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>LocalizedObjectClassDefinition</code> class is the implementation
 * of the <code>ObjectClassDefinition</code> interface. This class delegates
 * calls to the underlying {@link OCD} localizing the results of the following
 * methods: {@link #getName()}, {@link #getDescription()}, and
 * {@link #getIcon(int)}.
 *
 * @author fmeschbe
 */
public class LocalizedObjectClassDefinition extends LocalizedBase implements ObjectClassDefinition
{

    private Bundle bundle;

    private OCD ocd;


    /**
     * Creates and instance of this localizing facade.
     *
     * @param bundle The <code>Bundle</code> providing this object class
     *            definition.
     * @param ocd The {@link OCD} to which calls are delegated.
     * @param resources The {@link Resources} used to localize return values of
     * localizable methods.
     */
    public LocalizedObjectClassDefinition( Bundle bundle, OCD ocd, Resources resources )
    {
        super( resources );
        this.bundle = bundle;
        this.ocd = ocd;
    }


    /**
     * @param filter
     * @see org.osgi.service.metatype.ObjectClassDefinition#getAttributeDefinitions(int)
     */
    public AttributeDefinition[] getAttributeDefinitions( int filter )
    {
        if ( ocd.getAttributeDefinitions() == null )
        {
            return null;
        }

        Iterator adhIter = ocd.getAttributeDefinitions().values().iterator();
        if ( filter == ObjectClassDefinition.OPTIONAL || filter == ObjectClassDefinition.REQUIRED )
        {
            boolean required = ( filter == ObjectClassDefinition.REQUIRED );
            adhIter = new RequiredFilterIterator( adhIter, required );
        }
        else if ( filter != ObjectClassDefinition.ALL )
        {
            return null;
        }

        if ( !adhIter.hasNext() )
        {
            return null;
        }

        List result = new ArrayList();
        while ( adhIter.hasNext() )
        {
            result.add( new LocalizedAttributeDefinition( ( AD ) adhIter.next(), getResources() ) );
        }

        return ( AttributeDefinition[] ) result.toArray( new AttributeDefinition[result.size()] );
    }


    /**
     * @see org.osgi.service.metatype.ObjectClassDefinition#getDescription()
     */
    public String getDescription()
    {
        return localize( ocd.getDescription() );
    }


    /**
     * @param size
     * @throws IOException
     * @see org.osgi.service.metatype.ObjectClassDefinition#getIcon(int)
     */
    public InputStream getIcon( int size ) throws IOException
    {
        // nothing if no icons are defined
        Map icons = ocd.getIcons();
        if ( icons == null )
        {
            return null;
        }

        // get exact size
        String iconPath = ( String ) icons.get( new Integer( size ) );
        if ( iconPath == null )
        {
            // approximate size: largest icon smaller than requested
            Integer selected = new Integer( Integer.MIN_VALUE );
            for ( Iterator ei = icons.keySet().iterator(); ei.hasNext(); )
            {
                Map.Entry entry = ( Map.Entry ) ei.next();
                Integer keySize = ( Integer ) entry.getKey();
                if ( keySize.intValue() <= size && selected.compareTo( keySize ) < 0 )
                {
                    selected = keySize;
                }
            }
            // get the raw path, fail if no path can be found
            iconPath = ( String ) icons.get( selected );
        }

        // fail if no icon could be found
        if ( iconPath == null )
        {
            return null;
        }

        // localize the path
        iconPath = localize( iconPath );

        // try to resolve the path in the bundle
        URL url = bundle.getEntry( iconPath );
        if ( url == null )
        {
            return null;
        }

        // open the stream on the URL - this may throw an IOException
        return url.openStream();
    }


    /**
     * @see org.osgi.service.metatype.ObjectClassDefinition#getID()
     */
    public String getID()
    {
        return ocd.getID();
    }


    /**
     * @see org.osgi.service.metatype.ObjectClassDefinition#getName()
     */
    public String getName()
    {
        return localize( ocd.getName() );
    }

    private static class RequiredFilterIterator implements Iterator
    {

        private final Iterator base;

        private final boolean required;

        private AD next;


        private RequiredFilterIterator( Iterator base, boolean required )
        {
            this.base = base;
            this.required = required;
            this.next = seek();
        }


        public boolean hasNext()
        {
            return next != null;
        }


        public Object next()
        {
            if ( !hasNext() )
            {
                throw new NoSuchElementException();
            }

            AD toReturn = next;
            next = seek();
            return toReturn;
        }


        public void remove()
        {
            throw new UnsupportedOperationException( "remove" );
        }


        private AD seek()
        {
            if ( base.hasNext() )
            {
                AD next;
                do
                {
                    next = ( AD ) base.next();
                }
                while ( next.isRequired() != required && base.hasNext() );

                if ( next.isRequired() == required )
                {
                    return next;
                }
            }

            // nothing found any more
            return null;
        }

    }
}
