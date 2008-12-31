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
package org.apache.felix.ipojo.handlers.dependency;

import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.DependencyStateListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The dependency handler manages a list of service dependencies.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyHandler extends PrimitiveHandler implements DependencyStateListener {

    /**
     * Dependency field type : Vector
     * The dependency will be injected as a vector.
     */
    protected static final int VECTOR = 2;

    /**
     * Dependency Field Type : List.
     * The dependency will be injected as a list.
     */
    protected static final int LIST = 1;
    
    /**
     * Dependency Field Type : Set.
     * The dependency will be injected as a set.
     */
    protected static final int SET = 3;

    /**
     * List of dependencies of the component.
     */
    private Dependency[] m_dependencies = new Dependency[0];

    /**
     * Is the handler started.
     */
    private boolean m_started;
    
    /**
     * The handler description.
     */
    private DependencyHandlerDescription m_description;

    /**
     * Add a dependency.
     * @param dep : the dependency to add
     */
    private void addDependency(Dependency dep) {
        for (int i = 0; m_dependencies != null && i < m_dependencies.length; i++) {
            if (m_dependencies[i] == dep) {
                return;
            }
        }
        if (m_dependencies == null) {
            m_dependencies = new Dependency[] { dep };
        } else {
            Dependency[] newDep = new Dependency[m_dependencies.length + 1];
            System.arraycopy(m_dependencies, 0, newDep, 0, m_dependencies.length);
            newDep[m_dependencies.length] = dep;
            m_dependencies = newDep;
        }
    }

    /**
     * Get the list of managed dependency.
     * @return the dependency list
     */
    public Dependency[] getDependencies() {
        return m_dependencies;
    }

    /**
     * Validate method. This method is invoked by an AbstractServiceDependency when this dependency becomes RESOLVED.
     * @param dep : the dependency becoming RESOLVED.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#validate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void validate(DependencyModel dep) {
        checkContext();
    }

    /**
     * Invalidate method. This method is invoked by an AbstractServiceDependency when this dependency becomes UNRESOLVED or BROKEN.
     * @param dep : the dependency becoming UNRESOLVED or BROKEN.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#invalidate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void invalidate(DependencyModel dep) {
        setValidity(false);
    }

    /**
     * Check the validity of the dependencies.
     */
    protected void checkContext() {
        if (!m_started) {
            return;
        }
        synchronized (m_dependencies) {
            // Store the initial state
            boolean initialState = getValidity();

            boolean valid = true;
            for (int i = 0; i < m_dependencies.length; i++) {
                Dependency dep = m_dependencies[i];
                if (dep.getState() != Dependency.RESOLVED) {
                    valid = false;
                    break;
                }
            }

            // Check the component dependencies
            if (valid) {
                // The dependencies are valid
                if (!initialState) {
                    // There is a state change
                    setValidity(true);
                }
                // Else do nothing, the component state stay VALID
            } else {
                // The dependencies are not valid
                if (initialState) {
                    // There is a state change
                    setValidity(false);
                }
                // Else do nothing, the component state stay UNRESOLVED
            }

        }
    }

    /**
     * Check if the dependency given is valid in the sense that metadata are consistent.
     * @param dep : the dependency to check
     * @param manipulation : the component-type manipulation metadata
     * @return true if the dependency is valid
     * @throws ConfigurationException : the checked dependency is not correct
     */
    private boolean checkDependency(Dependency dep, PojoMetadata manipulation) throws ConfigurationException {
        // Check the internal type of dependency
        String field = dep.getField();
        DependencyCallback[] callbacks = dep.getCallbacks();

        if (callbacks == null && field == null) {
            throw new ConfigurationException("A service requirement requires at least binding methods or a field");
        }

        for (int i = 0; callbacks != null && i < callbacks.length; i++) {
            MethodMetadata[] mets = manipulation.getMethods(callbacks[i].getMethodName());
            if (mets.length == 0) {
                debug("A requirement callback " + callbacks[i].getMethodName() + " does not exist in the implementation class, will try the super classes");
            } else {
                if (mets[0].getMethodArguments().length > 2) {
                    throw new ConfigurationException("Requirement Callback : A requirement callback "
                            + callbacks[i].getMethodName()
                            + " must have 0, 1 or 2 arguments");
                }

                callbacks[i].setArgument(mets[0].getMethodArguments());

                if (mets[0].getMethodArguments().length == 1) {
                    if (!mets[0].getMethodArguments()[0].equals(ServiceReference.class.getName())) {
                        // The callback receives the service object.
                        setSpecification(dep, mets[0].getMethodArguments()[0], false); // Just warn if a mismatch is discovered.
                    }
                } else if (mets[0].getMethodArguments().length == 2) {
                    // The callback receives service object, service reference. Check that the second argument is a service reference
                    if (!(mets[0].getMethodArguments()[1].equals(ServiceReference.class.getName()) // callback with (service object, service reference)
                           || mets[0].getMethodArguments()[1].equals(Dictionary.class.getName()) // callback with (service object, service properties in a dictionary)
                           || mets[0].getMethodArguments()[1].equals(Map.class.getName()))) { // callback with (service object, service properties in a map)
                        String message =
                                "The requirement callback " + callbacks[i].getMethodName() + " must have a ServiceReference, a Dictionary or a Map as the second argument";
                        throw new ConfigurationException(message);
                    }
                    setSpecification(dep, mets[0].getMethodArguments()[0], false); // Just warn if a mismatch is discovered.
                }
            }

        }

        if (field != null) {
            FieldMetadata meta = manipulation.getField(field);
            if (meta == null) {
                throw new ConfigurationException("Requirement Callback : A requirement field "
                        + field
                        + " does not exist in the implementation class");
            }
            String type = meta.getFieldType();
            if (type.endsWith("[]")) {
                // Set the dependency to multiple
                dep.setAggregate(true);
                type = type.substring(0, type.length() - 2);
            } else if (type.equals(List.class.getName()) || type.equals(Collection.class.getName())) {
                dep.setType(LIST);
                type = null;
            } else if (type.equals(Vector.class.getName())) {
                dep.setType(VECTOR);
                type = null;
            } else if (type.equals(Set.class.getName())) {
                dep.setType(SET);
                type = null;
            } else {
                if (dep.isAggregate()) {
                    throw new ConfigurationException("A required service is not correct : the field "
                            + meta.getFieldName()
                            + " must be an array to support aggregate injections");
                }
            }
            setSpecification(dep, type, true); // Throws an exception if the field type mismatch.
        }

        // Check that all required info are set
        return dep.getSpecification() != null;
    }

    /**
     * Check if we have to set the dependency specification with the given class name.
     * @param dep : dependency to check
     * @param className : class name
     * @param error : set to true to throw an error if the set dependency specification and the given specification are different.
     * @throws ConfigurationException : the specification class cannot be loaded correctly
     */
    private void setSpecification(Dependency dep, String className, boolean error) throws ConfigurationException {
        if (className == null) {
            // No found type (list and vector)
            if (dep.getSpecification() == null) {
                if (error) {
                    throw new ConfigurationException("Cannot discover the required specification for " + dep.getField());
                } else {
                    // If the specification is different, warn that we will override it.
                    info("Cannot discover the required specification for " + dep.getField());
                }
            }
        } else { // In all other case, className is not null.
            if (dep.getSpecification() == null || !dep.getSpecification().getName().equals(className)) {
                if (dep.getSpecification() != null) {
                    if (error) {
                        throw new ConfigurationException("A required service is not correct : the discovered type ["
                            + className
                            + "] and the specified (or already discovered)  service interface ["
                            + dep.getSpecification().getName()
                            + "] are not the same");
                    } else {
                        // If the specification is different, warn that we will override it.
                        warn("["
                            + getInstanceManager().getInstanceName()
                            + "] The field type ["
                            + className
                            + "] and the required service interface ["
                            + dep.getSpecification()
                            + "] are not the same");
                    }
                }
            
                try {
                    dep.setSpecification(getInstanceManager().getContext().getBundle().loadClass(className));
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("The required service interface cannot be loaded : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Configure the handler.
     * @param componentMetadata : the component type metadata
     * @param configuration : the instance configuration
     * @throws ConfigurationException : one dependency metadata is not correct.
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element,
     *      java.util.Dictionary)
     */
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        // getPojoMetadata();
        PojoMetadata manipulation = getFactory().getPojoMetadata();
        boolean atLeastOneField = false;

        // Create the dependency according to the component metadata
        Element[] deps = componentMetadata.getElements("Requires");
        
        // Get instance filters.
        Dictionary filtersConfiguration = (Dictionary) configuration.get("requires.filters");
        Dictionary fromConfiguration = (Dictionary) configuration.get("requires.from");
        
        for (int i = 0; deps != null && i < deps.length; i++) {
            // Create the dependency metadata
            String field = deps[i].getAttribute("field");
            
            String serviceSpecification = deps[i].getAttribute("interface");
            // the 'interface' attribute is deprecated
            if (serviceSpecification != null) {
                warn("The 'interface' attribute is deprecated, use the 'specification' attribute instead");
            } else {
                serviceSpecification = deps[i].getAttribute("specification");
            }
            
            String filter = deps[i].getAttribute("filter");
            String opt = deps[i].getAttribute("optional");
            boolean optional = opt != null && opt.equalsIgnoreCase("true");
            String defaultImplem = deps[i].getAttribute("default-implementation");

            String agg = deps[i].getAttribute("aggregate");
            boolean aggregate = agg != null && agg.equalsIgnoreCase("true");
            String identitity = deps[i].getAttribute("id");
            
            String nul = deps[i].getAttribute("nullable");
            boolean nullable = nul == null || nul.equalsIgnoreCase("true");

            String scope = deps[i].getAttribute("scope");
            BundleContext context = getInstanceManager().getContext(); // Get the default bundle context.
            if (scope != null) {
                // If we are not in a composite, the policy is set to global.
                if (scope.equalsIgnoreCase("global") || ((((IPojoContext) getInstanceManager().getContext()).getServiceContext()) == null)) {
                    context =
                            new PolicyServiceContext(getInstanceManager().getGlobalContext(), getInstanceManager().getLocalServiceContext(),
                                    PolicyServiceContext.GLOBAL);
                } else if (scope.equalsIgnoreCase("composite")) {
                    context =
                            new PolicyServiceContext(getInstanceManager().getGlobalContext(), getInstanceManager().getLocalServiceContext(),
                                    PolicyServiceContext.LOCAL);
                } else if (scope.equalsIgnoreCase("composite+global")) {
                    context =
                            new PolicyServiceContext(getInstanceManager().getGlobalContext(), getInstanceManager().getLocalServiceContext(),
                                    PolicyServiceContext.LOCAL_AND_GLOBAL);
                }
            }

            // Get instance filter if available
            if (filtersConfiguration != null && identitity != null && filtersConfiguration.get(identitity) != null) {
                filter = (String) filtersConfiguration.get(identitity);
            }

            // Compute the 'from' attribute
            String from = deps[i].getAttribute("from");
            if (fromConfiguration != null && identitity != null && fromConfiguration.get(identitity) != null) {
                from = (String) fromConfiguration.get(identitity);
            }
            if (from != null) {
                String fromFilter = "(|(instance.name=" + from + ")(service.pid=" + from + "))";
                if (aggregate) {
                    warn("The 'from' attribute is incompatible with aggregate requirements: only one provider will match : " + fromFilter);
                }
                if (filter != null) {
                    filter = "(&" + fromFilter + filter + ")"; // Append the two filters
                } else {
                    filter = fromFilter;
                }
            }
            
            Filter fil = null;
            if (filter != null) {
                try {
                    fil = getInstanceManager().getContext().createFilter(filter);
                } catch (InvalidSyntaxException e) {
                    throw new ConfigurationException("A requirement filter is invalid : " + filter + " - " + e.getMessage());
                }
            }
            

            Class spec = null;
            if (serviceSpecification != null) {
                spec = DependencyModel.loadSpecification(serviceSpecification, getInstanceManager().getContext());
            }

            int policy = DependencyModel.getPolicy(deps[i]);
            Comparator cmp = DependencyModel.getComparator(deps[i], getInstanceManager().getGlobalContext());
            Dependency dep = new Dependency(this, field, spec, fil, optional, aggregate, nullable, identitity, context, policy, cmp, defaultImplem);

            // Look for dependency callback :
            Element[] cbs = deps[i].getElements("Callback");
            for (int j = 0; cbs != null && j < cbs.length; j++) {
                if (!cbs[j].containsAttribute("method") && cbs[j].containsAttribute("type")) {
                    throw new ConfigurationException("Requirement Callback : a dependency callback must contain a method and a type (bind or unbind) attribute");
                }
                String method = cbs[j].getAttribute("method");
                String type = cbs[j].getAttribute("type");
                int methodType = 0;
                if ("bind".equalsIgnoreCase(type)) {
                    methodType = DependencyCallback.BIND;
                } else {
                    methodType = DependencyCallback.UNBIND;
                }

                DependencyCallback callback = new DependencyCallback(dep, method, methodType);
                dep.addDependencyCallback(callback);
            }

            // Check the dependency :
            if (checkDependency(dep, manipulation)) {
                addDependency(dep);
                if (dep.getField() != null) {
                    getInstanceManager().register(manipulation.getField(dep.getField()), dep);
                    atLeastOneField = true;
                }
            }
        }

        if (atLeastOneField) { // Does register only if we have fields
            MethodMetadata[] methods = manipulation.getMethods();
            for (int i = 0; i < methods.length; i++) {
                for (int j = 0; j < m_dependencies.length; j++) {
                    getInstanceManager().register(methods[i], m_dependencies[j]);
                }
            }
        }
        
        m_description = new DependencyHandlerDescription(this, m_dependencies); // Initialize the description.
    }

    /**
     * Handler start method.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        // Start the dependencies
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
           
            dep.start();
        }
        // Check the state
        m_started = true;
        setValidity(false);
        checkContext();
    }

    /**
     * Handler stop method.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        m_started = false;
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i].stop();
        }
    }

    /**
     * Handler createInstance method. This method is override to allow delayed callback invocation.
     * @param instance : the created object
     * @see org.apache.felix.ipojo.Handler#onCreation(java.lang.Object)
     */
    public void onCreation(Object instance) {
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i].onObjectCreation(instance);
        }
    }

    /**
     * Get the dependency handler description.
     * @return the dependency handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        return m_description;
    }

}
