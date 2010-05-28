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
package org.apache.felix.scr.impl.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public class MetaTypeProviderImpl
    implements MetaTypeProvider, ManagedService
{

    private final int logLevel;

    private final boolean factoryEnabled;

    private final ManagedService delegatee;

    public MetaTypeProviderImpl(final int logLevel,
                                final boolean factoryEnabled,
                                final ManagedService delegatee)
    {
        this.logLevel = logLevel;
        this.factoryEnabled = factoryEnabled;
        this.delegatee = delegatee;
    }

    private ObjectClassDefinition ocd;

    public void updated(Dictionary properties) throws ConfigurationException
    {
        this.delegatee.updated(properties);
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    public String[] getLocales()
    {
        return null;
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String, java.lang.String)
     */
    public ObjectClassDefinition getObjectClassDefinition( String id, String locale )
    {
        if ( !ScrConfiguration.PID.equals( id ) )
        {
            return null;
        }

        if ( ocd == null )
        {
            final ArrayList adList = new ArrayList();

            adList.add( new AttributeDefinitionImpl( ScrConfiguration.PROP_LOGLEVEL, "SCR Log Level",
                    "Allows limiting the amount of logging information sent to the OSGi LogService." +
                    " Supported values are DEBUG, INFO, WARN, and ERROR. Default is ERROR.",
                    AttributeDefinition.INTEGER,
                    new String[] {String.valueOf(this.logLevel)}, 0,
                    new String[] {"Debug", "Information", "Warnings", "Error"},
                    new String[] {"4", "3", "2", "1"} ) );

            adList.add( new AttributeDefinitionImpl( ScrConfiguration.PROP_FACTORY_ENABLED, "Extended Factory Components",
                "Whether or not to enable the support for creating Factory Component instances based on factory configuration." +
                " This is an Apache Felix SCR specific extension, explicitly not supported by the Declarative Services " +
                "specification. Reliance on this feature prevent the component from being used with other Declarative " +
                "Services implementations. The default value is false to disable this feature.",
                this.factoryEnabled ) );

            ocd = new ObjectClassDefinition()
            {

                private final AttributeDefinition[] attrs = ( AttributeDefinition[] ) adList
                    .toArray( new AttributeDefinition[adList.size()] );


                public String getName()
                {
                    return "Apache Felix Declarative Service Implementation";
                }


                public InputStream getIcon( int arg0 )
                {
                    return null;
                }


                public String getID()
                {
                    return ScrConfiguration.PID;
                }


                public String getDescription()
                {
                    return "Configuration for the Apache Felix Declarative Services Implementation." +
                           " This configuration overwrites configuration defined in framework properties of the same names.";
                }


                public AttributeDefinition[] getAttributeDefinitions( int filter )
                {
                    return ( filter == OPTIONAL ) ? null : attrs;
                }
            };
        }

        return ocd;
    }

    class AttributeDefinitionImpl implements AttributeDefinition
    {

        private final String id;
        private final String name;
        private final String description;
        private final int type;
        private final String[] defaultValues;
        private final int cardinality;
        private final String[] optionLabels;
        private final String[] optionValues;


        AttributeDefinitionImpl( final String id, final String name, final String description, final boolean defaultValue )
        {
            this( id, name, description, BOOLEAN, new String[]
                { String.valueOf(defaultValue) }, 0, null, null );
        }

        AttributeDefinitionImpl( final String id, final String name, final String description, final int type,
            final String[] defaultValues, final int cardinality, final String[] optionLabels,
            final String[] optionValues )
        {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.defaultValues = defaultValues;
            this.cardinality = cardinality;
            this.optionLabels = optionLabels;
            this.optionValues = optionValues;
        }


        public int getCardinality()
        {
            return cardinality;
        }


        public String[] getDefaultValue()
        {
            return defaultValues;
        }


        public String getDescription()
        {
            return description;
        }


        public String getID()
        {
            return id;
        }


        public String getName()
        {
            return name;
        }


        public String[] getOptionLabels()
        {
            return optionLabels;
        }


        public String[] getOptionValues()
        {
            return optionValues;
        }


        public int getType()
        {
            return type;
        }


        public String validate( String arg0 )
        {
            return null;
        }
    }
}
