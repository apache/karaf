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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.om.Implementation;
import org.apache.felix.scrplugin.om.Interface;
import org.apache.felix.scrplugin.om.Property;
import org.apache.felix.scrplugin.om.Reference;
import org.apache.felix.scrplugin.om.Service;
import org.apache.felix.scrplugin.om.metatype.AttributeDefinition;
import org.apache.felix.scrplugin.om.metatype.Designate;
import org.apache.felix.scrplugin.om.metatype.MTObject;
import org.apache.felix.scrplugin.om.metatype.MetaData;
import org.apache.felix.scrplugin.om.metatype.OCD;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaClassDescriptorManager;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.xml.ComponentDescriptorIO;
import org.apache.felix.scrplugin.xml.MetaTypeIO;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * The <code>SCRDescriptorMojo</code>
 * generates a service descriptor file based on annotations found in the sources.
 *
 * @goal scr
 * @phase generate-resources
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

    public void execute() throws MojoExecutionException, MojoFailureException {
        this.getLog().debug("Starting SCRDescriptorMojo....");

        boolean hasFailures = false;

        JavaClassDescriptorManager jManager = new JavaClassDescriptorManager(this.getLog(),
                                                                             this.project);
        // iterate through all source classes and check for component tag
        final JavaClassDescription[] javaSources = jManager.getSourceDescriptions();

        final Components components = new Components();
        final Components abstractComponents = new Components();
        final MetaData metaData = new MetaData();
        metaData.setLocalization("metatype");

        for (int i = 0; i < javaSources.length; i++) {
            this.getLog().debug("Testing source " + javaSources[i].getName());
            final JavaTag tag = javaSources[i].getTagByName(Constants.COMPONENT);
            if (tag != null) {
                this.getLog().debug("Processing service class " + javaSources[i].getName());
                final Component comp = this.createComponent(javaSources[i], metaData);
                if (comp != null) {
                    if ( comp.isAbstract() ) {
                        this.getLog().debug("Adding abstract descriptor " + comp);
                        abstractComponents.addComponent(comp);
                    } else {
                        this.getLog().debug("Adding descriptor " + comp);
                        components.addComponent(comp);
                    }
                } else {
                    hasFailures = true;
                }
            }
        }

        // after checking all classes, throw if there were any failures
        if (hasFailures) {
            throw new MojoFailureException("SCR Descriptor parsing had failures (see log)");
        }

        // if we have abstract descriptors, write them
        final File adFile = new File(this.outputDirectory, Constants.ABSTRACT_DESCRIPTOR_RELATIVE_PATH);
        if ( !abstractComponents.getComponents().isEmpty() ) {
            this.getLog().info("Writing abstract service descriptor " + adFile + " with " + components.getComponents().size() + " entries.");
            adFile.getParentFile().mkdirs();
            ComponentDescriptorIO.write(abstractComponents, adFile);
        } else {
            this.getLog().debug("No abstract SCR Descriptors found in project.");
            // remove file
            if ( adFile.exists() ) {
                this.getLog().debug("Removing obsolete abstract service descriptor " + adFile);
                adFile.delete();
            }
        }

        // terminate if there is nothing else to write
        if (components.getComponents().isEmpty()) {
            this.getLog().debug("No SCR Descriptors found in project.");
            return;
        }

        // check file name
        if (StringUtils.isEmpty(this.finalName)) {
            this.getLog().error("Descriptor file name must not be empty.");
            return;
        }

        // finally the descriptors have to be written ....
        File descriptorFile = new File(new File(this.outputDirectory, "OSGI-INF"), this.finalName);
        descriptorFile.getParentFile().mkdirs(); // ensure parent dir

        this.getLog().info("Generating " + components.getComponents().size()
                + " Service Component Descriptors to " + descriptorFile);

        ComponentDescriptorIO.write(components, descriptorFile);

        // check file name
        if (StringUtils.isEmpty(this.metaTypeName)) {
            this.getLog().error("Meta type file name must not be empty.");
            return;
        }

        // create metatype information
        File mtFile = new File(this.outputDirectory, "OSGI-INF" + File.separator + "metatype" + File.separator + this.metaTypeName);
        mtFile.getParentFile().mkdirs();
        if ( metaData.getDescriptors().size() > 0 ) {
            MetaTypeIO.write(metaData, mtFile);
        } else {
            if ( mtFile.exists() ) {
                mtFile.delete();
            }
        }

        // now add the descriptor file to the maven resources
        final String ourRsrcPath = this.outputDirectory.getAbsolutePath();
        boolean found = false;
        final Iterator rsrcIterator = this.project.getResources().iterator();
        while ( !found && rsrcIterator.hasNext() ) {
            final Resource rsrc = (Resource)rsrcIterator.next();
            found = rsrc.getDirectory().equals(ourRsrcPath);
        }
        if ( !found ) {
            final Resource resource = new Resource();
            resource.setDirectory(this.outputDirectory.getAbsolutePath());
            this.project.addResource(resource);
        }
        // and set include accordingly
        this.project.getProperties().setProperty("Service-Component", "OSGI-INF/" + this.finalName);
    }

    /**
     * Create a component for the java class description.
     * @param description
     * @return The generated component descriptor or null if any error occurs.
     * @throws MojoExecutionException
     */
    protected Component createComponent(JavaClassDescription description, MetaData metaData)
    throws MojoExecutionException {

        final JavaTag componentTag = description.getTagByName(Constants.COMPONENT);
        final Component component = new Component(componentTag);

        // set implementation
        component.setImplementation(new Implementation(description.getName()));

        final OCD ocd = this.doComponent(componentTag, component, metaData);

        boolean inherited = this.getBoolean(componentTag, Constants.COMPONENT_INHERIT, false);
        boolean serviceFactory = this.doServices(description.getTagsByName(Constants.SERVICE, inherited), component, description);
        component.setServiceFactory(serviceFactory);

        // properties
        final JavaTag[] properties = description.getTagsByName(Constants.PROPERTY, inherited);
        if (properties != null && properties.length > 0) {
            for (int i=0; i < properties.length; i++) {
                this.doProperty(properties[i], null, component, ocd);
            }
        }

        // references
        final JavaTag[] references = description.getTagsByName(Constants.REFERENCE, inherited);
        if (references != null || references.length > 0) {
            for (int i=0; i < references.length; i++) {
                this.doReference(references[i], null, component);
            }
        }

        // fields
        do {
            JavaField[] fields = description.getFields();
            for (int i=0; fields != null && i < fields.length; i++) {
                JavaTag tag = fields[i].getTagByName(Constants.REFERENCE);
                if (tag != null) {
                    this.doReference(tag, fields[i].getName(), component);
                }

                tag = fields[i].getTagByName(Constants.PROPERTY);
                if (tag != null) {
                    this.doProperty(tag, fields[i].getInitializationExpression(), component, ocd);
                }
            }

            description = description.getSuperClass();
        } while (inherited && description != null);

        final List issues = new ArrayList();
        final List warnings = new ArrayList();
        component.validate(issues, warnings);

        // now log warnings and errors (warnings first)
        Iterator i = warnings.iterator();
        while ( i.hasNext() ) {
            this.getLog().warn((String)i.next());
        }
        i = issues.iterator();
        while ( i.hasNext() ) {
            this.getLog().error((String)i.next());
        }

        // return nothing if validation fails
        return issues.size() == 0 ? component : null;
    }

    /**
     * Fill the component object with the information from the tag.
     * @param tag
     * @param component
     */
    protected OCD doComponent(JavaTag tag, Component component, MetaData metaData) {

        // check if this is an abstract definition
        final String abstractType = tag.getNamedParameter(Constants.COMPONENT_ABSTRACT);
        component.setAbstract((abstractType == null ? false : "yes".equalsIgnoreCase(abstractType) || "true".equalsIgnoreCase(abstractType)));

        String name = tag.getNamedParameter(Constants.COMPONENT_NAME);
        component.setName(StringUtils.isEmpty(name) ? component.getImplementation().getClassame() : name);

        component.setEnabled(Boolean.valueOf(this.getBoolean(tag, Constants.COMPONENT_ENABLED, true)));
        component.setFactory(tag.getNamedParameter(Constants.COMPONENT_FACTORY));
        component.setImmediate(Boolean.valueOf(this.getBoolean(tag, Constants.COMPONENT_IMMEDIATE, true)));

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
     * @return
     * @throws MojoExecutionException
     */
    protected boolean doServices(JavaTag[] services, Component component, JavaClassDescription description)
    throws MojoExecutionException {
        // no services, hence certainly no service factory
        if (services == null || services.length == 0) {
            return false;
        }

        final Service service = new Service();
        component.setService(service);
        boolean serviceFactory = false;
        for (int i=0; i < services.length; i++) {
            String name = services[i].getNamedParameter(Constants.SERVICE_INTERFACE);
            if (StringUtils.isEmpty(name)) {

                while (description != null) {
                    JavaClassDescription[] interfaces = description.getImplementedInterfaces();
                    for (int j=0; interfaces != null && j < interfaces.length; j++) {
                        final Interface interf = new Interface(services[i]);
                        interf.setInterfacename(interfaces[j].getName());
                        service.addInterface(interf);
                    }

                    // try super class
                    description = description.getSuperClass();
                }
            } else {
                final Interface interf = new Interface(services[i]);
                interf.setInterfacename(name);
                service.addInterface(interf);
            }

            serviceFactory |= this.getBoolean(services[i], Constants.SERVICE_FACTORY, false);
        }

        return serviceFactory;
    }

    /**
     * @param property
     * @param defaultName
     * @param component
     */
    protected void doProperty(JavaTag property, String defaultName, Component component, OCD ocd) {
        String name = property.getNamedParameter(Constants.PROPERTY_NAME);
        if (StringUtils.isEmpty(name) && defaultName!= null) {
            name = defaultName.trim();
            if (name.startsWith("\"")) name = name.substring(1);
            if (name.endsWith("\"")) name = name.substring(0, name.length()-1);
        }

        if (!StringUtils.isEmpty(name)) {
            final Property prop = new Property(property);
            prop.setName(name);
            prop.setType(property.getNamedParameter(Constants.PROPERTY_TYPE));
            final String value = property.getNamedParameter(Constants.PROPERTY_VALUE);
            if ( value != null ) {
                prop.setValue(value);
            } else {
                // check for multivalue
                final List values = new ArrayList();
                final Map valueMap = property.getNamedParameterMap();
                for (Iterator vi = valueMap.entrySet().iterator(); vi.hasNext();) {
                    final Map.Entry entry = (Map.Entry) vi.next();
                    final String key = (String) entry.getKey();
                    if (key.startsWith("values")) {
                        values.add(entry.getValue());
                    }
                }
                if ( values.size() > 0 ) {
                    prop.setMultiValue((String[])values.toArray(new String[values.size()]));
                }
            }

            final boolean isPrivate = this.getBoolean(property, Constants.PROPERTY_PRIVATE, false);
            // if this is a public property and the component is generating metatype info
            // store the information!
            if ( !isPrivate && ocd != null ) {
                final AttributeDefinition ad = new AttributeDefinition();
                ocd.getProperties().add(ad);
                ad.setId(prop.getName());
                ad.setType(prop.getType());

                String adName = property.getNamedParameter(Constants.PROPERTY_LABEL);
                if ( adName == null ) {
                    adName = "%" + prop.getName() + ".name";
                }
                ad.setName(adName);
                String adDesc = property.getNamedParameter(Constants.PROPERTY_DESCRIPTION);
                if ( adDesc == null ) {
                    adDesc = "%" + prop.getName() + ".description";
                }
                ad.setDescription(adDesc);
                // set optional multivalues, cardinality might be overwritten by setValues !!
                final String cValue = property.getNamedParameter(Constants.PROPERTY_CARDINALITY);
                if (cValue != null) {
                    if ("-".equals(cValue)) {
                        // unlimited vector
                        ad.setCardinality(new Integer(Integer.MIN_VALUE));
                    } else if ("+".equals(cValue)) {
                       // unlimited array
                        ad.setCardinality(new Integer(Integer.MAX_VALUE));
                    } else {
                        try {
                            ad.setCardinality(Integer.valueOf(cValue));
                        } catch (NumberFormatException nfe) {
                            // default to scalar in case of conversion problem
                        }
                    }
                }
                ad.setDefaultValue(prop.getValue());
                ad.setDefaultMultiValue(prop.getMultiValue());

                // check options
                String[] parameters = property.getParameters();
                Map options = null;
                for (int j=0; j < parameters.length; j++) {
                    if (Constants.PROPERTY_OPTIONS.equals(parameters[j])) {
                        options = new LinkedHashMap();
                    } else if (options != null) {
                        String optionLabel = parameters[j];
                        String optionValue = (j < parameters.length-2) ? parameters[j+2] : null;
                        if (optionValue != null) {
                            options.put(optionLabel, optionValue);
                        }
                        j += 2;
                    }
                }
                ad.setOptions(options);
            }

            component.addProperty(prop);
        }
    }

    /**
     * @param reference
     * @param defaultName
     * @param component
     */
    protected void doReference(JavaTag reference, String defaultName, Component component) {
        String name = reference.getNamedParameter(Constants.REFERENCE_NAME);
        if (StringUtils.isEmpty(name)) {
            name = defaultName;
        }

        // ensure interface
        String type = reference.getNamedParameter(Constants.REFERENCE_INTERFACE);
        if (StringUtils.isEmpty(type)) {
            if ( reference.getField() != null ) {
                type = reference.getField().getType();
            }
        }

        if (!StringUtils.isEmpty(name)) {
            final Reference ref = new Reference(reference);
            ref.setName(name);
            ref.setInterfacename(type);
            ref.setCardinality(reference.getNamedParameter(Constants.REFERENCE_CARDINALITY));
            ref.setPolicy(reference.getNamedParameter(Constants.REFERENCE_POLICY));
            ref.setTarget(reference.getNamedParameter(Constants.REFERENCE_TARGET));
            String value;
            value = reference.getNamedParameter(Constants.REFERENCE_BIND);
            if ( value != null ) {
                ref.setBind(value);
            }
            value = reference.getNamedParameter(Constants.REFERENCE_UNDBIND);
            if ( value != null ) {
                ref.setUnbind(value);
            }
            component.addReference(ref);
        }
    }

    protected boolean getBoolean(JavaTag tag, String name, boolean defaultValue) {
        String value = tag.getNamedParameter(name);
        return (value == null) ? defaultValue : Boolean.valueOf(value).booleanValue();
    }
}
