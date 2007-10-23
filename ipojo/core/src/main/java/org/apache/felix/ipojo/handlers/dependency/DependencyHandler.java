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

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.handlers.dependency.nullable.NullableObjectWriter;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.ServiceReference;

/**
 * The dependency handler manages a list of service dependencies.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyHandler extends PrimitiveHandler {

    /**
     * List of dependencies of the component.
     */
    private Dependency[] m_dependencies = new Dependency[0];

    /**
     * Map of dependency - nullable objects for optional dependencies.
     */
    private Map m_nullableObjects;

    /**
     * State of the handler.
     * Lifecycle controller.
     */
    private boolean m_state;

    /**
     * Is the handler started.
     */
    private boolean m_started;

    /**
     * Add a dependency.
     * @param dep : the dependency to add
     */
    private void addDependency(Dependency dep) {
        for (int i = 0; i < m_dependencies.length; i++) {
            if (m_dependencies[i] == dep) { return; }
        }
        if (m_dependencies.length > 0) {
            Dependency[] newDep = new Dependency[m_dependencies.length + 1];
            System.arraycopy(m_dependencies, 0, newDep, 0, m_dependencies.length);
            newDep[m_dependencies.length] = dep;
            m_dependencies = newDep;
        } else {
            m_dependencies = new Dependency[] { dep };
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
     * Check the validity of the dependencies.
     */
    protected void checkContext() {
        if (! m_started) { return; }
        synchronized (m_dependencies) {
            // Store the initial state
            boolean initialState = m_state;

            boolean valid = true;
            for (int i = 0; i < m_dependencies.length; i++) {
                Dependency dep = m_dependencies[i];
                if (dep.getState() > Dependency.RESOLVED) { 
                    valid = false;
                    break;
                }
            }
            
            // Check the component dependencies
            if (valid) {
                // The dependencies are valid
                if (!initialState) {
                    // There is a state change
                    m_state = true;
                }
                // Else do nothing, the component state stay VALID
            } else {
                // The dependencies are not valid
                if (initialState) {
                    // There is a state change
                    m_state = false;
                }
                // Else do nothing, the component state stay UNRESOLVED
            }

        }
    }

    /**
     * Check if the dependency given is valid in the sense that metadata are
     * consistent.
     * @param dep : the dependency to check
     * @param manipulation : the component-type manipulation metadata
     * @return true if the dependency is valid
     * @throws ConfigurationException : the checked dependency is not correct
     */
    private boolean checkDependency(Dependency dep, ManipulationMetadata manipulation) throws ConfigurationException {
        // Check the internal type of dependency
        String field = dep.getField();
        DependencyCallback[] callbacks = dep.getCallbacks();

        for (int i = 0; i < callbacks.length; i++) {
            MethodMetadata[] mets = manipulation.getMethods(callbacks[i].getMethodName());
            if (mets.length != 0) {
                if (mets[0].getMethodArguments().length > 2) {
                    throw new ConfigurationException("Requirement Callback : A requirement callback " + callbacks[i].getMethodName() + " must have 0 or 1 or 2 arguments");
                }
                
                callbacks[i].setArgument(mets[0].getMethodArguments());
                if (mets[0].getMethodArguments().length == 1) {
                    if (!mets[0].getMethodArguments()[0].equals(ServiceReference.class.getName())) {
                        if (dep.getSpecification() == null) {
                            dep.setSpecification(mets[0].getMethodArguments()[0]);
                        }
                        if (!dep.getSpecification().equals(mets[0].getMethodArguments()[0])) {
                            log(Logger.WARNING, "[DependencyHandler on " + getInstanceManager().getInstanceName() + "] The field type [" + mets[0].getMethodArguments()[0] + "] and the needed service interface [" + dep.getSpecification() + "] are not the same");
                            dep.setSpecification(mets[0].getMethodArguments()[0]);
                        }
                    }
                } else if (mets[0].getMethodArguments().length == 2) {
                    // Check that the second arguments is a service reference
                    if (!mets[0].getMethodArguments()[1].equals(ServiceReference.class.getName())) {
                        String message = "The requirement callback " + callbacks[i].getMethodName() + " must have a ServiceReference as the second arguments";
                        throw new ConfigurationException(message);
                    }
                    if (dep.getSpecification() == null) {
                        dep.setSpecification(mets[0].getMethodArguments()[0]);
                    } else {
                        if (!dep.getSpecification().equals(mets[0].getMethodArguments()[0])) {
                            log(Logger.WARNING, "[DependencyHandler on " + getInstanceManager().getInstanceName() + "] The field type [" + mets[0].getMethodArguments()[0] + "] and the needed service interface [" + dep.getSpecification() + "] are not the same");
                            dep.setSpecification(mets[0].getMethodArguments()[0]);
                        }
                    }
                }
            } else {
                log(Logger.INFO, "A requirement callback " + callbacks[i].getMethodName() + " does not exist in the implementation, try the super classes");
            }

        }

        if (field != null) {
            FieldMetadata fm = manipulation.getField(field);
            if (fm == null) {
                throw new ConfigurationException("Requirement Callback : A requirement field " + field + " does not exist in the implementation class");
            }
            String type = fm.getFieldType();
            if (type.endsWith("[]")) {
                // Set the dependency to multiple
                dep.setAggregate();
                type = type.substring(0, type.length() - 2);
            }

            if (dep.getSpecification() == null) {
                dep.setSpecification(type);
            }

            if (!dep.getSpecification().equals(type)) {
                log(Logger.WARNING, "[DependencyHandler on " + getInstanceManager().getInstanceName() + "] The field type [" + type + "] and the needed service interface [" + dep.getSpecification() + "] are not the same");
                dep.setSpecification(type);
            }
        }

        //Check that all required info are set
        return dep.getSpecification() != null;
    }

    /**
     * Configure the handler.
     * @param componentMetadata : the component type metadata
     * @param configuration : the instance configuration
     * @throws ConfigurationException : one dependency metadata is not correct.
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        m_dependencies = new Dependency[0];
        m_nullableObjects = new HashMap();

        ManipulationMetadata manipulation = new ManipulationMetadata(componentMetadata);
        List fl = new ArrayList();

        // Create the dependency according to the component metadata
        Element[] deps = componentMetadata.getElements("Requires");

        // Get instance filters.
        Dictionary filtersConfiguration = (Dictionary) configuration.get("requires.filters");
        
        for (int i = 0; i < deps.length; i++) {
            // Create the dependency metadata
            String field = deps[i].getAttribute("field");
            String serviceSpecification = deps[i].getAttribute("interface");
            String filter = deps[i].getAttribute("filter");
            String opt = deps[i].getAttribute("optional");
            boolean optional = opt != null && opt.equalsIgnoreCase("true");
            
            String agg = deps[i].getAttribute("aggregate");
            boolean aggregate = agg != null && agg.equalsIgnoreCase("true");
            String id = deps[i].getAttribute("id");

            int scopePolicy = -1;
            String scope = deps[i].getAttribute("scope"); 
            if (scope != null) {
                if (scope.equalsIgnoreCase("global")) {
                    scopePolicy = PolicyServiceContext.GLOBAL;
                } else if (scope.equalsIgnoreCase("composite")) {
                    scopePolicy = PolicyServiceContext.LOCAL;
                } else if (scope.equalsIgnoreCase("composite+global")) {
                    scopePolicy = PolicyServiceContext.LOCAL_AND_GLOBAL;
                }
            }

            // Get instance filter if available
            if (filtersConfiguration != null && id != null && filtersConfiguration.get(id) != null) {
                filter = (String) filtersConfiguration.get(id);
            }
            
            
            // Parse binding policy
            int bindingPolicy = Dependency.DYNAMIC_POLICY;
            String policy = deps[i].getAttribute("policy"); 
            if (policy != null) {
                if (policy.equalsIgnoreCase("static")) {
                    bindingPolicy = Dependency.STATIC_POLICY;
                } else if (policy.equalsIgnoreCase("dynamic")) {
                    bindingPolicy = Dependency.DYNAMIC_POLICY;
                } else if (policy.equalsIgnoreCase("dynamic-priority")) {
                    bindingPolicy = Dependency.DYNAMIC_PRIORITY_POLICY;
                }
            }
            
            Dependency dep = new Dependency(this, field, serviceSpecification, filter, optional, aggregate, id, scopePolicy, bindingPolicy);

            // Look for dependency callback :
            for (int j = 0; j < (deps[i].getElements("Callback", "")).length; j++) {
                if (!(deps[i].getElements("Callback", "")[j].containsAttribute("method") && deps[i].getElements("Callback", "")[j].containsAttribute("type"))) { 
                    throw new ConfigurationException("Requirement Callback : a dependency callback must contain a method and a type attribute"); 
                }
                String method = deps[i].getElements("Callback", "")[j].getAttribute("method");
                String type = deps[i].getElements("Callback", "")[j].getAttribute("type");
                int methodType = 0;
                if ("bind".equalsIgnoreCase(type)) {
                    methodType = DependencyCallback.BIND;
                } else {
                    methodType = DependencyCallback.UNBIND;
                }

                DependencyCallback dc = new DependencyCallback(dep, method, methodType);
                dep.addDependencyCallback(dc);
            }

            // Check the dependency :
            if (checkDependency(dep, manipulation)) {
                addDependency(dep);
                if (dep.getField() != null) {
                    fl.add(manipulation.getField(dep.getField()));
                }
            }
            
            if (optional) {
                String defaultImpl = deps[i].getAttribute("default-implementation");
                if (defaultImpl != null) {
                    m_nullableObjects.put(dep, defaultImpl);
                }
            }
        }

        if (deps.length > 0) {
            getInstanceManager().register(this, (FieldMetadata[]) fl.toArray(new FieldMetadata[0]), manipulation.getMethods());
        } else {
            throw new ConfigurationException("No dependencies found in " + getInstanceManager().getInstanceName());
        }
    }

    /**
     * Create a nullable class for the given dependency.
     * @param dep : the service dependency
     */
    private void createNullableObject(Dependency  dep) {
        String spec = dep.getSpecification();
        String className = spec + "Nullable";
        String resource = spec.replace('.', '/') + ".class";
        URL url = getInstanceManager().getContext().getBundle().getResource(resource);

        byte[] b = NullableObjectWriter.dump(url, spec);
        Class c = getInstanceManager().getFactory().defineClass(className, b, null);
        try {
            Object o = c.newInstance();
            m_nullableObjects.put(dep, o);
        } catch (InstantiationException e) {
            log(Logger.ERROR, "The nullable object for " + dep.getSpecification() + " cannot be instantiate : " + e.getMessage());
            getInstanceManager().stop(); 
        } catch (IllegalAccessException e) {
            log(Logger.ERROR, "The nullable object for " + dep.getSpecification() + " cannot be instantiate : " + e.getMessage());
            getInstanceManager().stop();
        }
    }

    /**
     * Return the nullable class corresponding to the given name.
     * @param dep the dependency which require the nullable class.
     * @return the class corresponding to the name, or null if the class does not exist.
     */
    protected Object getNullableObject(Dependency dep) {
        Object obj = m_nullableObjects.get(dep);
        if (obj == null) { return null; } // Should not happen
        if (obj instanceof String) { 
            try {
                Class c = getInstanceManager().getContext().getBundle().loadClass((String) obj);
                obj = c.newInstance();
                m_nullableObjects.put(dep, obj);
                return obj;
            } catch (ClassNotFoundException e) {
                // A default-implementation class cannot be loaded
                log(Logger.ERROR, "The default-implementation class " + obj + " cannot be loaded : " + e.getMessage());
                getInstanceManager().stop();
                return null;
            } catch (InstantiationException e) {
                log(Logger.ERROR, "The default-implementation class " + obj + " cannot be instantiated : " + e.getMessage());
                getInstanceManager().stop();
            } catch (IllegalAccessException e) {
                log(Logger.ERROR, "The default-implementation class " + obj + " cannot be instantiated : " + e.getMessage());
                getInstanceManager().stop();
            }
            return null;
        } else {
            return obj;
        }
    }

    /**
     * GetterCallback Method.
     * @param fieldName : the field name.
     * @param value : the value passed to the field (by the previous handler).
     * @return the object that the dependency handler want to push.
     * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String, java.lang.Object)
     */
    public Object getterCallback(String fieldName, Object value) {
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            String field = dep.getField();
            if (field != null && field.equals(fieldName)) {
                // The field name is a dependency, return the get
                return dep.get();
            }
        }
        // Else return the value
        return value;
    }

    /**
     * Method Entry callback.
     * @param methodId : method Id.
     * @see org.apache.felix.ipojo.Handler#entryCallback(java.lang.String)
     */
    public void entryCallback(String methodId) {
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            if (dep.getField() != null) {
                dep.entry(methodId);
            }
        }
    }

    /**
     * Method Exit callback.
     * @param methodId : method id.
     * @param returnedObj : returned object.
     * @see org.apache.felix.ipojo.Handler#exitCallback(java.lang.String, java.lang.Object)
     */
    public void exitCallback(String methodId, Object returnedObj) {
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            if (dep.getField() != null) {
                dep.exit(methodId);
            }
        }
    }

    /**
     * Handler start method.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        // Start the dependencies, for optional dependencies create Nullable class
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            if (dep.isOptional() && !dep.isAggregate() && ! m_nullableObjects.containsKey(dep)) {
                createNullableObject(dep);
            }
            dep.start();
        }
        // Check the state
        m_started = true;
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
     * Handler createInstance method.
     * This method is override to allow delayed callback invocation.
     * @param instance : the created object
     * @see org.apache.felix.ipojo.Handler#objectCreated(java.lang.Object)
     */
    public void objectCreated(Object instance) {
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i].callBindMethod(instance);
        }
    }
    
    /**
     * The instance state changes. If the new state is valid, we need to activate dependencies.
     * @param state : the new state
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int state) {
        if (state == ComponentInstance.VALID) {
            for (int i = 0; i < m_dependencies.length; i++) {
                m_dependencies[i].activate();
            }
        }
    }

    /**
     * Get the dependency handler description.
     * @return the dependency handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        DependencyHandlerDescription dhd = new DependencyHandlerDescription(this);
        for (int j = 0; j < getDependencies().length; j++) {
            Dependency dep = getDependencies()[j];
            // Create & add the dependency description
            DependencyDescription dd = new DependencyDescription(dep.getSpecification(), dep.isAggregate(), dep.isOptional(), dep.getFilter(), dep.getState());
            dd.setServiceReferences(dep.getServiceReferences());
            dd.setUsedServices(dep.getUsedServices());
            dhd.addDependency(dd);
        }
        return dhd;
    }

}
