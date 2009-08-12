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


import org.apache.felix.metatype.AD;
import org.apache.felix.metatype.internal.l10n.Resources;
import org.osgi.service.metatype.AttributeDefinition;


/**
 * The <code>LocalizedAttributeDefinition</code> class is the implementation
 * of the <code>AttributeDefinition</code> interface. This class delegates
 * calls to the underlying {@link AD} localizing the results of the following
 * methods: {@link #getName()}, {@link #getDescription()},
 * {@link #getOptionLabels()}, and {@link #validate(String)}.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class LocalizedAttributeDefinition extends LocalizedBase implements AttributeDefinition
{

    private final AD ad;


    /**
     * Creates and instance of this localizing facade.
     *
     * @param ad The {@link AD} to which calls are delegated.
     * @param resources The {@link Resources} used to localize return values of
     * localizable methods.
     */
    LocalizedAttributeDefinition( AD ad, Resources resources )
    {
        super( resources );
        this.ad = ad;
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getCardinality()
     */
    public int getCardinality()
    {
        return ad.getCardinality();
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getDefaultValue()
     */
    public String[] getDefaultValue()
    {
        return ad.getDefaultValue();
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getDescription()
     */
    public String getDescription()
    {
        return localize( ad.getDescription() );
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getID()
     */
    public String getID()
    {
        return ad.getID();
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getName()
     */
    public String getName()
    {
        return localize( ad.getName() );
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getOptionLabels()
     */
    public String[] getOptionLabels()
    {
        return localize( ad.getOptionLabels() );
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getOptionValues()
     */
    public String[] getOptionValues()
    {
        return ad.getOptionValues();
    }


    /**
     * @see org.osgi.service.metatype.AttributeDefinition#getType()
     */
    public int getType()
    {
        return ad.getType();
    }


    /**
     * @param value
     * @see org.osgi.service.metatype.AttributeDefinition#validate(java.lang.String)
     */
    public String validate( String value )
    {
        String message = ad.validate( value );
        if ( message == null || message.length() == 0 )
        {
            return message;
        }

        return localize( message );
    }
}
