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
import java.util.*;

import org.apache.felix.scrplugin.om.*;
import org.apache.felix.scrplugin.om.metatype.*;
import org.apache.felix.scrplugin.tags.*;
import org.apache.felix.scrplugin.tags.qdox.QDoxJavaClassDescription;
import org.apache.felix.scrplugin.xml.ComponentDescriptorIO;
import org.apache.felix.scrplugin.xml.MetaTypeIO;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.osgi.service.metatype.MetaTypeService;

/**
 * The <code>SCRDescriptorMojo</code>
 * generates a service descriptor file based on annotations found in the sources.
 *
 * @goal scr
 * @phase process-classes
 * @description Build Service Descriptors from Java Source
 * @requiresDependencyResolution compile
 */
public class SCRDescriptorMojo extends AbstractMojo {

    /**
     * @parameter expression="${project.build.directory}/scr-plugin-generated"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Name of the generated descriptor.
     *
     * @parameter expression="${scr.descriptor.name}" default-value="serviceComponents.xml"
     */
    private String finalName;

    /**
     * Name of the generated meta type file.
     *
     * @parameter default-value="metatype.xml"
     */
    private String metaTypeName;

    /**
     * This flag controls the generation of the bind/unbind methods.
     * @parameter default-value="true"
     */
    private boolean generateAccessors;

    /**
     * This flag controls whether the javadoc source code will be scanned for
     * tags.
     * @parameter default-value="true"
     */
    protected boolean parseJavadoc;

    /**
     * This flag controls whether the annotations in the sources will be
     * processed.
     * @parameter default-value="true"
     */
    protected boolean processAnnotations;

    /**
     * In strict mode the plugin even fails on warnings.
     * @parameter default-value="false"
     */
    protected boolean strictMode;

    /**
     * The comma separated list of tokens to exclude when processing sources.
     *
     * @parameter alias="excludes"
     */
    private String sourceExcludes;

    /**
     * Predefined properties.
     *
     * @parameter
     */
    private Map<String, String> properties = new HashMap<String, String>();

    /**
     * Allows to define additional implementations of the interface
     * {@link org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider}
     * that provide mappings from custom annotations to
     * {@link org.apache.felix.scrplugin.tags.JavaTag} implementations.
     * List of full qualified class file names.
     *
     * @parameter
     */
    private String[] annotationTagProviders = {};

    /**
     * The version of the DS spec this plugin generates a descriptor for.
     * By default the version is detected by the used tags.
     * @parameter
     */
    private String specVersion;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.getLog().debug("Starting SCRDescriptorMojo....");
        this.getLog().debug("..generating accessors: " + this.generateAccessors);
        this.getLog().debug("..parsing javadocs: " + this.parseJavadoc);
        this.getLog().debug("..processing annotations: " + this.processAnnotations);

        // check speck version configuration
        int specVersion = Constants.VERSION_1_0;
        if ( this.specVersion != null ) {
            if ( this.specVersion.equals("1.1") ) {
                specVersion = Constants.VERSION_1_1;
            } else if ( !this.specVersion.equals("1.0") ) {
                throw new MojoExecutionException("Unknown configuration for spec version: " + this.specVersion);
            }
        } else {
            this.getLog().debug("..auto detecting spec version");
        }
        JavaClassDescriptorManager jManager = new JavaClassDescriptorManager(this.getLog(),
                                                                             this.project,
                                                                             this.annotationTagProviders,
                                                                             this.sourceExcludes,
                                                                             this.parseJavadoc,
                                                                             this.processAnnotations);
        final IssueLog iLog = new IssueLog(this.strictMode);

        final MetaData metaData = new MetaData();
        metaData.setLocalization(MetaTypeService.METATYPE_DOCUMENTS_LOCATION + "/metatype");

        // iterate through all source classes and check for component tag
        final JavaClassDescription[] javaSources = jManager.getSourceDescriptions();
        Arrays.sort(javaSources, new JavaClassDescriptionInheritanceComparator());

        final List<Component> scannedComponents = new ArrayList<Component>();
        for (int i = 0; i < javaSources.length; i++) {
            this.getLog().debug("Testing source " + javaSources[i].getName());
            final JavaTag tag = javaSources[i].getTagByName(Constants.COMPONENT);
            if (tag != null) {
                this.getLog().debug("Processing service class " + javaSources[i].getName());
                // check if there is more than one component tag!
                if ( javaSources[i].getTagsByName(Constants.COMPONENT, false).length > 1 ) {
                    iLog.addError("Class " + javaSources[i].getName() + " has more than one " + Constants.COMPONENT + " tag." +
                            " Merge the tags to a single tag.");
                } else {
                    final Component comp = this.createComponent(javaSources[i], tag, metaData, iLog);
                    if ( comp.getSpecVersion() > specVersion ) {
                        // if a spec version has been configured and a component requires a higher
                        // version, this is considered an error!
                        if ( this.specVersion != null ) {
                            String v = "1.0";
                            if ( comp.getSpecVersion() == Constants.VERSION_1_1 ) {
                                v = "1.1";
                            }
                            iLog.addError("Component " + comp + " requires spec version " + v + " but plugin is configured to use version " + this.specVersion);
                        }
                        specVersion = comp.getSpecVersion();
                    }
                    scannedComponents.add(comp);
                }
            }
        }
        this.getLog().debug("..generating descriptor for spec version: " + this.specVersion);

        // now check for abstract components and fill components objects
        final Components components = new Components();
        final Components abstractComponents = new Components();
        components.setSpecVersion(specVersion);
        abstractComponents.setSpecVersion(specVersion);

        for(final Component comp : scannedComponents ) {
            final int errorCount = iLog.getNumberOfErrors();
            // before we can validate we should check the references for bind/unbind method
            // in order to create them if possible

            for(final Reference ref : comp.getReferences() ) {
                // if this is a field with a single cardinality,
                // we look for the bind/unbind methods
                // and create them if they are not availabe
                if ( this.generateAccessors && !ref.isLookupStrategy() ) {
                    if ( ref.getJavaTag().getField() != null && comp.getJavaClassDescription() instanceof ModifiableJavaClassDescription ) {
                        if ( ref.getCardinality().equals("0..1") || ref.getCardinality().equals("1..1") ) {
                            final String bindValue = ref.getBind();
                            final String unbindValue = ref.getUnbind();
                            final String name = ref.getName();
                            final String type = ref.getInterfacename();

                            boolean createBind = false;
                            boolean createUnbind = false;
                            // Only create method if no bind name has been specified
                            if ( bindValue == null && ref.findMethod(specVersion, "bind") == null ) {
                                // create bind method
                                createBind = true;
                            }
                            if ( unbindValue == null && ref.findMethod(specVersion, "unbind") == null ) {
                                // create unbind method
                                createUnbind = true;
                            }
                            if ( createBind || createUnbind ) {
                                ((ModifiableJavaClassDescription)comp.getJavaClassDescription()).addMethods(name, type, createBind, createUnbind);
                            }
                        }
                    }
                }
            }
            comp.validate(specVersion, iLog);
            // ignore component if it has errors
            if ( iLog.getNumberOfErrors() == errorCount ) {
                if ( !comp.isDs() ) {
                    getLog().debug("Ignoring descriptor " + comp);
                } else if ( comp.isAbstract() ) {
                    this.getLog().debug("Adding abstract descriptor " + comp);
                    abstractComponents.addComponent(comp);
                } else {
                    this.getLog().debug("Adding descriptor " + comp);
                    components.addComponent(comp);
                    abstractComponents.addComponent(comp);
                }
            }
        }

        // log issues
        iLog.log(this.getLog());

        // after checking all classes, throw if there were any failures
        if ( iLog.hasErrors() ) {
            throw new MojoFailureException("SCR Descriptor parsing had failures (see log)");
        }

        boolean addResources = false;
        // write meta type info if there is a file name
        if (!StringUtils.isEmpty(this.metaTypeName)) {
            File mtFile = new File(this.outputDirectory, "OSGI-INF" + File.separator + "metatype" + File.separator + this.metaTypeName);
            final int size = metaData.getOCDs().size() + metaData.getDesignates().size();
            if (size > 0 ) {
                this.getLog().info("Generating "
                    + size
                    + " MetaType Descriptors to " + mtFile);
                mtFile.getParentFile().mkdirs();
                MetaTypeIO.write(metaData, mtFile);
                addResources = true;
            } else {
                if ( mtFile.exists() ) {
                    mtFile.delete();
                }
            }

        } else {
            this.getLog().info("Meta type file name is not set: meta type info is not written.");
        }

        // if we have descriptors, write them in our scr private file (for component inheritance)
        final File adFile = new File(this.outputDirectory, Constants.ABSTRACT_DESCRIPTOR_RELATIVE_PATH);
        if ( !abstractComponents.getComponents().isEmpty() ) {
            this.getLog().info("Writing abstract service descriptor " + adFile + " with " + abstractComponents.getComponents().size() + " entries.");
            adFile.getParentFile().mkdirs();
            ComponentDescriptorIO.write(abstractComponents, adFile, true);
            addResources = true;
        } else {
            this.getLog().debug("No abstract SCR Descriptors found in project.");
            // remove file
            if ( adFile.exists() ) {
                this.getLog().debug("Removing obsolete abstract service descriptor " + adFile);
                adFile.delete();
            }
        }

        // check descriptor file
        final File descriptorFile = StringUtils.isEmpty(this.finalName) ? null : new File(new File(this.outputDirectory, "OSGI-INF"), this.finalName);

        // terminate if there is nothing else to write
        if (components.getComponents().isEmpty()) {
            this.getLog().debug("No SCR Descriptors found in project.");
            // remove file if it exists
            if ( descriptorFile != null && descriptorFile.exists() ) {
                this.getLog().debug("Removing obsolete service descriptor " + descriptorFile);
                descriptorFile.delete();
            }
        } else {

            if (descriptorFile == null) {
                throw new MojoFailureException("Descriptor file name must not be empty.");
            }

            // finally the descriptors have to be written ....
            descriptorFile.getParentFile().mkdirs(); // ensure parent dir

            this.getLog().info("Generating " + components.getComponents().size()
                    + " Service Component Descriptors to " + descriptorFile);

            ComponentDescriptorIO.write(components, descriptorFile, false);
            addResources = true;

            // and set include accordingly
            String svcComp = project.getProperties().getProperty("Service-Component");
            svcComp= (svcComp == null) ? "OSGI-INF/" + finalName : svcComp + ", " + "OSGI-INF/" + finalName;
            project.getProperties().setProperty("Service-Component", svcComp);
        }

        // now add the descriptor directory to the maven resources
        if (addResources) {
            final String ourRsrcPath = this.outputDirectory.getAbsolutePath();
            boolean found = false;
            @SuppressWarnings("unchecked")
            final Iterator<Resource> rsrcIterator = this.project.getResources().iterator();
            while ( !found && rsrcIterator.hasNext() ) {
                final Resource rsrc = rsrcIterator.next();
                found = rsrc.getDirectory().equals(ourRsrcPath);
            }
            if ( !found ) {
                final Resource resource = new Resource();
                resource.setDirectory(this.outputDirectory.getAbsolutePath());
                this.project.addResource(resource);
            }
        }
    }

    /**
     * Create a component for the java class description.
     * @param description
     * @return The generated component descriptor or null if any error occurs.
     * @throws MojoExecutionException
     */
    protected Component createComponent(JavaClassDescription description,
                                        JavaTag componentTag,
                                        MetaData metaData,
                                        final IssueLog iLog)
    throws MojoExecutionException {
        // create a new component
        final Component component = new Component(componentTag);

        // set implementation
        component.setImplementation(new Implementation(description.getName()));

        final OCD ocd = this.doComponent(componentTag, component, metaData, iLog);

        boolean inherited = getBoolean(componentTag, Constants.COMPONENT_INHERIT, true);
        this.doServices(description.getTagsByName(Constants.SERVICE, inherited), component, description);

        // collect references from class tags and fields
        final Map<String, Object[]> references = new HashMap<String, Object[]>();
        // Utility handler for propertie
        final PropertyHandler propertyHandler = new PropertyHandler(component, ocd);

        JavaClassDescription currentDescription = description;
        do {
            // properties
            final JavaTag[] props = currentDescription.getTagsByName(Constants.PROPERTY, false);
            for (int i=0; i < props.length; i++) {
                propertyHandler.testProperty(props[i], null, description == currentDescription);
            }

            // references
            final JavaTag[] refs = currentDescription.getTagsByName(Constants.REFERENCE, false);
            for (int i=0; i < refs.length; i++) {
                this.testReference(references, refs[i], null, description == currentDescription);
            }

            // fields
            final JavaField[] fields = currentDescription.getFields();
            for (int i=0; i < fields.length; i++) {
                JavaTag tag = fields[i].getTagByName(Constants.REFERENCE);
                if (tag != null) {
                    this.testReference(references, tag, fields[i].getName(), description == currentDescription);
                }

                propertyHandler.handleField(fields[i], description == currentDescription);
            }

            currentDescription = currentDescription.getSuperClass();
        } while (inherited && currentDescription != null);

        // process properties
        propertyHandler.processProperties(this.properties, iLog);

        // process references
        final Iterator<Map.Entry<String, Object[]>> refIter = references.entrySet().iterator();
        while ( refIter.hasNext() ) {
            final Map.Entry<String, Object[]> entry = refIter.next();
            final String refName = entry.getKey();
            final Object[] values = entry.getValue();
            final JavaTag tag = (JavaTag)values[0];
            this.doReference(tag, refName, component, values[1].toString());
        }

        // pid handling
        final boolean createPid = getBoolean(componentTag, Constants.COMPONENT_CREATE_PID, true);
        if ( createPid ) {
            // check for an existing pid first
            boolean found = false;
            final Iterator<Property> iter = component.getProperties().iterator();
            while ( !found && iter.hasNext() ) {
                final Property prop = iter.next();
                found = org.osgi.framework.Constants.SERVICE_PID.equals( prop.getName() );
            }
            if ( !found ) {
                final Property pid = new Property();
                component.addProperty(pid);
                pid.setName(org.osgi.framework.Constants.SERVICE_PID);
                pid.setValue(component.getName());
            }
        }
        return component;
    }

    /**
     * Fill the component object with the information from the tag.
     * @param tag
     * @param component
     */
    protected OCD doComponent(JavaTag tag, Component component, MetaData metaData,
            final IssueLog iLog) {

        // check if this is an abstract definition
        final String abstractType = tag.getNamedParameter(Constants.COMPONENT_ABSTRACT);
        if (abstractType != null) {
            component.setAbstract("yes".equalsIgnoreCase(abstractType) || "true".equalsIgnoreCase(abstractType));
        } else {
            // default true for abstract classes, false otherwise
            component.setAbstract(tag.getJavaClassDescription().isAbstract());
        }

        // check if this is a definition to ignore
        final String ds = tag.getNamedParameter(Constants.COMPONENT_DS);
        component.setDs((ds == null) ? true : ("yes".equalsIgnoreCase(ds) || "true".equalsIgnoreCase(ds)));

        String name = tag.getNamedParameter(Constants.COMPONENT_NAME);
        component.setName(StringUtils.isEmpty(name) ? component.getImplementation().getClassame() : name);

        component.setEnabled(Boolean.valueOf(getBoolean(tag, Constants.COMPONENT_ENABLED, true)));
        component.setFactory(tag.getNamedParameter(Constants.COMPONENT_FACTORY));

        // FELIX-593: immediate attribute does not default to true all the
        // times hence we only set it if declared in the tag
        if (tag.getNamedParameter(Constants.COMPONENT_IMMEDIATE) != null) {
            component.setImmediate(Boolean.valueOf(getBoolean(tag,
                Constants.COMPONENT_IMMEDIATE, true)));
        }

        // check for V1.1 attributes: configuration policy
        if ( tag.getNamedParameter(Constants.COMPONENT_CONFIG_POLICY) != null ) {
            component.setSpecVersion(Constants.VERSION_1_1);
            component.setConfigurationPolicy(tag.getNamedParameter(Constants.COMPONENT_CONFIG_POLICY));
        }
        // check for V1.1 attributes: activate, deactivate
        if ( tag.getNamedParameter(Constants.COMPONENT_ACTIVATE) != null ) {
            component.setSpecVersion(Constants.VERSION_1_1);
            component.setActivate(tag.getNamedParameter(Constants.COMPONENT_ACTIVATE));
        }
        if ( tag.getNamedParameter(Constants.COMPONENT_DEACTIVATE) != null ) {
            component.setSpecVersion(Constants.VERSION_1_1);
            component.setDeactivate(tag.getNamedParameter(Constants.COMPONENT_DEACTIVATE));
        }
        if ( tag.getNamedParameter(Constants.COMPONENT_MODIFIED) != null ) {
            component.setSpecVersion(Constants.VERSION_1_1);
            component.setModified(tag.getNamedParameter(Constants.COMPONENT_MODIFIED));
        }

        // whether metatype information is to generated for the component
        final String metaType = tag.getNamedParameter(Constants.COMPONENT_METATYPE);
        final boolean hasMetaType = metaType == null || "yes".equalsIgnoreCase(metaType)
            || "true".equalsIgnoreCase(metaType);
        if ( hasMetaType ) {
            // ocd
            final OCD ocd = new OCD();
            metaData.addOCD(ocd);
            ocd.setId(component.getName());
            String ocdName = tag.getNamedParameter(Constants.COMPONENT_LABEL);
            if ( ocdName == null ) {
                ocdName = "%" + component.getName() + ".name";
            }
            ocd.setName(ocdName);
            String ocdDescription = tag.getNamedParameter(Constants.COMPONENT_DESCRIPTION);
            if ( ocdDescription == null ) {
                ocdDescription = "%" + component.getName() + ".description";
            }
            ocd.setDescription(ocdDescription);
            // designate
            final Designate designate = new Designate();
            metaData.addDesignate(designate);
            designate.setPid(component.getName());

            // factory pid
            final String setFactoryPidValue = tag.getNamedParameter(Constants.COMPONENT_SET_METATYPE_FACTORY_PID);
            final boolean setFactoryPid = setFactoryPidValue != null &&
                ("yes".equalsIgnoreCase(setFactoryPidValue) || "true".equalsIgnoreCase(setFactoryPidValue));
            if ( setFactoryPid ) {
                if ( component.getFactory() == null ) {
                    designate.setFactoryPid( component.getName() );
                } else {
                    iLog.addError("Component factory " + component.getName() + " should not set metatype factory pid.");
                }
            }
            // designate.object
            final MTObject mtobject = new MTObject();
            designate.setObject(mtobject);
            mtobject.setOcdref(component.getName());
            return ocd;
        }
        return null;
    }

    /**
     * Process the service annotations
     * @param services
     * @param component
     * @param description
     * @throws MojoExecutionException
     */
    protected void doServices(JavaTag[] services, Component component, JavaClassDescription description)
    throws MojoExecutionException {
        // no services, hence certainly no service factory
        if (services == null || services.length == 0) {
            return;
        }

        final Service service = new Service();
        component.setService(service);
        boolean serviceFactory = false;
        for (int i=0; i < services.length; i++) {
            final String name = services[i].getNamedParameter(Constants.SERVICE_INTERFACE);
            if (StringUtils.isEmpty(name)) {

                this.addInterfaces(service, services[i], description);
            } else {
                String interfaceName = name;
                // check if the value points to a class/interface
                // and search through the imports
                // but only for local services
                if ( description instanceof QDoxJavaClassDescription ) {
                    final JavaClassDescription serviceClass = description.getReferencedClass(name);
                    if ( serviceClass == null ) {
                        throw new MojoExecutionException("Interface '"+ name + "' in class " + description.getName() + " does not point to a valid class/interface.");
                    }
                    interfaceName = serviceClass.getName();
                }
                final Interface interf = new Interface(services[i]);
                interf.setInterfacename(interfaceName);
                service.addInterface(interf);
            }

            serviceFactory |= getBoolean(services[i], Constants.SERVICE_FACTORY, false);
        }

        service.setServicefactory(serviceFactory);
    }

    /**
     * Recursively add interfaces to the service.
     */
    protected void addInterfaces(final Service service, final JavaTag serviceTag, final JavaClassDescription description)
    throws MojoExecutionException {
        if ( description != null ) {
            JavaClassDescription[] interfaces = description.getImplementedInterfaces();
            for (int j=0; j < interfaces.length; j++) {
                final Interface interf = new Interface(serviceTag);
                interf.setInterfacename(interfaces[j].getName());
                service.addInterface(interf);
                // recursivly add interfaces implemented by this interface
                this.addInterfaces(service, serviceTag, interfaces[j]);
            }

            // try super class
            this.addInterfaces(service, serviceTag, description.getSuperClass());
        }
    }

    /**
     * Test a newly found reference
     * @param references
     * @param reference
     * @param defaultName
     * @param isInspectedClass
     * @throws MojoExecutionException
     */
    protected void testReference(Map<String, Object[]> references, JavaTag reference, String defaultName, boolean isInspectedClass)
    throws MojoExecutionException {
        final String refName = this.getReferenceName(reference, defaultName);

        if ( refName != null ) {
            if ( references.containsKey(refName) ) {
                // if the current class is the class we are currently inspecting, we
                // have found a duplicate definition
                if ( isInspectedClass ) {
                    throw new MojoExecutionException("Duplicate definition for reference " + refName + " in class " + reference.getJavaClassDescription().getName());
                }
            } else {
                // ensure interface
                String type = reference.getNamedParameter(Constants.REFERENCE_INTERFACE);
                if (StringUtils.isEmpty(type)) {
                    if ( reference.getField() != null ) {
                        type = reference.getField().getType();
                    } else {
                        throw new MojoExecutionException("Interface missing for reference " + refName + " in class " + reference.getJavaClassDescription().getName());
                    }
                } else if ( isInspectedClass ) {
                    // check if the value points to a class/interface
                    // and search through the imports
                    final JavaClassDescription serviceClass = reference.getJavaClassDescription().getReferencedClass(type);
                    if ( serviceClass == null ) {
                        throw new MojoExecutionException("Interface '"+ type + "' in class " + reference.getJavaClassDescription().getName() + " does not point to a valid class/interface.");
                    }
                    type = serviceClass.getName();
                }
                references.put(refName, new Object[] {reference, type});
            }
        }
    }

    protected String getReferenceName(JavaTag reference, String defaultName)
    throws MojoExecutionException {
        String name = reference.getNamedParameter(Constants.REFERENCE_NAME);
        if (!StringUtils.isEmpty(name)) {
            return name;
        }
        name = reference.getNamedParameter(Constants.REFERENCE_NAME_REF);
        if (!StringUtils.isEmpty(name)) {
            final JavaField refField = this.getReferencedField(reference, name);
            final String[] values = refField.getInitializationExpression();
            if ( values == null || values.length == 0 ) {
                throw new MojoExecutionException("Referenced field for " + name + " has no values for a reference name.");
            }
            if ( values.length > 1 ) {
                throw new MojoExecutionException("Referenced field " + name + " has more than one value for a reference name.");
            }
            name = values[0];
        }

        return defaultName;
    }

    protected JavaField getReferencedField(final JavaTag tag, String ref)
    throws MojoExecutionException {
        int classSep = ref.lastIndexOf('.');
        JavaField field = null;
        if ( classSep == -1 ) {
            // local variable
            field = tag.getJavaClassDescription().getFieldByName(ref);
        }
        if ( field == null ) {
            field = tag.getJavaClassDescription().getExternalFieldByName(ref);
        }
        if ( field == null ) {
            throw new MojoExecutionException("Reference references unknown field " + ref + " in class " + tag.getJavaClassDescription().getName());
        }
        return field;
    }

    /**
     * @param reference
     * @param defaultName
     * @param component
     */
    protected void doReference(JavaTag reference, String name, Component component, String type)
    throws MojoExecutionException {
        final Reference ref = new Reference(reference, component.getJavaClassDescription());
        ref.setName(name);
        ref.setInterfacename(type);
        ref.setCardinality(reference.getNamedParameter(Constants.REFERENCE_CARDINALITY));
        if ( ref.getCardinality() == null ) {
            ref.setCardinality("1..1");
        }
        ref.setPolicy(reference.getNamedParameter(Constants.REFERENCE_POLICY));
        if ( ref.getPolicy() == null ) {
            ref.setPolicy("static");
        }
        ref.setTarget(reference.getNamedParameter(Constants.REFERENCE_TARGET));
        final String bindValue = reference.getNamedParameter(Constants.REFERENCE_BIND);
        if ( bindValue != null ) {
            ref.setBind(bindValue);
        }
        final String unbindValue = reference.getNamedParameter(Constants.REFERENCE_UNDBIND);
        if ( unbindValue != null ) {
            ref.setUnbind(unbindValue);
        }
        final String isChecked = reference.getNamedParameter(Constants.REFERENCE_CHECKED);
        if ( isChecked != null ) {
            ref.setChecked(Boolean.valueOf(isChecked).booleanValue());
        }
        final String strategy = reference.getNamedParameter(Constants.REFERENCE_STRATEGY);
        if ( strategy != null ) {
            ref.setStrategy(strategy);
        }

        component.addReference(ref);
    }

    public static boolean getBoolean(JavaTag tag, String name, boolean defaultValue) {
        String value = tag.getNamedParameter(name);
        return (value == null) ? defaultValue : Boolean.valueOf(value).booleanValue();
    }
}
