/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.handlers.dependency;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

import org.apache.felix.ipojo.ComponentManager;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.Activator;
import org.apache.felix.ipojo.handlers.dependency.nullable.NullableObjectWriter;
import org.apache.felix.ipojo.metadata.Element;

/**
 * The dependency handler manages a list of dependencies.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class DependencyHandler implements Handler {

	/**
	 * The component manager using this handler.
	 */
	private ComponentManager m_componentManager;

	/**
	 * List of depednencies of the component.
	 */
	private Dependency[] m_dependencies = new Dependency[0];

	/**
	 * List of nullable class for optional dependencies.
	 */
	private Class[] m_nullableClasses = new Class[0];

	/**
	 * Classloader to use to load nullable classes.
	 */
	private NullableClassloader m_classloader;

	/**
	 * State of the handler.
	 */
	private int m_state;

//	 ===================== Fields getters & setters =====================

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
        }
        else {
        	m_dependencies = new Dependency[] {dep};
        }
	}

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
        }
        else {
        	m_nullableClasses = new Class[] {clazz};
        }
	}

	/**
	 * @return the dependency list
	 */
	public Dependency[] getDependencies() { return m_dependencies; }

	/**
	 * @return the component manager
	 */
	protected ComponentManager getComponentManager() { return m_componentManager; }

//	 ===================== Handler implementation =====================

	/**
	 * Check the validity of the dependencies.
	 */
	protected void checkContext() {

		synchronized (this) {

			Activator.getLogger().log(Level.INFO, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Check Context ...");

			// Store the initial state
			int initialState = m_state;

			// Check the component dependencies
			if (!validateComponentDependencies()) {
				// The dependencies are not valid
				if (initialState == ComponentManager.VALID) {
					//There is a state change
					m_state = ComponentManager.INVALID;
					m_componentManager.check();
				}
				// Else do nothing, the component state stay UNRESOLVED
			}
			else {
				// The dependencies are valid
				if (initialState == ComponentManager.INVALID) {
					//There is a state change
					m_state = ComponentManager.VALID;
					m_componentManager.check();
				}
				// Else do nothing, the component state stay VALID
			}

		}
	}

	private boolean checkDependency(Dependency dep) {
        // Check the internal type of dependency
            String field = dep.getMetadata().getField();

            Element manipulation = m_componentManager.getComponentMetatada().getMetadata().getElements("Manipulation")[0];
        	String type = null;
        	for (int i = 0; i < manipulation.getElements("Field").length; i++) {
        		if (field.equals(manipulation.getElements("Field")[i].getAttribute("name"))) {
        			type = manipulation.getElements("Field")[i].getAttribute("type");
        			break;
        		}
        	}

            if (type == null) {
            	Activator.getLogger().log(Level.SEVERE, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] A declared dependency was not found in the class : " + dep.getMetadata().getField());
            	return false;
            }

            if (type != null) {
                if (type.endsWith("[]")) {
                	// Set the dependency to multiple
                	dep.getMetadata().setMultiple();
                	type = type.substring(0, type.length() - 2);
                	}

                if (dep.getMetadata().getServiceSpecification() == null) { dep.getMetadata().setServiceSpecification(type); }

                if (!dep.getMetadata().getServiceSpecification().equals(type)) {
                    Activator.getLogger().log(Level.WARNING, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] The field type [" + type + "] and the needed service interface [" + dep.getMetadata().getServiceSpecification() + "] are not the same");
                    dep.getMetadata().setServiceSpecification(type);
                }
            }
            else {
                Activator.getLogger().log(Level.WARNING, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] The declared dependency " + dep.getMetadata().getField() + "  does not exist in the code");
            }
		return true;
	}


	/**
	 * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.ComponentManager, org.apache.felix.ipojo.metadata.Element)
	 */
	public void configure(ComponentManager cm, Element componentMetadata) {
		// Fix the component manager
		m_componentManager = cm;
		m_dependencies = new Dependency[0];
		m_nullableClasses = new Class[0];

		// Create the dependency according to the component metadata
		Element[] deps = componentMetadata.getElements("Dependency");
		for (int i = 0; i < deps.length; i++) {
			// Create the dependency metadata
			String field = deps[i].getAttribute("field");
			String serviceSpecification = null;
			if (deps[i].containsAttribute("interface")) { serviceSpecification = deps[i].getAttribute("interface"); }
			String filter = "";
			if (deps[i].containsAttribute("filter")) { filter = deps[i].getAttribute("filter"); }
			boolean optional = false;
			if (deps[i].containsAttribute("optional") && deps[i].getAttribute("optional").equals("true")) { optional = true; }
			DependencyMetadata dm = new DependencyMetadata(field, serviceSpecification, filter, optional);


			Dependency dep = new Dependency(this, dm);
			// Check the dependency :
			if (checkDependency(dep)) { addDependency(dep); }
			else { Activator.getLogger().log(Level.SEVERE, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] The dependency on " + dep.getMetadata().getField() + " is not valid"); }

			// Look for dependency callback :
			for (int j = 0; j < (deps[i].getElements("Callback", "")).length; j++) {
				String method = deps[i].getElements("Callback", "")[j].getAttribute("method");
				String type = deps[i].getElements("Callback", "")[j].getAttribute("type");
				int methodType = 0;
				if (type.equals("bind")) { methodType = DependencyCallback.BIND; }
				else { methodType = DependencyCallback.UNBIND; }
				boolean isStatic = false;
				if (deps[i].getElements("Callback", "")[j].containsAttribute("isStatic") && deps[i].getElements("Callback", "")[j].getAttribute("isStatic").equals("true")) { isStatic = true; }
				DependencyCallback dc = new DependencyCallback(dep, method, methodType, isStatic);
				dep.getMetadata().addDependencyCallback(dc);
			}
		}

		if (deps.length > 0) {
			m_componentManager.register(this);

			// Create the nullable classloader
			m_classloader = new NullableClassloader(m_componentManager.getContext().getBundle());

		}
	}

	private void createNullableClass(Dependency dep) {
		Activator.getLogger().log(Level.INFO, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Try to load the nullable class for " + dep.getMetadata().getServiceSpecification());
		// Try to load the nullable object :
        String[] segment = dep.getMetadata().getServiceSpecification().split("[.]");
        String className = "org/apache/felix/ipojo/" + segment[segment.length - 1] + "Nullable";

        String resource = dep.getMetadata().getServiceSpecification().replace('.', '/') + ".class";
        URL url =  m_componentManager.getContext().getBundle().getResource(resource);

            try {
                byte[] b = NullableObjectWriter.dump(url,  dep.getMetadata().getServiceSpecification());

//                // DEBUG :
//                try {
//                    File file = new File("P:\\workspace\\iPOJO\\adapted\\" + className.replace('/', '.') + ".class");
//                    file.createNewFile();
//                    FileOutputStream fos = new FileOutputStream(file);
//                    fos.write(b);
//                    fos.close();
//                } catch (Exception e3) {
//                    System.err.println("Problem to write the adapted class on the file system  : " + e3.getMessage());
//
//                }

                addNullableClass(m_classloader.defineClass(className.replace('/', '.'), b, null));
                Activator.getLogger().log(Level.INFO, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Nullable class created for " + dep.getMetadata().getServiceSpecification());

            } catch (IOException e1) {
                Activator.getLogger().log(Level.SEVERE, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Cannot open a stream of an interface to generate the nullable class for " + dep.getMetadata().getServiceSpecification() + " -> " + e1.getMessage());
            } catch (Exception e2) {
            	Activator.getLogger().log(Level.SEVERE, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Cannot load the nullable class for  " + dep.getMetadata().getServiceSpecification() + " -> " + e2.getMessage());
            }
	}

	/**
     * Return the nullable class corresponding to the given name.
	 * @param name the needed type
	 * @return the class correspondig to the name, or null if the class does not exist.
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
	 * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String, java.lang.Object)
	 */
	public Object getterCallback(String fieldName, Object value) {
		for (int i = 0; i < m_dependencies.length; i++) {
			Dependency dep = m_dependencies[i];
			if (dep.getMetadata().getField().equals(fieldName)) {
				// The field name is a dependency, return the get
				return dep.get();
			}
		}
		// Else return the value
		return value;
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#isValid()
	 */
	public boolean isValid() {
		return (m_state == ComponentManager.VALID);
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#setterCallback(java.lang.String, java.lang.Object)
	 */
	public void setterCallback(String fieldName, Object value) {
	    // Do nothing
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#start()
	 */
	public void start() {
		Activator.getLogger().log(Level.INFO, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Start the dependency handler");

		// Start the dependencies, for optional dependencies create Nullable class
		for (int i = 0; i < m_dependencies.length; i++) {
			Dependency dep = m_dependencies[i];
			if (dep.getMetadata().isOptional() && !dep.getMetadata().isMultiple()) { createNullableClass(dep); }
			dep.start();
		}
		// Check the state
		m_state = m_componentManager.getState();
		checkContext();
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#stateChanged(int)
	 */
	public void stateChanged(int state) {
		// Another handler or the component manager itself change the state
		if (m_state == ComponentManager.VALID && state == ComponentManager.INVALID) {
			// The component is stopped => Stop the dependency tracking
			stop();
		}
		m_state = state;
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#stop()
	 */
	public void stop() {
		for (int i = 0; i < m_dependencies.length; i++) { m_dependencies[i].stop(); }
	}

	private boolean validateComponentDependencies() {
		boolean valide = true;
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            valide = valide & dep.isSatisfied();
            if (!valide) {
                Activator.getLogger().log(Level.INFO, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Component Dependencies are not valid : " + dep.getMetadata().getServiceSpecification());
                return false;
            }
        }
        Activator.getLogger().log(Level.INFO, "[DependencyHandler on " + m_componentManager.getComponentMetatada().getClassName() + "] Component Dependencies are valid");
        return valide;
	}

}
