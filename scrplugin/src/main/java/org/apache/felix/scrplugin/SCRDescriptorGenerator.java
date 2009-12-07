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
package org.apache.felix.scrplugin;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scrplugin.helper.IssueLog;
import org.apache.felix.scrplugin.helper.PropertyHandler;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.om.Implementation;
import org.apache.felix.scrplugin.om.Interface;
import org.apache.felix.scrplugin.om.Property;
import org.apache.felix.scrplugin.om.Reference;
import org.apache.felix.scrplugin.om.Service;
import org.apache.felix.scrplugin.om.metatype.Designate;
import org.apache.felix.scrplugin.om.metatype.MTObject;
import org.apache.felix.scrplugin.om.metatype.MetaData;
import org.apache.felix.scrplugin.om.metatype.OCD;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaClassDescriptionInheritanceComparator;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.ModifiableJavaClassDescription;
import org.apache.felix.scrplugin.tags.qdox.QDoxJavaClassDescription;
import org.apache.felix.scrplugin.xml.ComponentDescriptorIO;
import org.apache.felix.scrplugin.xml.MetaTypeIO;
import org.osgi.service.metatype.MetaTypeService;


/**
 * The <code>SCRDescriptorGenerator</code> class does the hard work of
 * generating the SCR descriptors. This class is being instantiated and
 * configured by clients and the {@link #execute()} method called to generate
 * the descriptor files.
 * <p>
 * When using this class carefully consider calling <i>all</i> setter methods
 * to properly configure the generator. All setter method document, which
 * default value is assumed for the respective property if the setter is
 * not called.
 * <p>
 * Instances of this class are not thread save and should not be reused.
 */
public class SCRDescriptorGenerator
{

    private final Log logger;

    private File outputDirectory = null;

    private JavaClassDescriptorManager descriptorManager;

    private String finalName = "serviceComponents.xml";

    private String metaTypeName = "metatype.xml";

    private boolean generateAccessors = true;

    protected boolean strictMode = false;

    private Map<String, String> properties = new HashMap<String, String>();

    private String specVersion = null;


    /**
     * Create an instance of this generator using the given {@link Log}
     * instance of logging.
     */
    public SCRDescriptorGenerator( Log logger )
    {
        this.logger = logger;
    }


    /**
     * Sets the directory where the descriptor files will be created.
     * <p>
     * This field has no default value and this setter <b>must</b> called prior
     * to calling {@link #execute()}.
     */
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }


    /**
     * Sets the {@link JavaClassDescriptorManager} instance used to access
     * existing descriptors and to parse JavaDoc tags as well as interpret
     * the SCR annotations.
     * <p>
     * This field has no default value and this setter <b>must</b> called prior
     * to calling {@link #execute()}.
     */
    public void setDescriptorManager( JavaClassDescriptorManager descriptorManager )
    {
        this.descriptorManager = descriptorManager;
    }


    /**
     * Sets the name of the SCR declaration descriptor file. This file will be
     * created in the <i>OSGI-INF</i> directory below the
     * {@link #setOutputDirectory(File) output directory}.
     * <p>
     * This file will be overwritten if already existing. If no descriptors
     * are created the file is actually removed.
     * <p>
     * The default value of this property is <code>serviceComponents.xml</code>.
     */
    public void setFinalName( String finalName )
    {
        this.finalName = finalName;
    }


    /**
     * Sets the name of the file taking the Metatype Service descriptors. This
     * file will be created in the <i>OSGI-INF/metatype</i> directory below the
     * {@link #setOutputDirectory(File) output directory}.
     * <p>
     * This file will be overwritten if already existing. If no descriptors
     * are created the file is actually removed.
     * <p>
     * The default value of this property is <code>metatype.xml</code>.
     */
    public void setMetaTypeName( String metaTypeName )
    {
        this.metaTypeName = metaTypeName;
    }


    /**
     * Defines whether bind and unbind methods are automatically created by
     * the SCR descriptor generator.
     * <p>
     * The generator uses the ASM library to create the method byte codes
     * directly inside the class files. If bind and unbind methods are not
     * to be created, the generator fails if such methods are missing.
     * <p>
     * The default value of this property is <code>true</code>.
     */
    public void setGenerateAccessors( boolean generateAccessors )
    {
        this.generateAccessors = generateAccessors;
    }


    /**
     * Defines whether warnings should be considered as errors and thus cause
     * the generation process to fail.
     * <p>
     * The default value of this property is <code>false</code>.
     */
    public void setStrictMode( boolean strictMode )
    {
        this.strictMode = strictMode;
    }


    /**
     * Sets global properties to be set for each descriptor. If a descriptor
     * provides properties of the same name, the descriptor properties are preferred
     * over the properties provided here.
     * <p>
     * The are no default global properties.
     */
    public void setProperties( Map<String, String> properties )
    {
        this.properties = new HashMap<String, String>( properties );
    }


    /**
     * Sets the Declarative Services specification version number to be forced
     * on the declarations.
     * <p>
     * Supported values for this property are <code>null</code> to autodetect
     * the specification version, <code>1.0</code> to force 1.0 descriptors and
     * <code>1.1</code> to force 1.1 descriptors. If 1.0 descriptors are forced
     * the generation fails if a descriptor requires 1.1 functionality.
     * <p>
     * The default is to generate the descriptor version according to the
     * capabilities used by the descriptors. If no 1.1 capabilities, such as
     * <code>configuration-policy</code>, are used, version 1.0 is used,
     * otherwise a 1.1 descriptor is generated.
     */
    public void setSpecVersion( String specVersion )
    {
        this.specVersion = specVersion;
    }


    /**
     * Actually generates the Declarative Services and Metatype descriptors
     * scanning the java sources provided by the
     * {@link #setDescriptorManager(JavaClassDescriptorManager) descriptor manager}.
     *
     * @return <code>true</code> if descriptors have been generated.
     *
     * @throws SCRDescriptorException
     * @throws SCRDescriptorFailureException
     */
    public boolean execute() throws SCRDescriptorException, SCRDescriptorFailureException
    {
        this.logger.debug( "Starting SCRDescriptorMojo...." );
        this.logger.debug( "..generating accessors: " + this.generateAccessors );
        this.logger.debug( "..parsing javadocs: " + this.descriptorManager.isParseJavadocs() );
        this.logger.debug( "..processing annotations: " + this.descriptorManager.isProcessAnnotations() );

        // check speck version configuration
        int specVersion = toSpecVersionCode( this.specVersion, null );
        if ( this.specVersion == null )
        {
            this.logger.debug( "..auto detecting spec version" );
        }
        else
        {
            this.logger.debug( "..using spec version " + this.specVersion + " (" + specVersion + ")" );
        }

        final IssueLog iLog = new IssueLog( this.strictMode );

        final MetaData metaData = new MetaData();
        metaData.setLocalization( MetaTypeService.METATYPE_DOCUMENTS_LOCATION + "/metatype" );

        // iterate through all source classes and check for component tag
        final JavaClassDescription[] javaSources = descriptorManager.getSourceDescriptions();
        Arrays.sort( javaSources, new JavaClassDescriptionInheritanceComparator() );

        final List<Component> scannedComponents = new ArrayList<Component>();
        for ( int i = 0; i < javaSources.length; i++ )
        {
            this.logger.debug( "Testing source " + javaSources[i].getName() );
            final JavaTag tag = javaSources[i].getTagByName( Constants.COMPONENT );
            if ( tag != null )
            {
                this.logger.debug( "Processing service class " + javaSources[i].getName() );
                // check if there is more than one component tag!
                if ( javaSources[i].getTagsByName( Constants.COMPONENT, false ).length > 1 )
                {
                    iLog.addError( "Class " + javaSources[i].getName() + " has more than one " + Constants.COMPONENT
                        + " tag." + " Merge the tags to a single tag.", tag.getSourceLocation(), tag.getLineNumber() );
                }
                else
                {
                    try
                    {
                        final Component comp = this.createComponent( javaSources[i], tag, metaData, iLog );
                        if ( comp.getSpecVersion() > specVersion )
                        {
                            // if a spec version has been configured and a component requires a higher
                            // version, this is considered an error!
                            if ( this.specVersion != null )
                            {
                                String v = Constants.COMPONENT_DS_SPEC_VERSION_10;
                                if ( comp.getSpecVersion() == Constants.VERSION_1_1 )
                                {
                                    v = Constants.COMPONENT_DS_SPEC_VERSION_11;
                                }
                                iLog.addError( "Component " + comp + " requires spec version " + v
                                    + " but plugin is configured to use version " + this.specVersion, tag
                                    .getSourceLocation(), tag.getLineNumber() );
                            }
                            specVersion = comp.getSpecVersion();
                        }
                        scannedComponents.add( comp );
                    }
                    catch ( SCRDescriptorException sde )
                    {
                        iLog.addError( sde.getMessage(), sde.getSourceLocation(), sde.getLineNumber() );
                    }
                }
            }
        }
        this.logger.debug( "..generating descriptor for spec version: " + this.specVersion );

        // now check for abstract components and fill components objects
        final Components components = new Components();
        final Components abstractComponents = new Components();
        components.setSpecVersion( specVersion );
        abstractComponents.setSpecVersion( specVersion );

        for ( final Component comp : scannedComponents )
        {
            final int errorCount = iLog.getNumberOfErrors();
            // before we can validate we should check the references for bind/unbind method
            // in order to create them if possible

            for ( final Reference ref : comp.getReferences() )
            {
                // if this is a field with a single cardinality,
                // we look for the bind/unbind methods
                // and create them if they are not availabe
                if ( this.generateAccessors && !ref.isLookupStrategy() )
                {
                    if ( ref.getJavaTag().getField() != null
                        && comp.getJavaClassDescription() instanceof ModifiableJavaClassDescription )
                    {
                        if ( ref.getCardinality().equals( "0..1" ) || ref.getCardinality().equals( "1..1" ) )
                        {
                            final String bindValue = ref.getBind();
                            final String unbindValue = ref.getUnbind();
                            final String name = ref.getName();
                            final String type = ref.getInterfacename();

                            boolean createBind = false;
                            boolean createUnbind = false;
                            // Only create method if no bind name has been specified
                            if ( bindValue == null && ref.findMethod( specVersion, "bind" ) == null )
                            {
                                // create bind method
                                createBind = true;
                            }
                            if ( unbindValue == null && ref.findMethod( specVersion, "unbind" ) == null )
                            {
                                // create unbind method
                                createUnbind = true;
                            }
                            if ( createBind || createUnbind )
                            {
                                ( ( ModifiableJavaClassDescription ) comp.getJavaClassDescription() ).addMethods( name,
                                    type, createBind, createUnbind );
                            }
                        }
                    }
                }
            }
            comp.validate( specVersion, iLog );
            // ignore component if it has errors
            if ( iLog.getNumberOfErrors() == errorCount )
            {
                if ( !comp.isDs() )
                {
                    logger.debug( "Ignoring descriptor " + comp );
                }
                else if ( comp.isAbstract() )
                {
                    this.logger.debug( "Adding abstract descriptor " + comp );
                    abstractComponents.addComponent( comp );
                }
                else
                {
                    this.logger.debug( "Adding descriptor " + comp );
                    components.addComponent( comp );
                    abstractComponents.addComponent( comp );
                }
            }
        }

        // log issues
        iLog.logMessages( logger );

        // after checking all classes, throw if there were any failures
        if ( iLog.hasErrors() )
        {
            throw new SCRDescriptorFailureException( "SCR Descriptor parsing had failures (see log)" );
        }

        boolean addResources = false;
        // write meta type info if there is a file name
        if ( !StringUtils.isEmpty( this.metaTypeName ) )
        {
            File mtFile = new File( this.outputDirectory, "OSGI-INF" + File.separator + "metatype" + File.separator
                + this.metaTypeName );
            final int size = metaData.getOCDs().size() + metaData.getDesignates().size();
            if ( size > 0 )
            {
                this.logger.info( "Generating " + size + " MetaType Descriptors to " + mtFile );
                mtFile.getParentFile().mkdirs();
                MetaTypeIO.write( metaData, mtFile );
                addResources = true;
            }
            else
            {
                if ( mtFile.exists() )
                {
                    mtFile.delete();
                }
            }

        }
        else
        {
            this.logger.info( "Meta type file name is not set: meta type info is not written." );
        }

        // if we have descriptors, write them in our scr private file (for component inheritance)
        final File adFile = new File( this.outputDirectory, Constants.ABSTRACT_DESCRIPTOR_RELATIVE_PATH );
        if ( !abstractComponents.getComponents().isEmpty() )
        {
            this.logger.info( "Writing abstract service descriptor " + adFile + " with "
                + abstractComponents.getComponents().size() + " entries." );
            adFile.getParentFile().mkdirs();
            ComponentDescriptorIO.write( abstractComponents, adFile, true );
            addResources = true;
        }
        else
        {
            this.logger.debug( "No abstract SCR Descriptors found in project." );
            // remove file
            if ( adFile.exists() )
            {
                this.logger.debug( "Removing obsolete abstract service descriptor " + adFile );
                adFile.delete();
            }
        }

        // check descriptor file
        final File descriptorFile = StringUtils.isEmpty( this.finalName ) ? null : new File( new File(
            this.outputDirectory, "OSGI-INF" ), this.finalName );

        // terminate if there is nothing else to write
        if ( components.getComponents().isEmpty() )
        {
            this.logger.debug( "No SCR Descriptors found in project." );
            // remove file if it exists
            if ( descriptorFile != null && descriptorFile.exists() )
            {
                this.logger.debug( "Removing obsolete service descriptor " + descriptorFile );
                descriptorFile.delete();
            }
        }
        else
        {
            if ( descriptorFile == null )
            {
                throw new SCRDescriptorFailureException( "Descriptor file name must not be empty." );
            }

            // finally the descriptors have to be written ....
            descriptorFile.getParentFile().mkdirs(); // ensure parent dir

            this.logger.info( "Generating " + components.getComponents().size() + " Service Component Descriptors to "
                + descriptorFile );

            ComponentDescriptorIO.write( components, descriptorFile, false );
            addResources = true;
        }

        return addResources;
    }


    /**
     * Create a component for the java class description.
     * @param description
     * @return The generated component descriptor or null if any error occurs.
     * @throws SCRDescriptorException
     */
    protected Component createComponent( JavaClassDescription description, JavaTag componentTag, MetaData metaData,
        final IssueLog iLog ) throws SCRDescriptorException
    {
        // create a new component
        final Component component = new Component( componentTag );

        // set implementation
        component.setImplementation( new Implementation( description.getName() ) );

        final OCD ocd = this.doComponent( componentTag, component, metaData, iLog );

        boolean inherited = getBoolean( componentTag, Constants.COMPONENT_INHERIT, true );
        this.doServices( description.getTagsByName( Constants.SERVICE, inherited ), component, description );

        // collect references from class tags and fields
        final Map<String, Object[]> references = new HashMap<String, Object[]>();
        // Utility handler for propertie
        final PropertyHandler propertyHandler = new PropertyHandler( component, ocd );

        JavaClassDescription currentDescription = description;
        do
        {
            // properties
            final JavaTag[] props = currentDescription.getTagsByName( Constants.PROPERTY, false );
            for ( int i = 0; i < props.length; i++ )
            {
                propertyHandler.testProperty( props[i], null, description == currentDescription );
            }

            // references
            final JavaTag[] refs = currentDescription.getTagsByName( Constants.REFERENCE, false );
            for ( int i = 0; i < refs.length; i++ )
            {
                this.testReference( references, refs[i], null, description == currentDescription );
            }

            // fields
            final JavaField[] fields = currentDescription.getFields();
            for ( int i = 0; i < fields.length; i++ )
            {
                JavaTag tag = fields[i].getTagByName( Constants.REFERENCE );
                if ( tag != null )
                {
                    this.testReference( references, tag, fields[i].getName(), description == currentDescription );
                }

                propertyHandler.handleField( fields[i], description == currentDescription );
            }

            currentDescription = currentDescription.getSuperClass();
        }
        while ( inherited && currentDescription != null );

        // process properties
        propertyHandler.processProperties( this.properties, iLog );

        // process references
        final Iterator<Map.Entry<String, Object[]>> refIter = references.entrySet().iterator();
        while ( refIter.hasNext() )
        {
            final Map.Entry<String, Object[]> entry = refIter.next();
            final String refName = entry.getKey();
            final Object[] values = entry.getValue();
            final JavaTag tag = ( JavaTag ) values[0];
            this.doReference( tag, refName, component, values[1].toString() );
        }

        // pid handling
        final boolean createPid = getBoolean( componentTag, Constants.COMPONENT_CREATE_PID, true );
        if ( createPid )
        {
            // check for an existing pid first
            boolean found = false;
            final Iterator<Property> iter = component.getProperties().iterator();
            while ( !found && iter.hasNext() )
            {
                final Property prop = iter.next();
                found = org.osgi.framework.Constants.SERVICE_PID.equals( prop.getName() );
            }
            if ( !found )
            {
                final Property pid = new Property();
                component.addProperty( pid );
                pid.setName( org.osgi.framework.Constants.SERVICE_PID );
                pid.setValue( component.getName() );
            }
        }
        return component;
    }


    /**
     * Fill the component object with the information from the tag.
     * @param tag
     * @param component
     */
    protected OCD doComponent( JavaTag tag, Component component, MetaData metaData, final IssueLog iLog )
        throws SCRDescriptorException
    {

        // check if this is an abstract definition
        final String abstractType = tag.getNamedParameter( Constants.COMPONENT_ABSTRACT );
        if ( abstractType != null )
        {
            component.setAbstract( "yes".equalsIgnoreCase( abstractType ) || "true".equalsIgnoreCase( abstractType ) );
        }
        else
        {
            // default true for abstract classes, false otherwise
            component.setAbstract( tag.getJavaClassDescription().isAbstract() );
        }

        // check if this is a definition to ignore
        final String ds = tag.getNamedParameter( Constants.COMPONENT_DS );
        component.setDs( ( ds == null ) ? true : ( "yes".equalsIgnoreCase( ds ) || "true".equalsIgnoreCase( ds ) ) );

        String name = tag.getNamedParameter( Constants.COMPONENT_NAME );
        component.setName( StringUtils.isEmpty( name ) ? component.getImplementation().getClassame() : name );

        component.setEnabled( Boolean.valueOf( getBoolean( tag, Constants.COMPONENT_ENABLED, true ) ) );
        component.setFactory( tag.getNamedParameter( Constants.COMPONENT_FACTORY ) );

        // FELIX-1703: support explicit SCR version declaration
        final String dsSpecVersion = tag.getNamedParameter( Constants.COMPONENT_DS_SPEC_VERSION );
        if ( dsSpecVersion != null )
        {
            component.setSpecVersion( toSpecVersionCode( dsSpecVersion, tag ) );
        }

        // FELIX-593: immediate attribute does not default to true all the
        // times hence we only set it if declared in the tag
        if ( tag.getNamedParameter( Constants.COMPONENT_IMMEDIATE ) != null )
        {
            component.setImmediate( Boolean.valueOf( getBoolean( tag, Constants.COMPONENT_IMMEDIATE, true ) ) );
        }

        // check for V1.1 attributes: configuration policy
        if ( tag.getNamedParameter( Constants.COMPONENT_CONFIG_POLICY ) != null )
        {
            component.setSpecVersion( Constants.VERSION_1_1 );
            component.setConfigurationPolicy( tag.getNamedParameter( Constants.COMPONENT_CONFIG_POLICY ) );
        }
        // check for V1.1 attributes: activate, deactivate
        if ( tag.getNamedParameter( Constants.COMPONENT_ACTIVATE ) != null )
        {
            component.setSpecVersion( Constants.VERSION_1_1 );
            component.setActivate( tag.getNamedParameter( Constants.COMPONENT_ACTIVATE ) );
        }
        if ( tag.getNamedParameter( Constants.COMPONENT_DEACTIVATE ) != null )
        {
            component.setSpecVersion( Constants.VERSION_1_1 );
            component.setDeactivate( tag.getNamedParameter( Constants.COMPONENT_DEACTIVATE ) );
        }
        if ( tag.getNamedParameter( Constants.COMPONENT_MODIFIED ) != null )
        {
            component.setSpecVersion( Constants.VERSION_1_1 );
            component.setModified( tag.getNamedParameter( Constants.COMPONENT_MODIFIED ) );
        }

        // whether metatype information is to generated for the component
        final String metaType = tag.getNamedParameter( Constants.COMPONENT_METATYPE );
        final boolean hasMetaType = metaType == null || "yes".equalsIgnoreCase( metaType )
            || "true".equalsIgnoreCase( metaType );
        if ( hasMetaType )
        {
            // ocd
            final OCD ocd = new OCD();
            metaData.addOCD( ocd );
            ocd.setId( component.getName() );
            String ocdName = tag.getNamedParameter( Constants.COMPONENT_LABEL );
            if ( ocdName == null )
            {
                ocdName = "%" + component.getName() + ".name";
            }
            ocd.setName( ocdName );
            String ocdDescription = tag.getNamedParameter( Constants.COMPONENT_DESCRIPTION );
            if ( ocdDescription == null )
            {
                ocdDescription = "%" + component.getName() + ".description";
            }
            ocd.setDescription( ocdDescription );
            // designate
            final Designate designate = new Designate();
            metaData.addDesignate( designate );
            designate.setPid( component.getName() );

            // factory pid
            final String setFactoryPidValue = tag.getNamedParameter( Constants.COMPONENT_SET_METATYPE_FACTORY_PID );
            final boolean setFactoryPid = setFactoryPidValue != null
                && ( "yes".equalsIgnoreCase( setFactoryPidValue ) || "true".equalsIgnoreCase( setFactoryPidValue ) );
            if ( setFactoryPid )
            {
                if ( component.getFactory() == null )
                {
                    designate.setFactoryPid( component.getName() );
                }
                else
                {
                    iLog.addWarning( "Component factory " + component.getName()
                        + " should not set metatype factory pid.", tag.getSourceLocation(), tag.getLineNumber() );
                }
            }
            // designate.object
            final MTObject mtobject = new MTObject();
            designate.setObject( mtobject );
            mtobject.setOcdref( component.getName() );
            return ocd;
        }
        return null;
    }


    /**
     * Process the service annotations
     * @param services
     * @param component
     * @param description
     * @throws SCRDescriptorException
     */
    protected void doServices( JavaTag[] services, Component component, JavaClassDescription description )
        throws SCRDescriptorException
    {
        // no services, hence certainly no service factory
        if ( services == null || services.length == 0 )
        {
            return;
        }

        final Service service = new Service();
        component.setService( service );
        boolean serviceFactory = false;
        for ( int i = 0; i < services.length; i++ )
        {
            final String name = services[i].getNamedParameter( Constants.SERVICE_INTERFACE );
            if ( StringUtils.isEmpty( name ) )
            {

                this.addInterfaces( service, services[i], description );
            }
            else
            {
                String interfaceName = name;
                // check if the value points to a class/interface
                // and search through the imports
                // but only for local services
                if ( description instanceof QDoxJavaClassDescription )
                {
                    final JavaClassDescription serviceClass = description.getReferencedClass( name );
                    if ( serviceClass == null )
                    {
                        throw new SCRDescriptorException( "Interface '" + name + "' in class " + description.getName()
                            + " does not point to a valid class/interface.", services[i] );
                    }
                    interfaceName = serviceClass.getName();
                }
                final Interface interf = new Interface( services[i] );
                interf.setInterfacename( interfaceName );
                service.addInterface( interf );
            }

            serviceFactory |= getBoolean( services[i], Constants.SERVICE_FACTORY, false );
        }

        service.setServicefactory( serviceFactory );
    }


    /**
     * Recursively add interfaces to the service.
     */
    protected void addInterfaces( final Service service, final JavaTag serviceTag,
        final JavaClassDescription description ) throws SCRDescriptorException
    {
        if ( description != null )
        {
            JavaClassDescription[] interfaces = description.getImplementedInterfaces();
            for ( int j = 0; j < interfaces.length; j++ )
            {
                final Interface interf = new Interface( serviceTag );
                interf.setInterfacename( interfaces[j].getName() );
                service.addInterface( interf );
                // recursivly add interfaces implemented by this interface
                this.addInterfaces( service, serviceTag, interfaces[j] );
            }

            // try super class
            this.addInterfaces( service, serviceTag, description.getSuperClass() );
        }
    }


    /**
     * Test a newly found reference
     * @param references
     * @param reference
     * @param defaultName
     * @param isInspectedClass
     * @throws SCRDescriptorException
     */
    protected void testReference( Map<String, Object[]> references, JavaTag reference, String defaultName,
        boolean isInspectedClass ) throws SCRDescriptorException
    {
        final String refName = this.getReferenceName( reference, defaultName );

        if ( refName != null )
        {
            if ( references.containsKey( refName ) )
            {
                // if the current class is the class we are currently inspecting, we
                // have found a duplicate definition
                if ( isInspectedClass )
                {
                    throw new SCRDescriptorException( "Duplicate definition for reference " + refName + " in class "
                        + reference.getJavaClassDescription().getName(), reference );
                }
            }
            else
            {
                // ensure interface
                String type = reference.getNamedParameter( Constants.REFERENCE_INTERFACE );
                if ( StringUtils.isEmpty( type ) )
                {
                    if ( reference.getField() != null )
                    {
                        type = reference.getField().getType();
                    }
                    else
                    {
                        throw new SCRDescriptorException( "Interface missing for reference " + refName + " in class "
                            + reference.getJavaClassDescription().getName(), reference );
                    }
                }
                else if ( isInspectedClass )
                {
                    // check if the value points to a class/interface
                    // and search through the imports
                    final JavaClassDescription serviceClass = reference.getJavaClassDescription().getReferencedClass(
                        type );
                    if ( serviceClass == null )
                    {
                        throw new SCRDescriptorException( "Interface '" + type + "' in class "
                            + reference.getJavaClassDescription().getName()
                            + " does not point to a valid class/interface.", reference );
                    }
                    type = serviceClass.getName();
                }
                references.put( refName, new Object[]
                    { reference, type } );
            }
        }
    }


    protected String getReferenceName( JavaTag reference, String defaultName ) throws SCRDescriptorException
    {
        String name = reference.getNamedParameter( Constants.REFERENCE_NAME );
        if ( !StringUtils.isEmpty( name ) )
        {
            return name;
        }
        name = reference.getNamedParameter( Constants.REFERENCE_NAME_REF );
        if ( !StringUtils.isEmpty( name ) )
        {
            final JavaField refField = this.getReferencedField( reference, name );
            final String[] values = refField.getInitializationExpression();
            if ( values == null || values.length == 0 )
            {
                throw new SCRDescriptorException( "Referenced field for " + name
                    + " has no values for a reference name.", reference );
            }
            if ( values.length > 1 )
            {
                throw new SCRDescriptorException( "Referenced field " + name
                    + " has more than one value for a reference name.", reference );
            }
            name = values[0];
        }

        return defaultName;
    }


    protected JavaField getReferencedField( final JavaTag tag, String ref ) throws SCRDescriptorException
    {
        int classSep = ref.lastIndexOf( '.' );
        JavaField field = null;
        if ( classSep == -1 )
        {
            // local variable
            field = tag.getJavaClassDescription().getFieldByName( ref );
        }
        if ( field == null )
        {
            field = tag.getJavaClassDescription().getExternalFieldByName( ref );
        }
        if ( field == null )
        {
            throw new SCRDescriptorException( "Reference references unknown field " + ref + " in class "
                + tag.getJavaClassDescription().getName(), tag );
        }
        return field;
    }


    /**
     * @param reference
     * @param defaultName
     * @param component
     */
    protected void doReference( JavaTag reference, String name, Component component, String type )
        throws SCRDescriptorException
    {
        final Reference ref = new Reference( reference, component.getJavaClassDescription() );
        ref.setName( name );
        ref.setInterfacename( type );
        ref.setCardinality( reference.getNamedParameter( Constants.REFERENCE_CARDINALITY ) );
        if ( ref.getCardinality() == null )
        {
            ref.setCardinality( "1..1" );
        }
        ref.setPolicy( reference.getNamedParameter( Constants.REFERENCE_POLICY ) );
        if ( ref.getPolicy() == null )
        {
            ref.setPolicy( "static" );
        }
        ref.setTarget( reference.getNamedParameter( Constants.REFERENCE_TARGET ) );
        final String bindValue = reference.getNamedParameter( Constants.REFERENCE_BIND );
        if ( bindValue != null )
        {
            ref.setBind( bindValue );
        }
        final String unbindValue = reference.getNamedParameter( Constants.REFERENCE_UNDBIND );
        if ( unbindValue != null )
        {
            ref.setUnbind( unbindValue );
        }
        final String updatedValue = reference.getNamedParameter( Constants.REFERENCE_UPDATED );
        if ( updatedValue != null )
        {
            ref.setUpdated( updatedValue );
        }
        final String isChecked = reference.getNamedParameter( Constants.REFERENCE_CHECKED );
        if ( isChecked != null )
        {
            ref.setChecked( Boolean.valueOf( isChecked ).booleanValue() );
        }
        final String strategy = reference.getNamedParameter( Constants.REFERENCE_STRATEGY );
        if ( strategy != null )
        {
            ref.setStrategy( strategy );
        }

        component.addReference( ref );
    }


    public static boolean getBoolean( JavaTag tag, String name, boolean defaultValue )
    {
        String value = tag.getNamedParameter( name );
        return ( value == null ) ? defaultValue : Boolean.valueOf( value ).booleanValue();
    }



    /**
     * Converts the specification version string to a specification version
     * code. Currently the following conversions are supported:
     * <table>
     * <tr><td><code>null</code></td><td>0</td></tr>
     * <tr><td>1.0</td><td>0</td></tr>
     * <tr><td>1.1</td><td>1</td></tr>
     * <tr><td>1.1-felix</td><td>2</td></tr>
     * </table>
     *
     * @param specVersion The specification version to convert. This may be
     *      <code>null</code> to assume the default version.
     *
     * @return The specification version code.
     *
     * @throws SCRDescriptorException if the <code>specVersion</code> parameter
     *      is not a supported value.
     */
    private int toSpecVersionCode( String specVersion, JavaTag tag ) throws SCRDescriptorException
    {
        if ( specVersion == null || specVersion.equals( Constants.COMPONENT_DS_SPEC_VERSION_10 ) )
        {
            return Constants.VERSION_1_0;
        }
        else if ( specVersion.equals( Constants.COMPONENT_DS_SPEC_VERSION_11 ) )
        {
            return Constants.VERSION_1_1;
        }
        else if ( specVersion.equals( Constants.COMPONENT_DS_SPEC_VERSION_11_FELIX ) )
        {
            return Constants.VERSION_1_1_FELIX;
        }

        // unknown specVersion string
        throw new SCRDescriptorException( "Unsupported or unknown DS spec version: " + specVersion, tag );
    }
}
