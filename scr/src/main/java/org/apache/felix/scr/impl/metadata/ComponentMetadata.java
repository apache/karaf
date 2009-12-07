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
package org.apache.felix.scr.impl.metadata;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.scr.impl.helper.Logger;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;


/**
 * This class holds the information associated to a component in the descriptor *  */
public class ComponentMetadata
{
    // Configuration required for component activation (since DS 1.1)
    public static final String CONFIGURATION_POLICY_REQUIRE = "require";

    // Configuration not provided to component (since DS 1.1)
    public static final String CONFIGURATION_POLICY_IGNORE = "ignore";

    // Configuration optional (default) (since DS 1.1)
    public static final String CONFIGURATION_POLICY_OPTIONAL = "optional";

    // set of valid configuration policy settings
    private static final Set CONFIGURATION_POLICY_VALID;

    // marker value indicating duplicate implementation class setting
    private static final String IMPLEMENTATION_CLASS_DUPLICATE = "icd";

    // marker value indicating duplicate service setting
    private static final ServiceMetadata SERVICE_DUPLICATE = new ServiceMetadata();

    // the namespace code of the namespace declaring this component, this is
    // one of the XmlHandler.DS_VERSION_* constants
    private final int m_namespaceCode;

    // 112.4.3: A Globally unique component name (required)
    private String m_name;

    // 112.4.3: Controls whether the component is enabled when the bundle is started. (optional, default is true).
    private boolean m_enabled = true;

    // 112.4.3: Factory identified. If set to a non empty string, it indicates that the component is a factory component (optional).
    private String m_factory = null;

    // 112.4.3: Controls whether component configurations must be immediately activated after becoming
    // satisfied or whether activation should be delayed. (optional, default value depends
    // on whether the component has a service element or not).
    private Boolean m_immediate = null;

    // 112.4.4 Implementation Element (required)
    private String m_implementationClassName = null;

    // 112.5.8 activate can be specified (since DS 1.1)
    private String m_activate = null;

    // 112.5.8 whether activate has been specified
    private boolean m_activateDeclared = false;

    // 112.5.12 deactivate can be specified (since DS 1.1)
    private String m_deactivate = null;

    // 112.5.12 whether deactivate has been specified
    private boolean m_deactivateDeclared = false;

    // 112.??.?? modified method (configuration update, since DS 1.1)
    private String m_modified = null;

    // 112.4.3 configuration-policy (since DS 1.1)
    private String m_configurationPolicy = null;

    // Associated properties (0..*)
    private Dictionary m_properties = new Hashtable();

    // List of Property metadata - used while building the meta data
    // while validating the properties contained in the PropertyMetadata
    // instances are copied to the m_properties Dictionary while this
    // list will be cleared
    private List m_propertyMetaData = new ArrayList();

    // Provided services (0..1)
    private ServiceMetadata m_service = null;

    // List of service references, (required services 0..*)
    private List m_references = new ArrayList();

    // Flag that is set once the component is verified (its properties cannot be changed)
    private boolean m_validated = false;

    static
    {
        CONFIGURATION_POLICY_VALID = new TreeSet();
        CONFIGURATION_POLICY_VALID.add( CONFIGURATION_POLICY_IGNORE );
        CONFIGURATION_POLICY_VALID.add( CONFIGURATION_POLICY_OPTIONAL );
        CONFIGURATION_POLICY_VALID.add( CONFIGURATION_POLICY_REQUIRE );
    }


    public ComponentMetadata( int namespaceCode )
    {
        this.m_namespaceCode = namespaceCode;
    }

    /////////////////////////////////////////// SETTERS //////////////////////////////////////

    /**
     * Setter for the name
     *
     * @param name
     */
    public void setName( String name )
    {
        if ( m_validated )
        {
            return;
        }
        m_name = name;
    }


    /**
     * Setter for the enabled property
     *
     * @param enabled
     */
    public void setEnabled( boolean enabled )
    {
        if ( m_validated )
        {
            return;
        }
        m_enabled = enabled;
    }


    /**
     *
     * @param factoryIdentifier
     */
    public void setFactoryIdentifier( String factoryIdentifier )
    {
        if ( m_validated )
        {
            return;
        }
        m_factory = factoryIdentifier;
    }


    /**
     * Setter for the immediate property
     *
     * @param immediate
     */
    public void setImmediate( boolean immediate )
    {
        if ( m_validated )
        {
            return;
        }
        m_immediate = immediate ? Boolean.TRUE : Boolean.FALSE;
    }


    /**
     * Sets the name of the implementation class
     *
     * @param implementationClassName a class name
     */
    public void setImplementationClassName( String implementationClassName )
    {
        if ( m_validated )
        {
            return;
        }

        // set special flag value if implementation class is already set
        if ( m_implementationClassName != null )
        {
            m_implementationClassName = IMPLEMENTATION_CLASS_DUPLICATE;
        }
        else
        {
            m_implementationClassName = implementationClassName;
        }
    }


    /**
     * Sets the configuration policy
     *
     * @param configurationPolicy configuration policy
     * @since 1.2.0 (DS 1.1)
     */
    public void setConfigurationPolicy( String configurationPolicy )
    {
        if ( m_validated )
        {
            return;
        }
        m_configurationPolicy = configurationPolicy;
    }


    /**
     * Sets the name of the activate method
     *
     * @param activate a method name
     * @since 1.2.0 (DS 1.1)
     */
    public void setActivate( String activate )
    {
        if ( m_validated )
        {
            return;
        }
        m_activate = activate;
        m_activateDeclared = true;
    }


    /**
     * Sets the name of the deactivate method
     *
     * @param deactivate a method name
     * @since 1.2.0 (DS 1.1)
     */
    public void setDeactivate( String deactivate )
    {
        if ( m_validated )
        {
            return;
        }
        m_deactivate = deactivate;
        m_deactivateDeclared = true;
    }


    /**
     * Sets the name of the modified method
     *
     * @param modified a method name
     * @since 1.2.0 (DS 1.1)
     */
    public void setModified( String modified )
    {
        if ( m_validated )
        {
            return;
        }
        m_modified = modified;
    }


    /**
     * Used to add a property to the instance
     *
     * @param newProperty a property metadata object
     */
    public void addProperty( PropertyMetadata newProperty )
    {
        if ( m_validated )
        {
            return;
        }
        if ( newProperty == null )
        {
            throw new IllegalArgumentException( "Cannot add a null property" );
        }
        m_propertyMetaData.add( newProperty );
    }


    /**
     * Used to set a ServiceMetadata object.
     *
     * @param service a ServiceMetadata
     */
    public void setService( ServiceMetadata service )
    {
        if ( m_validated )
        {
            return;
        }

        // set special flag value if implementation class is already set
        if ( m_service != null )
        {
            m_service = SERVICE_DUPLICATE;
        }
        else
        {
            m_service = service;
        }
    }


    /**
     * Used to add a reference metadata to the component
     *
     * @param newReference a new ReferenceMetadata to be added
     */
    public void addDependency( ReferenceMetadata newReference )
    {
        if ( m_validated )
        {
            return;
        }
        if ( newReference == null )
        {
            throw new IllegalArgumentException( "Cannot add a null ReferenceMetadata" );
        }
        m_references.add( newReference );
    }


    /////////////////////////////////////////// GETTERS //////////////////////////////////////

    /**
     * Returns the namespace code of the namespace of the component element
     * declaring this component. This is one of the XmlHandler.DS_VERSION_*
     * constants.
     */
    public int getNamespaceCode()
    {
        return m_namespaceCode;
    }


    /**
     * Returns <code>true</code> if the metadata declaration has used the
     * Declarative Services version 1.1 namespace or a later namespace.
     */
    public boolean isDS11()
    {
        return getNamespaceCode() >= XmlHandler.DS_VERSION_1_1;
    }


    /**
     * Returns <code>true</code> if the metadata declaration has used the
     * Declarative Services version 1.1-felixnamespace or a later namespace.
     *
     * @see <a href="https://issues.apache.org/jira/browse/FELIX-1893">FELIX-1893</a>
     */
    public boolean isDS11Felix()
    {
        return getNamespaceCode() >= XmlHandler.DS_VERSION_1_1_FELIX;
    }


    /**
     * Returns the name of the component
     *
     * @return A string containing the name of the component
     */
    public String getName()
    {
        return m_name;
    }


    /**
     * Returns the value of the enabled flag
     *
     * @return a boolean containing the value of the enabled flag
     */
    public boolean isEnabled()
    {
        return m_enabled;
    }


    /**
     * Returns the factory identifier
     *
     * @return A string containing a factory identifier or null
     */
    public String getFactoryIdentifier()
    {
        return m_factory;
    }


    /**
     * Returns the flag that defines the activation policy for the component.
     * <p>
     * This method may only be trusted after this instance has been validated
     * by the {@link #validate()} call. Else it will either return the value
     * of an explicitly set "immediate" attribute or return false if a service
     * element or the factory attribute is set or true otherwise. This latter
     * default value deduction may be unsafe while the descriptor has not been
     * completely read.
     *
     * @return a boolean that defines the activation policy
     */
    public boolean isImmediate()
    {
        // return explicit value if known
        if ( m_immediate != null )
        {
            return m_immediate.booleanValue();
        }

        // deduce default from service element and factory attribute presence
        return m_service == null && m_factory == null;
    }


    /**
     * Returns the name of the implementation class
     *
     * @return the name of the implementation class
     */
    public String getImplementationClassName()
    {
        return m_implementationClassName;
    }


    /**
     * Returns the configuration Policy
     *
     * @return the configuration policy
     * @since 1.2.0 (DS 1.1)
     */
    public String getConfigurationPolicy()
    {
        return m_configurationPolicy;
    }


    /**
     * Returns the name of the activate method
     *
     * @return the name of the activate method
     * @since 1.2.0 (DS 1.1)
     */
    public String getActivate()
    {
        return m_activate;
    }


    /**
     * Returns whether the activate method has been declared in the descriptor
     * or not.
     *
     * @return whether the activate method has been declared in the descriptor
     *      or not.
     * @since 1.2.0 (DS 1.1)
     */
    public boolean isActivateDeclared()
    {
        return m_activateDeclared;
    }


    /**
     * Returns the name of the deactivate method
     *
     * @return the name of the deactivate method
     * @since 1.2.0 (DS 1.1)
     */
    public String getDeactivate()
    {
        return m_deactivate;
    }


    /**
     * Returns whether the deactivate method has been declared in the descriptor
     * or not.
     *
     * @return whether the deactivate method has been declared in the descriptor
     *      or not.
     * @since 1.2.0 (DS 1.1)
     */
    public boolean isDeactivateDeclared()
    {
        return m_deactivateDeclared;
    }


    /**
     * Returns the name of the modified method
     *
     * @return the name of the modified method
     * @since 1.2.0 (DS 1.1)
     */
    public String getModified()
    {
        return m_modified;
    }


    /**
     * Returns the associated ServiceMetadata
     *
     * @return a ServiceMetadata object or null if the Component does not provide any service
     */
    public ServiceMetadata getServiceMetadata()
    {
        return m_service;
    }


    /**
     * Returns the properties.
     *
     * @return the properties as a Dictionary
     */
    public Dictionary getProperties()
    {
        return m_properties;
    }


    /**
     * Returns the list of property meta data.
     * <b>Note: This method is intended for unit testing only</b>
     *
     * @return the list of property meta data.
     */
    List getPropertyMetaData()
    {
        return m_propertyMetaData;
    }


    /**
     * Returns the dependency descriptors
     *
     * @return a Collection of dependency descriptors
     */
    public List getDependencies()
    {
        return m_references;
    }


    /**
     * Test to see if this service is a factory
     *
     * @return true if it is a factory, false otherwise
     */
    public boolean isFactory()
    {
        return m_factory != null;
    }


    /**
     * Returns <code>true</code> if the configuration policy is configured to
     * {@link #CONFIGURATION_POLICY_REQUIRE}.
     */
    public boolean isConfigurationRequired()
    {
        return CONFIGURATION_POLICY_REQUIRE.equals( m_configurationPolicy );
    }


    /**
     * Returns <code>true</code> if the configuration policy is configured to
     * {@link #CONFIGURATION_POLICY_IGNORE}.
     */
    public boolean isConfigurationIgnored()
    {
        return CONFIGURATION_POLICY_IGNORE.equals( m_configurationPolicy );
    }


    /**
     * Returns <code>true</code> if the configuration policy is configured to
     * {@link #CONFIGURATION_POLICY_OPTIONAL}.
     */
    public boolean isConfigurationOptional()
    {
        return CONFIGURATION_POLICY_OPTIONAL.equals( m_configurationPolicy );
    }


    /**
     * Method used to verify if the semantics of this metadata are correct
     */
    public void validate( Logger logger )
    {
        // nothing to do if already validated
        if ( m_validated )
        {
            return;
        }

        // 112.10 The name of the component is required
        if ( m_name == null )
        {
            // 112.4.3 name is optional defaulting to implementation class name since DS 1.1
            if ( m_namespaceCode < XmlHandler.DS_VERSION_1_1 )
            {
                throw new ComponentException( "The component name has not been set" );
            }
            setName( getImplementationClassName() );
        }

        // 112.10 There must be one implementation element and the class atribute is required
        if ( m_implementationClassName == null )
        {
            throw validationFailure( "Implementation class name missing" );
        }
        else if ( m_implementationClassName == IMPLEMENTATION_CLASS_DUPLICATE )
        {
            throw validationFailure( "Implementation element must occur exactly once" );
        }

        // 112.4.3 configuration-policy (since DS 1.1)
        if ( m_configurationPolicy == null )
        {
            // default if not specified or pre DS 1.1
            m_configurationPolicy = CONFIGURATION_POLICY_OPTIONAL;
        }
        else if ( m_namespaceCode < XmlHandler.DS_VERSION_1_1 )
        {
            logger.log( LogService.LOG_WARNING, "Ignoring configuration policy, DS 1.1 or later namespace required",
                this, null );
            m_configurationPolicy = CONFIGURATION_POLICY_OPTIONAL;
        }
        else if ( !CONFIGURATION_POLICY_VALID.contains( m_configurationPolicy ) )
        {
            throw validationFailure( "Configuration policy must be one of " + CONFIGURATION_POLICY_VALID );
        }

        // 112.5.8 activate can be specified (since DS 1.1)
        if ( m_activate == null )
        {
            // default if not specified or pre DS 1.1
            m_activate = "activate";
        }
        else if ( m_namespaceCode < XmlHandler.DS_VERSION_1_1 )
        {
            // DS 1.0 cannot declare the activate method, assume default and undeclared
            logger.log( LogService.LOG_WARNING,
                "Ignoring activate method declaration, DS 1.1 or later namespace required", this, null );
            m_activate = "activate";
            m_activateDeclared = false;
        }

        // 112.5.12 deactivate can be specified (since DS 1.1)
        if ( m_deactivate == null )
        {
            // default if not specified or pre DS 1.1
            m_deactivate = "deactivate";
        }
        else if ( m_namespaceCode < XmlHandler.DS_VERSION_1_1 )
        {
            // DS 1.0 cannot declare the deactivate method, assume default and undeclared
            logger.log( LogService.LOG_WARNING,
                "Ignoring deactivate method declaration, DS 1.1 or later namespace required", this, null );
            m_deactivate = "deactivate";
            m_deactivateDeclared = false;
        }

        // 112.??.?? modified can be specified (since DS 1.1)
        if ( m_modified != null && m_namespaceCode < XmlHandler.DS_VERSION_1_1 )
        {
            // require new namespace if modified is specified
            logger.log( LogService.LOG_WARNING,
                "Ignoring modified method declaration, DS 1.1 or later namespace required", this, null );
            m_modified = null;
        }

        // Next check if the properties are valid (and extract property values)
        Iterator propertyIterator = m_propertyMetaData.iterator();
        while ( propertyIterator.hasNext() )
        {
            PropertyMetadata propMeta = ( PropertyMetadata ) propertyIterator.next();
            propMeta.validate( this );
            m_properties.put( propMeta.getName(), propMeta.getValue() );
        }
        m_propertyMetaData.clear();

        // Check that the provided services are valid too
        if ( m_service == SERVICE_DUPLICATE )
        {
            throw validationFailure( "Service element must occur at most once" );
        }
        else if ( m_service != null )
        {
            m_service.validate( this );
        }

        // Check that the references are ok
        HashSet refs = new HashSet();
        Iterator referenceIterator = m_references.iterator();
        while ( referenceIterator.hasNext() )
        {
            ReferenceMetadata refMeta = ( ReferenceMetadata ) referenceIterator.next();
            refMeta.validate( this, logger );

            // flag duplicates
            if ( !refs.add( refMeta.getName() ) )
            {
                logger.log( LogService.LOG_WARNING, "Detected duplicate reference name: ''{0}''", new Object[]
                    { refMeta.getName() }, this, null );
            }
        }

        // verify value of immediate attribute if set
        if ( m_immediate != null )
        {
            if ( isImmediate() )
            {
                // FELIX-593: 112.4.3 clarification, immediate is false for factory
                if ( isFactory() )
                {
                    throw validationFailure( "Factory cannot be immediate" );
                }
            }
            else
            {
                // 112.2.3 A delayed component specifies a service, is not specified to be a factory component
                // and does not have the immediate attribute of the component element set to true.
                // FELIX-593: 112.4.3 clarification, immediate may be true for factory
                if ( m_service == null && !isFactory() )
                {
                    throw validationFailure( "Delayed must provide a service or be a factory" );
                }
            }
        }

        // 112.4.6 The serviceFactory attribute (of a provided service) must not be true if
        // the component is a factory component or an immediate component
        if ( m_service != null )
        {
            if ( m_service.isServiceFactory() && ( isFactory() || isImmediate() ) )
            {
                throw validationFailure( "ServiceFactory cannot be factory or immediate" );
            }
        }

        m_validated = true;
        // TODO: put a similar flag on the references and the services
    }


    /**
     * Returns a <code>ComponentException</code> for this compeonent with the
     * given explanation for failure.
     *
     * @param reason The explanation for failing to validate this component.
     */
    ComponentException validationFailure( String reason )
    {
        return new ComponentException( "Component " + getName() + " validation failed: " + reason );
    }
}
