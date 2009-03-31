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
package org.apache.felix.scrplugin.om;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scrplugin.tags.*;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * <code>Component</code>
 * is a described component.
 *
 */
public class Component extends AbstractObject {

    /** The name of the component. */
    protected String name;

    /** Is this component enabled? */
    protected Boolean enabled;

    /** Is this component immediately started. */
    protected Boolean immediate;

    /** The factory. */
    protected String factory;

    /** The implementation. */
    protected Implementation implementation;

    /** All properties. */
    protected List<Property> properties = new ArrayList<Property>();

    /** The corresponding service. */
    protected Service service;

    /** The references. */
    protected List<Reference> references = new ArrayList<Reference>();

    /** Is this an abstract description? */
    protected boolean isAbstract;

    /** Is this a descriptor to be ignored ? */
    protected boolean isDs;

    /**
     * Default constructor.
     */
    public Component() {
        this(null);
    }

    /**
     * Constructor from java source.
     */
    public Component(JavaTag t) {
        super(t);
    }

    /**
     * Return the associated java class description
     */
    public JavaClassDescription getJavaClassDescription() {
        if ( this.tag != null ) {
            return this.tag.getJavaClassDescription();
        }
        return null;
    }

    /**
     * @return All properties of this component.
     */
    public List<Property> getProperties() {
        return this.properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public void addProperty(Property property) {
        this.properties.add(property);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFactory() {
        return this.factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isImmediate() {
        return this.immediate;
    }

    public void setImmediate(Boolean immediate) {
        this.immediate = immediate;
    }

    public Implementation getImplementation() {
        return this.implementation;
    }

    public void setImplementation(Implementation implementation) {
        this.implementation = implementation;
    }

    public Service getService() {
        return this.service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public List<Reference> getReferences() {
        return this.references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public void addReference(Reference ref) {
        this.references.add(ref);
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isDs() {
        return isDs;
    }

    public void setDs(boolean isDs) {
        this.isDs = isDs;
    }

    /**
     * Validate the component description.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(List<String> issues, List<String> warnings)
    throws MojoExecutionException {
        final int currentIssueCount = issues.size();

        // nothing to check if this is ignored
        if (!isDs()) {
            return;
        }

        final JavaClassDescription javaClass = this.tag.getJavaClassDescription();
        if (javaClass == null) {
            issues.add(this.getMessage("Tag not declared in a Java Class"));
        } else {

            // if the service is abstract, we do not validate everything
            if ( !this.isAbstract ) {
                // ensure non-abstract, public class
                if (!javaClass.isPublic()) {
                    issues.add(this.getMessage("Class must be public: " + javaClass.getName()));
                }
                if (javaClass.isAbstract() || javaClass.isInterface()) {
                    issues.add(this.getMessage("Class must be concrete class (not abstract or interface) : " + javaClass.getName()));
                }

                // no errors so far, let's continue
                if ( issues.size() == currentIssueCount ) {
                    // check activate and deactivate methods
                    this.checkLifecycleMethod(javaClass, "activate", warnings);
                    this.checkLifecycleMethod(javaClass, "deactivate", warnings);

                    // ensure public default constructor
                    boolean constructorFound = true;
                    JavaMethod[] methods = javaClass.getMethods();
                    for (int i = 0; methods != null && i < methods.length; i++) {
                        if (methods[i].isConstructor()) {
                            // if public default, succeed
                            if (methods[i].isPublic()
                                && (methods[i].getParameters() == null || methods[i].getParameters().length == 0)) {
                                constructorFound = true;
                                break;
                            }

                            // non-public/non-default constructor found, must have explicit
                            constructorFound = false;
                        }
                    }
                    if (!constructorFound) {
                        issues.add(this.getMessage("Class must have public default constructor: " + javaClass.getName()));
                    }

                    // verify properties
                    for(final Property prop : this.getProperties()) {
                        prop.validate(issues, warnings);
                    }

                    // verify service
                    boolean isServiceFactory = false;
                    if (this.getService() != null) {
                        if ( this.getService().getInterfaces().size() == 0 ) {
                            issues.add(this.getMessage("Service interface information is missing for @scr.service tag"));
                        }
                        this.getService().validate(issues, warnings);
                        isServiceFactory = this.getService().isServicefactory();
                    }

                    // serviceFactory must not be true for immediate of component factory
                    if (isServiceFactory && this.isImmediate() != null && this.isImmediate().booleanValue() && this.getFactory() != null) {
                        issues.add(this.getMessage("Component must not be a ServiceFactory, if immediate and/or component factory: " + javaClass.getName()));
                    }

                    // immediate must not be true for component factory
                    if (this.isImmediate() != null && this.isImmediate().booleanValue() && this.getFactory() != null) {
                        issues.add(this.getMessage("Component must not be immediate if component factory: " + javaClass.getName()));
                    }
                }
            }
            if ( issues.size() == currentIssueCount ) {
                // verify references
                for(final Reference ref : this.getReferences()) {
                    ref.validate(issues, warnings, this.isAbstract);
                }
            }
        }
    }

    /**
     * Check for existence of lifecycle methods.
     * @param javaClass The java class to inspect.
     * @param methodName The method name.
     * @param warnings The list of warnings used to add new warnings.
     */
    protected void checkLifecycleMethod(JavaClassDescription javaClass, String methodName, List<String> warnings)
    throws MojoExecutionException {
        final JavaMethod method = javaClass.getMethodBySignature(methodName, new String[] {"org.osgi.service.component.ComponentContext"});
        if ( method != null ) {
            // check protected
            if (method.isPublic()) {
                warnings.add(this.getMessage("Lifecycle method " + method.getName() + " should be declared protected"));
            } else if (!method.isProtected()) {
                warnings.add(this.getMessage("Lifecycle method " + method.getName() + " has wrong qualifier, public or protected required"));
            }
        } else {
            // if no method is found, we check for any method with that name
            final JavaMethod[] methods = javaClass.getMethods();
            for(int i=0; i<methods.length; i++) {
                if ( methodName.equals(methods[i].getName()) ) {

                    if ( methods[i].getParameters().length != 1 ) {
                        warnings.add(this.getMessage("Lifecycle method " + methods[i].getName() + " has wrong number of arguments"));
                    } else {
                        warnings.add(this.getMessage("Lifecycle method " + methods[i].getName() + " has wrong argument " + methods[i].getParameters()[0].getType()));
                    }
                }
            }
        }
    }
}
