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
import java.util.List;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.handlers.dependency.nullable.NullableObjectWriter;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.ServiceReference;

/**
 * The dependency handler manages a list of dependencies.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyHandler extends Handler {

    /**
     * The instance manager using this handler.
     */
    private InstanceManager m_manager;

    /**
     * List of depednencies of the component.
     */
    private Dependency[] m_dependencies = new Dependency[0];

    /**
     * List of nullable class for optional dependencies.
     */
    private Class[] m_nullableClasses = new Class[0];

    /**
     * State of the handler.
     */
    private int m_state;

    // ===================== Fields getters & setters =====================

    /**
     * Add a dependency.
     * 
     * @param dep : the dependency to add
     */
    private void addDependency(Dependency dep) {
        for (int i = 0; (m_dependencies != null) && (i < m_dependencies.length); i++) {
            if (m_dependencies[i] == dep) {
                return;
            }
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
     * Add a nullable class.
     * 
     * @param clazz : the class to add
     */
    private void addNullableClass(Class clazz) {
        for (int i = 0; (m_nullableClasses != null) && (i < m_nullableClasses.length); i++) {
            if (m_nullableClasses[i] == clazz) {
                return;
            }
        }
        if (m_nullableClasses.length > 0) {
            Class[] newClass = new Class[m_nullableClasses.length + 1];
            System.arraycopy(m_nullableClasses, 0, newClass, 0, m_nullableClasses.length);
            newClass[m_nullableClasses.length] = clazz;
            m_nullableClasses = newClass;
        } else {
            m_nullableClasses = new Class[] { clazz };
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
     * Get the instance manager.
     * @return the instance manager
     */
    protected InstanceManager getInstanceManager() {
        return m_manager;
    }

    // ===================== Handler implementation =====================

    /**
     * Check the validity of the dependencies.
     */
    protected void checkContext() {
        synchronized (this) {
            // Store the initial state
            int initialState = m_state;

            // Check the component dependencies
            if (validateComponentDependencies()) {
                // The dependencies are valid
                if (initialState == InstanceManager.INVALID) {
                    // There is a state change
                    m_state = InstanceManager.VALID;
                    m_manager.checkInstanceState();
                }
                // Else do nothing, the component state stay VALID
            } else {
                // The dependencies are not valid
                if (initialState == InstanceManager.VALID) {
                    // There is a state change
                    m_state = InstanceManager.INVALID;
                    m_manager.checkInstanceState();
                }
                // Else do nothing, the component state stay UNRESOLVED
            }

        }
    }

    /**
     * Check if the dependency given is valid in the sense that metadata are
     * consistent.
     * 
     * @param dep : the dependency to check
     * @param manipulation : the component-type manipulation metadata
     * @return true if the dependency is valid
     */
    private boolean checkDependency(Dependency dep, ManipulationMetadata manipulation) {
        // Check the internal type of dependency
        String field = dep.getField();
        DependencyCallback[] callbacks = dep.getCallbacks();
        
        for (int i = 0; i < callbacks.length; i++) {
            MethodMetadata[] mets = manipulation.getMethods(callbacks[i].getMethodName());
            if (mets.length == 0) {
                getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "A dependency callback " + callbacks[i].getMethodName() + " does not exist in the implementation");
                return false;
            }
            if (mets[0].getMethodArguments().length > 1) {
                getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "A dependency callback " + callbacks[i].getMethodName() + " must have 0 or 1 argument");
                return false;
            }
            if (mets[0].getMethodArguments().length == 0) {
                callbacks[i].setArgument("EMPTY");
            } else {
                callbacks[i].setArgument(mets[0].getMethodArguments()[0]);
                if (!mets[0].getMethodArguments()[0].equals(ServiceReference.class.getName())) {
                    if (dep.getSpecification() == null) {
                        dep.setSpecification(mets[0].getMethodArguments()[0]);
                    }
                    if (!dep.getSpecification().equals(mets[0].getMethodArguments()[0])) {
                        m_manager.getFactory().getLogger().log(Logger.WARNING, "[DependencyHandler on " + m_manager.getClassName() + "] The field type [" + mets[0].getMethodArguments()[0] + "] and the needed service interface ["
                                    + dep.getSpecification() + "] are not the same");
                        dep.setSpecification(mets[0].getMethodArguments()[0]);
                    }
                }
            }   
        }
        
        if (field != null) {
            FieldMetadata fm = manipulation.getField(field);
            if (fm == null) {
                getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "A dependency field " + field + " does not exist in the implementation class");
                return false;
            }
            String type = fm.getFieldType(); 
            if (type.endsWith("[]")) {
                // Set the dependency to multiple
                dep.setAggregate();
                type = type.substring(0, type.length() - 2);
            }

            if (dep.getSpecification() == null) { dep.setSpecification(type); }

            if (!dep.getSpecification().equals(type)) {
                m_manager.getFactory().getLogger().log(Logger.WARNING, "[DependencyHandler on " + m_manager.getClassName() + "] The field type [" + type + "] and the needed service interface ["
                                + dep.getSpecification() + "] are not the same");
                dep.setSpecification(type);
            }
        }
        
        //Check that all required info are set
        return dep.getSpecification() != null;
    }

    /**
     * Configure the handler.
     * @param im : the instance manager
     * @param componentMetadata : the component type metadata
     * @param configuration : the instance configuration
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(InstanceManager im, Element componentMetadata, Dictionary configuration) {
        m_manager = im;
        m_dependencies = new Dependency[0];
        m_nullableClasses = new Class[0];
        
        ManipulationMetadata manipulation = new ManipulationMetadata(componentMetadata);
        List fl = new ArrayList();

        // Create the dependency according to the component metadata
        Element[] deps = componentMetadata.getElements("Requires"); 
        
        // DEPRECATED BLOCK :
        if (deps.length == 0) {
            deps = componentMetadata.getElements("Dependency");
            if (deps.length != 0) {
                im.getFactory().getLogger().log(Logger.WARNING, "Dependency is deprecated, please use 'requires' instead of 'dependency'");
            }
        }
        // END OF DEPRECATED BLOCK

         
        for (int i = 0; i < deps.length; i++) {
            // Create the dependency metadata
            String field = null;
            if (deps[i].containsAttribute("field")) {
                field = deps[i].getAttribute("field");
            }
            String serviceSpecification = null;
            if (deps[i].containsAttribute("interface")) {
                serviceSpecification = deps[i].getAttribute("interface");
            }
            String filter = "";
            if (deps[i].containsAttribute("filter")) {
                filter = deps[i].getAttribute("filter");
            }
            boolean optional = false;
            if (deps[i].containsAttribute("optional") && deps[i].getAttribute("optional").equals("true")) {
                optional = true;
            }
            boolean aggregate = false;
            if (deps[i].containsAttribute("aggregate") && deps[i].getAttribute("aggregate").equals("true")) {
                aggregate = true;
            }

            Dependency dep = new Dependency(this, field, serviceSpecification, filter, optional, aggregate);
            
            // Look for dependency callback :
            for (int j = 0; j < (deps[i].getElements("Callback", "")).length; j++) {
                String method = deps[i].getElements("Callback", "")[j].getAttribute("method");
                String type = deps[i].getElements("Callback", "")[j].getAttribute("type");
                int methodType = 0;
                if (type.equals("bind")) {
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
            } else {
                m_manager.getFactory().getLogger().log(Logger.ERROR,
                        "[DependencyHandler on " + m_manager.getClassName() + "] The dependency on " + dep.getField() + " is not valid");
            }

        }

        if (deps.length > 0) {
            m_manager.register(this, (FieldMetadata[]) fl.toArray(new FieldMetadata[0]), null);
        }
    }

    /**
     * Create a nullable class for the given dependency.
     * 
     * @param dep : the dependency
     */
    private void createNullableClass(Dependency dep) {
        m_manager.getFactory().getLogger().log(Logger.INFO,
                "[DependencyHandler on " + m_manager.getClassName() + "] Try to load the nullable class for " + dep.getSpecification());

        // String[] segment =
        // dep.getMetadata().getServiceSpecification().split("[.]");
        // String className = "org/apache/felix/ipojo/" + segment[segment.length
        // - 1] + "Nullable";
        String className = dep.getSpecification() + "Nullable";
        String resource = dep.getSpecification().replace('.', '/') + ".class";
        URL url = m_manager.getContext().getBundle().getResource(resource);

        try {
            byte[] b = NullableObjectWriter.dump(url, dep.getSpecification());
            Class c = null;
            try {
                c = m_manager.getFactory().defineClass(className, b, null);
            } catch (Exception e) {
                m_manager.getFactory().getLogger().log(Logger.ERROR, "Cannot define the nullable class : " + e.getMessage());
                return;
            }
            addNullableClass(c);
            m_manager.getFactory().getLogger().log(Logger.INFO,
                    "[DependencyHandler on " + m_manager.getClassName() + "] Nullable class created for " + dep.getSpecification());
        } catch (Exception e2) {
            m_manager.getFactory().getLogger().log(Logger.ERROR,
                    "[DependencyHandler on " + m_manager.getClassName() + "] Cannot load the nullable class for  " + dep.getSpecification(), e2);
        }
    }

    /**
     * Return the nullable class corresponding to the given name.
     * 
     * @param name the needed type
     * @return the class correspondig to the name, or null if the class does not
     * exist.
     */
    protected Class getNullableClass(String name) {
        for (int i = 0; i < m_nullableClasses.length; i++) {
            Class c = m_nullableClasses[i];
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
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
            if (dep.getField().equals(fieldName)) {
                // The field name is a dependency, return the get
                return dep.get();
            }
        }
        // Else return the value
        return value;
    }

    /**
     * Check the handler validity.
     * @return true if all mandatory dependencies are resolved.
     * @see org.apache.felix.ipojo.Handler#isValid()
     */
    public boolean isValid() {
        return m_state == InstanceManager.VALID;
    }

    /**
     * Handler start method.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        m_manager.getFactory().getLogger().log(Logger.INFO, "[DependencyHandler on " + m_manager.getClassName() + "] Start the dependency handler");

        // Start the dependencies, for optional dependencies create Nullable
        // class
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            if (dep.isOptional() && !dep.isAggregate()) {
                createNullableClass(dep);
            }
            dep.start();
        }
        // Check the state
        m_state = m_manager.getState();
        checkContext();
    }

    /**
     * Handler stateChanged method.
     * @param state : new instance state
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int state) {
        m_state = state;
    }

    /**
     * Handler stop method.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i].stop();
        }
        m_nullableClasses = new Class[0];
    }

    /**
     * Handler createInstance method.
     * This method is overided to allow delayed callback invocation.
     * @param instance : the created object
     * @see org.apache.felix.ipojo.Handler#createInstance(java.lang.Object)
     */
    public void createInstance(Object instance) {
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i].callBindMethod(instance);
        }
    }

    /**
     * Check if dependencies are resolved.
     * @return true if all dependencies are resolved.
     */
    private boolean validateComponentDependencies() {
        boolean valide = true;
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            valide = valide & dep.isSatisfied();
            if (!valide) {
                return false;
            }
        }
        return valide;
    }

    /**
     * Get the dependency handler description.
     * @return the dependency handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        DependencyHandlerDescription dhd = new DependencyHandlerDescription(isValid());
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
