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

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.helper.IssueLog;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaMethod;
import org.apache.felix.scrplugin.tags.JavaTag;

/**
 * <code>Reference.java</code>...
 *
 */
public class Reference extends AbstractObject {

    protected String name;
    protected String interfacename;
    protected String target;
    protected String cardinality;
    protected String policy;
    protected String bind;
    protected String unbind;
    protected String updated;

    /** @since 1.0.9 */
    protected String strategy;

    /** Is this reference already checked? */
    protected boolean checked = false;

    /** The class description containing this reference. */
    protected final JavaClassDescription javaClassDescription;

    /**
     * Default constructor.
     */
    public Reference() {
        this(null, null);
    }

    /**
     * Constructor from java source.
     */
    public Reference(JavaTag t, JavaClassDescription desc) {
        super(t);
        this.javaClassDescription = desc;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInterfacename() {
        return this.interfacename;
    }

    public void setInterfacename(String interfacename) {
        this.interfacename = interfacename;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getCardinality() {
        return this.cardinality;
    }

    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    public String getPolicy() {
        return this.policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getBind() {
        return this.bind;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public String getUnbind() {
        return this.unbind;
    }

    public void setUnbind(String unbind) {
        this.unbind = unbind;
    }

    public String getUpdated() {
        return this.updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    /** @since 1.0.9 */
    public String getStrategy() {
        return strategy;
    }

    /** @since 1.0.9 */
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /** @since 1.0.9 */
    public boolean isLookupStrategy() {
        return Constants.REFERENCE_STRATEGY_LOOKUP.equals(getStrategy());
    }

    /**
     * Validate the property.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(final int specVersion,
                         final boolean componentIsAbstract,
                         final IssueLog iLog)
    throws SCRDescriptorException {
        // if this reference is already checked, return immediately
        if ( this.checked ) {
            return;
        }
        final int currentIssueCount = iLog.getNumberOfErrors();

        // validate name
        if (StringUtils.isEmpty(this.name)) {
            this.logError( iLog, "Reference has no name" );
        }

        // validate interface
        if (StringUtils.isEmpty(this.interfacename)) {
            this.logError( iLog, "Missing interface name" );
        }

        // validate cardinality
        if (this.cardinality == null) {
            this.cardinality = "1..1";
        } else if (!"0..1".equals(this.cardinality) && !"1..1".equals(this.cardinality)
            && !"0..n".equals(this.cardinality) && !"1..n".equals(this.cardinality)) {
            this.logError( iLog, "Invalid Cardinality specification " + this.cardinality );
        }

        // validate policy
        if (this.policy == null) {
            this.policy = "static";
        } else if (!"static".equals(this.policy) && !"dynamic".equals(this.policy)) {
            this.logError( iLog, "Invalid Policy specification " + this.policy );
        }

        // validate strategy
        if (this.strategy == null) {
            this.strategy = Constants.REFERENCE_STRATEGY_EVENT;
        } else if (!Constants.REFERENCE_STRATEGY_EVENT.equals(this.strategy)
                   && !Constants.REFERENCE_STRATEGY_LOOKUP.equals(this.strategy)) {
            this.logError( iLog, "Invalid strategy type " + this.strategy );
        }

        // validate bind and unbind methods
        if (!isLookupStrategy()) {
            // set default values
            if ( this.bind == null ) {
                this.setBind("bind");
            }
            if ( this.unbind == null ) {
                this.setUnbind("unbind");
            }
            final String oldBind = this.bind;
            final String oldUnbind = this.unbind;
            this.bind = this.validateMethod(specVersion, this.bind, componentIsAbstract, iLog);
            this.unbind = this.validateMethod(specVersion, this.unbind, componentIsAbstract, iLog);
            if ( iLog.getNumberOfErrors() == currentIssueCount ) {
                if ( this.bind != null && this.unbind != null ) {
                    // no errors, so we're checked
                    this.checked = true;
                } else {
                    if ( this.bind == null ) {
                        this.bind = oldBind;
                    }
                    if ( this.unbind == null ) {
                        this.unbind = oldUnbind;
                    }
                }
            }
        } else {
            this.bind = null;
            this.unbind = null;
        }

        // validate updated method
        if ( this.updated != null ) {
            if ( specVersion < Constants.VERSION_1_1_FELIX ) {
                this.logError( iLog, "Updated method declaration requires namespace "
                    + Constants.COMPONENT_DS_SPEC_VERSION_11_FELIX + " or newer" );
            }
        }
    }

    protected String validateMethod(final int      specVersion,
                                    final String   methodName,
                                    final boolean  componentIsAbstract,
                                    final IssueLog iLog)
    throws SCRDescriptorException {
        final JavaMethod method = this.findMethod(specVersion, methodName);
        if (method == null) {
            if ( !componentIsAbstract ) {
                this.logError( iLog, "Missing method " + methodName + " for reference " + this.getName() );
            }
            return null;
        }

        // method needs to be protected for 1.0
        if ( specVersion == Constants.VERSION_1_0 ) {
            if (method.isPublic()) {
                this.logWarn( iLog, "Method " + method.getName() + " should be declared protected" );
            } else if (!method.isProtected()) {
                this.logError( iLog, "Method " + method.getName() + " has wrong qualifier, public or protected required" );
                return null;
            }
        }
        return method.getName();
    }

    private static final String TYPE_SERVICE_REFERENCE = "org.osgi.framework.ServiceReference";
    private static final String TYPE_MAP = "java.util.Map";

    public JavaMethod findMethod(final int    specVersion,
                                 final String methodName)
    throws SCRDescriptorException {
        final String[] sig = new String[]{ TYPE_SERVICE_REFERENCE };
        final String[] sig2 = new String[]{ this.getInterfacename() };
        final String[] sig3 = new String[]{ this.getInterfacename(), TYPE_MAP};

        // service interface or ServiceReference first
        String realMethodName = methodName;
        JavaMethod method = this.javaClassDescription.getMethodBySignature(realMethodName, sig);
        if (method == null) {
            method = this.javaClassDescription.getMethodBySignature(realMethodName, sig2);
            if ( specVersion >= Constants.VERSION_1_1 && method == null ) {
                method = this.javaClassDescription.getMethodBySignature(realMethodName, sig3);
            }
        }

        // append reference name with service interface and ServiceReference
        if (method == null) {
            realMethodName = methodName + Character.toUpperCase(this.name.charAt(0))
            + this.name.substring(1);

            method = this.javaClassDescription.getMethodBySignature(realMethodName, sig);
        }
        if (method == null) {
            method = this.javaClassDescription.getMethodBySignature(realMethodName, sig2);
            if ( specVersion >= Constants.VERSION_1_1 && method == null ) {
                method = this.javaClassDescription.getMethodBySignature(realMethodName, sig3);
            }
        }

        // append type name with service interface and ServiceReference
        if (method == null) {
            int lastDot = this.getInterfacename().lastIndexOf('.');
            realMethodName = methodName
                + this.getInterfacename().substring(lastDot + 1);
            method = this.javaClassDescription.getMethodBySignature(realMethodName, sig);
        }
        if (method == null) {
            method = this.javaClassDescription.getMethodBySignature(realMethodName, sig2);
            if ( specVersion >= Constants.VERSION_1_1 && method == null ) {
                method = this.javaClassDescription.getMethodBySignature(realMethodName, sig3);
            }
        }

        return method;
    }

}
