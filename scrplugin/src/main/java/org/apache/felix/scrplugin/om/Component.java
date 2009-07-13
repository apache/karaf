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

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.IssueLog;
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

    /** Configuration policy. (V1.1) */
    protected String configurationPolicy;

    /** Activation method. (V1.1) */
    protected String activate;

    /** Deactivation method. (V1.1) */
    protected String deactivate;

    /** Modified method. (V1.1) */
    protected String modified;

    /** The spec version. */
    protected int specVersion;

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
     * Get the spec version.
     */
    public int getSpecVersion() {
        return this.specVersion;
    }

    /**
     * Set the spec version.
     */
    public void setSpecVersion(int value) {
        this.specVersion = value;
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
     * Get the name of the activate method (or null for default)
     */
    public String getActivate() {
        return this.activate;
    }

    /**
     * Set the name of the deactivate method (or null for default)
     */
    public void setDeactivate(final String value) {
        this.deactivate = value;
    }

    /**
     * Get the name of the deactivate method (or null for default)
     */
    public String getDeactivate() {
        return this.deactivate;
    }

    /**
     * Set the name of the activate method (or null for default)
     */
    public void setActivate(final String value) {
        this.activate = value;
    }

    /**
     * Set the name of the modified method (or null for default)
     */
    public void setModified(final String value) {
        this.modified = value;
    }

    /**
     * Get the name of the modified method (or null for default)
     */
    public String getModified() {
        return this.modified;
    }

    /**
     * Validate the component description.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(final int specVersion, final IssueLog iLog)
    throws MojoExecutionException {
        final int currentIssueCount = iLog.getNumberOfErrors();

        // nothing to check if this is ignored
        if (!isDs()) {
            return;
        }

        final JavaClassDescription javaClass = this.tag.getJavaClassDescription();
        if (javaClass == null) {
            iLog.addError(this.getMessage("Tag not declared in a Java Class"));
        } else {

            // if the service is abstract, we do not validate everything
            if ( !this.isAbstract ) {
                // ensure non-abstract, public class
                if (!javaClass.isPublic()) {
                    iLog.addError(this.getMessage("Class must be public: " + javaClass.getName()));
                }
                if (javaClass.isAbstract() || javaClass.isInterface()) {
                    iLog.addError(this.getMessage("Class must be concrete class (not abstract or interface) : " + javaClass.getName()));
                }

                // no errors so far, let's continue
                if ( iLog.getNumberOfErrors() == currentIssueCount ) {
                    final String activateName = this.activate == null ? "activate" : this.activate;
                    final String deactivateName = this.deactivate == null ? "deactivate" : this.deactivate;

                    // check activate and deactivate methods
                    this.checkLifecycleMethod(specVersion, javaClass, activateName, true, iLog);
                    this.checkLifecycleMethod(specVersion, javaClass, deactivateName, false, iLog);

                    if ( this.modified != null && specVersion == Constants.VERSION_1_1 ) {
                        this.checkLifecycleMethod(specVersion, javaClass, this.modified, true, iLog);
                    }
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
                        iLog.addError(this.getMessage("Class must have public default constructor: " + javaClass.getName()));
                    }

                    // verify properties
                    for(final Property prop : this.getProperties()) {
                        prop.validate(specVersion, iLog);
                    }

                    // verify service
                    boolean isServiceFactory = false;
                    if (this.getService() != null) {
                        if ( this.getService().getInterfaces().size() == 0 ) {
                            iLog.addError(this.getMessage("Service interface information is missing for @scr.service tag"));
                        }
                        this.getService().validate(specVersion, iLog);
                        isServiceFactory = this.getService().isServicefactory();
                    }

                    // serviceFactory must not be true for immediate of component factory
                    if (isServiceFactory && this.isImmediate() != null && this.isImmediate().booleanValue() && this.getFactory() != null) {
                        iLog.addError(this.getMessage("Component must not be a ServiceFactory, if immediate and/or component factory: " + javaClass.getName()));
                    }

                    // immediate must not be true for component factory
                    if (this.isImmediate() != null && this.isImmediate().booleanValue() && this.getFactory() != null) {
                        iLog.addError(this.getMessage("Component must not be immediate if component factory: " + javaClass.getName()));
                    }
                }
            }
            if ( iLog.getNumberOfErrors() == currentIssueCount ) {
                // verify references
                for(final Reference ref : this.getReferences()) {
                    ref.validate(specVersion, this.isAbstract, iLog);
                }
            }
        }
        // check additional stuff if version is 1.1
        if ( specVersion == Constants.VERSION_1_1 ) {
            final String cp = this.getConfigurationPolicy();
            if ( cp != null
                 && !Constants.COMPONENT_CONFIG_POLICY_IGNORE.equals(cp)
                 && !Constants.COMPONENT_CONFIG_POLICY_REQUIRE.equals(cp)
                 && !Constants.COMPONENT_CONFIG_POLICY_OPTIONAL.equals(cp) ) {
                iLog.addError(this.getMessage("Component has an unknown value for configuration policy: " + cp));
            }

        }
    }

    private static final String TYPE_COMPONENT_CONTEXT = "org.osgi.service.component.ComponentContext";
    private static final String TYPE_BUNDLE_CONTEXT = "org.osgi.framework.BundleContext";
    private static final String TYPE_MAP = "java.util.Map";
    private static final String TYPE_INT = "int";
    private static final String TYPE_INTEGER = "java.lang.Integer";

    /**
     * Check for existence of lifecycle methods.
     * @param specVersion The spec version
     * @param javaClass The java class to inspect.
     * @param methodName The method name.
     * @param warnings The list of warnings used to add new warnings.
     */
    protected void checkLifecycleMethod(final int specVersion,
                                        final JavaClassDescription javaClass,
                                        final String methodName,
                                        final boolean isActivate,
                                        final IssueLog iLog)
    throws MojoExecutionException {
        // first candidate is (de)activate(ComponentContext)
        JavaMethod method = javaClass.getMethodBySignature(methodName, new String[] {TYPE_COMPONENT_CONTEXT});
        if ( method == null ) {
            if ( specVersion == Constants.VERSION_1_1) {
                // second candidate is (de)activate(BundleContext)
                method = javaClass.getMethodBySignature(methodName, new String[] {TYPE_BUNDLE_CONTEXT});
                if ( method == null ) {
                    // third candidate is (de)activate(Map)
                    method = javaClass.getMethodBySignature(methodName, new String[] {TYPE_MAP});

                    if ( method == null ) {
                        // if this is a deactivate method, we have two additional possibilities
                        // a method with parameter of type int and one of type Integer
                        if ( !isActivate ) {
                            method = javaClass.getMethodBySignature(methodName, new String[] {TYPE_INT});
                            if ( method == null ) {
                                method = javaClass.getMethodBySignature(methodName, new String[] {TYPE_INTEGER});
                            }
                        }

                        // fourth candidate is (de)activate with two or three arguments (type must be BundleContext, ComponentCtx and Map)
                        // as we have to iterate now and the fifth candidate is zero arguments
                        // we already store this option
                        JavaMethod zeroArgMethod = null;
                        JavaMethod found = method;
                        final JavaMethod[] methods = javaClass.getMethods();
                        int i = 0;
                        while ( i < methods.length ) {
                            if ( methodName.equals(methods[i].getName()) ) {

                                if ( methods[i].getParameters().length == 0 ) {
                                    zeroArgMethod = methods[i];
                                } else if ( methods[i].getParameters().length >= 2 ) {
                                    boolean valid = true;
                                    for(int m=0; m<methods[i].getParameters().length; m++) {
                                        final String type = methods[i].getParameters()[m].getType();
                                        if ( !type.equals(TYPE_BUNDLE_CONTEXT)
                                              && !type.equals(TYPE_COMPONENT_CONTEXT)
                                              && !type.equals(TYPE_MAP) ) {
                                            // if this is deactivate, int and integer are possible as well
                                            if ( isActivate || (!type.equals(TYPE_INT) && !type.equals(TYPE_INTEGER)) ) {
                                                valid = false;
                                            }
                                        }
                                    }
                                    if ( valid ) {
                                        if ( found == null ) {
                                            found = methods[i];
                                        } else {
                                            // print warning
                                            iLog.addWarning(this.getMessage("Lifecycle method " + methods[i].getName() + " occurs several times with different matching signature."));
                                        }
                                    }
                                }
                            }
                            i++;
                        }
                        if ( found != null ) {
                            method = found;
                        } else {
                            method = zeroArgMethod;
                        }
                    }
                }
            }
            // if no method is found, we check for any method with that name to print some warnings!
            if ( method == null ) {
                final JavaMethod[] methods = javaClass.getMethods();
                for(int i=0; i<methods.length; i++) {
                    if ( methodName.equals(methods[i].getName()) ) {
                        if ( methods[i].getParameters().length != 1 ) {
                            iLog.addWarning(this.getMessage("Lifecycle method " + methods[i].getName() + " has wrong number of arguments"));
                        } else {
                            iLog.addWarning(this.getMessage("Lifecycle method " + methods[i].getName() + " has wrong argument " + methods[i].getParameters()[0].getType()));
                        }
                    }
                }
            }
        }
        // method must be protected for version 1.0
        if ( method != null && specVersion == Constants.VERSION_1_0) {
            // check protected
            if (method.isPublic()) {
                iLog.addWarning(this.getMessage("Lifecycle method " + method.getName() + " should be declared protected"));
            } else if (!method.isProtected()) {
                iLog.addWarning(this.getMessage("Lifecycle method " + method.getName() + " has wrong qualifier, public or protected required"));
            }
        }
    }

    /**
     * Return the configuration policy.
     */
    public String getConfigurationPolicy() {
        return this.configurationPolicy;
    }

    /**
     * Set the configuration policy.
     */
    public void setConfigurationPolicy(final String value) {
        this.configurationPolicy = value;
    }
}
