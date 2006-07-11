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
package org.apache.felix.ipojo.handlers.providedservice;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.apache.felix.ipojo.Activator;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Represent a property i.e. a set : [name, type, value].
 * A property can be attached to a field.
 * The value of the property is thefield value.
 * When the value change, the published value change too.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Property {

    /**
     * A property is link with a service.
     * This field represent this provided service.
     * m_providedService : ProvidedService
     */
    private ProvidedService m_providedService;

    /**
     * Value of the property (before we know the type).
     */
    private Object m_value;

    /**
     * Metadata of the property.
     */
    private PropertyMetadata m_metadata;

    /**
     * Property constructor.
     * @param ps : the provided service
     * @param pm : metadata of the property
     */
    public Property(ProvidedService ps, PropertyMetadata pm) {
    	m_providedService = ps;
    	m_metadata = pm;

    	// Fix the type of the property if null
    	if (pm.getType() == null) {
    		// If the type is not found, it is a dynamic property
    		Element manipulation = m_providedService.getComponentManager().getComponentMetatada().getMetadata().getElements("Manipulation")[0];
        	String type = null;
        	String field = m_metadata.getField();
        	for (int i = 0; i < manipulation.getElements("Field").length; i++) {
        		if (field.equals(manipulation.getElements("Field")[i].getAttribute("name"))) {
        			type = manipulation.getElements("Field")[i].getAttribute("type");
        			break;
        		}
        	}
    		pm.setType(type);
    	}

    	if (pm.getValue() != null) { setValue(pm.getValue()); }
    }

    /**
     * Property constructor.
     * This constructor is used only for non-field property (property not attached to a field).
     * @param ps : the provided service
     * @param name : the name of the property
     * @param value : the value of the property
     */
    public Property(ProvidedService ps, String name, Object value) {
    	m_providedService = ps;
    	m_metadata = new PropertyMetadata(name, null, value.getClass().getName(), null);
    	m_value = value;
    }

    /**
     * @return the Object value of the property
     */
    protected Object get() {
        if (m_value == null) {
            Activator.getLogger().log(Level.INFO, "[" + m_providedService.getComponentManager().getComponentMetatada().getClassName() + "] A property " + m_metadata.getName() + " can not be returned : no value assigned");
        }
        return m_value;
    }


    /**
     * @return the property metadata.
     */
    public PropertyMetadata getMetadata() {
    	return m_metadata;
    }

    /**
     * This method is automaticaly called when the value of the property is changed.
     * Set the value of a property.
     * @param s : the new value of the property (in String)
     */
    protected void set(String s) {
        setValue(s);
        m_providedService.update();
    }

    /**
     * This method is called when the value of the property is changed.
     * Set the value of a property.
     * @param o : the new value of the property (object)
     */
    protected void set(Object o) {
        m_value = o;
        m_providedService.update();
    }

    /**
     * Set the provided service of this property.
     * @param ps : the provided service to attached.
     */
    void setProvidedService(ProvidedService ps) {
        m_providedService = ps;
    }

    /**
     * Set the value of the property.
     * @param value : value of the property (String)
     */
    private void setValue(String value) {
    	String type = m_metadata.getType();

    	// Array :
    	if (type.endsWith("[]")) {
    		String internalType = type.substring(0, type.length() - 2);
    		value = value.substring(1, value.length() - 1);
    		String[] values = value.split(",");
    		setArrayValue(internalType, values);
    		return;
    	}

    	// Simple :
    	Activator.getLogger().log(Level.INFO, "[" + m_providedService.getComponentManager().getComponentMetatada().getClassName() + "] Set the value of the property " + m_metadata.getName() + " [" + m_metadata.getType() + "] " + " with the value : " + value);

        if (type.equals("string") || type.equals("String")) { m_value = new String(value); return; }
        if (type.equals("boolean")) { m_value = new Boolean(value); return; }
        if (type.equals("byte")) { m_value = new Byte(value); return; }
        if (type.equals("short")) { m_value = new Short(value); return; }
        if (type.equals("int")) { m_value = new Integer(value); return; }
        if (type.equals("long")) { m_value = new Long(value); return; }
        if (type.equals("float")) { m_value = new Float(value); return; }
        if (type.equals("double")) { m_value = new Double(value); return; }

        // Else it is a neither a primitive type neither a String -> create the object by calling a constructor with a string in argument.
        try {
            Class c = m_providedService.getComponentManager().getContext().getBundle().loadClass(type);
            //Class string = m_providedService.getComponentManager().getContext().getBundle().loadClass("java.lang.String");
            Constructor cst = c.getConstructor(new Class[] {String.class});
            m_value = cst.newInstance(new Object[] {value});
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found exception in setValue on " + type);
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("Constructor not found exeption in setValue on " + type);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Argument problem to call the constructor of the type " + type);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Instantiation problem  " + type);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Invocation problem " + type);
            e.printStackTrace();
        }
    }

    private void setArrayValue(String internalType, String[] values) {
    	 if (internalType.equals("string") || internalType.equals("String")) { m_value = values; return; }
         if (internalType.equals("boolean")) {
        	 boolean[] bool = new boolean[values.length];
        	 for (int i = 0; i < values.length; i++) { bool[i] = new Boolean(values[i]).booleanValue(); }
        	 m_value = bool;
        	 return;
        }
        if (internalType.equals("byte")) {
        	byte[] byt = new byte[values.length];
       	 	for (int i = 0; i < values.length; i++) { byt[i] = new Byte(values[i]).byteValue(); }
       	 	m_value = byt;
        	return;
        }
         if (internalType.equals("short")) {
        	 short[] shor = new short[values.length];
        	 for (int i = 0; i < values.length; i++) { shor[i] = new Short(values[i]).shortValue(); }
        	 m_value = shor;
        	 return;
        }
         if (internalType.equals("int")) {
        	 int[] in = new int[values.length];
        	 for (int i = 0; i < values.length; i++) { in[i] = new Integer(values[i]).intValue(); }
        	 m_value = in;
        	 return;
        }
         if (internalType.equals("long")) {
        	 long[] ll = new long[values.length];
        	 for (int i = 0; i < values.length; i++) { ll[i] = new Long(values[i]).longValue(); }
        	 m_value = ll;
        	 return;
        }
         if (internalType.equals("float")) {
        	 float[] fl = new float[values.length];
        	 for (int i = 0; i < values.length; i++) { fl[i] = new Float(values[i]).floatValue(); }
        	 m_value = fl;
        	 return; }
         if (internalType.equals("double")) {
        	 double[] dl = new double[values.length];
        	 for (int i = 0; i < values.length; i++) { dl[i] = new Double(values[i]).doubleValue(); }
        	 m_value = dl;
        	 return; }

         // Else it is a neither a primitive type neither a String -> create the object by calling a constructor with a string in argument.
         try {
             Class c = m_providedService.getComponentManager().getContext().getBundle().loadClass(internalType);
             Constructor cst = c.getConstructor(new Class[] {String.class});
             Object[] ob = (Object[]) Array.newInstance(c, values.length);
             for (int i = 0; i < values.length; i++) {
            	 ob[i] = cst.newInstance(new Object[] {values[i]});
             }
             m_value = ob;
             return;
         } catch (ClassNotFoundException e) {
             System.err.println("Class not found exception in setValue on " + internalType);
             e.printStackTrace();
         } catch (SecurityException e) {
             e.printStackTrace();
         } catch (NoSuchMethodException e) {
             System.err.println("Constructor not found exeption in setValue on " + internalType);
             e.printStackTrace();
         } catch (IllegalArgumentException e) {
             System.err.println("Argument problem to call the constructor of the type " + internalType);
             e.printStackTrace();
         } catch (InstantiationException e) {
             System.err.println("Instantiation problem  " + internalType);
             e.printStackTrace();
         } catch (IllegalAccessException e) {
             e.printStackTrace();
         } catch (InvocationTargetException e) {
             System.err.println("Invocation problem " + internalType);
             e.printStackTrace();
         }
	}

	/**
     * @return the value of the property.
     */
    public Object getValue() { return m_value; }
}
