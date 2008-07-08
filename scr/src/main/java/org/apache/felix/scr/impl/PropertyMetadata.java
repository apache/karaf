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
package org.apache.felix.scr.impl;

import java.util.*;

/**
 * A property descriptor that contains the information for properties
 * defined in the descriptor
 *
 */
public class PropertyMetadata {

	// Name of the property (required)
	private String m_name;

	// Type of the property (optional)
	private String m_type = "String";

	// Value of the type (optional)
	private Object m_value;

	// Flag that indicates if this PropertyMetadata has been validated and thus has become immutable
	private boolean m_validated = false;

	/**
	 * Set the name
	 *
	 * @param name
	 */
	public void setName(String name) {
		if (m_validated == true) {
			return;
		}

		m_name = name;
	}


	/**
	 * Set the type
	 *
	 * @param type
	 */
	public void setType(String type) {
		if (m_validated == true) {
			return;
		}
		m_type = type;
	}

	/**
	 * Set the value
	 *
	 * @param value
	 */
	public void setValue(String value) {
		if (m_validated == true) {
			return;
		}

        m_value = toType( value );
	}

    /**
     * Set multiple values as an array, where the values are contained in
     * the string as one value per line.
     *
     * @param values
     */
    public void setValues(String values) {
        // splite the values and convert to boxed objects
        List valueList = new ArrayList();
        StringTokenizer tokener = new StringTokenizer(values, "\r\n");
        while (tokener.hasMoreTokens()) {
            String value = tokener.nextToken().trim();
            if (value.length() > 0) {
                valueList.add(toType( value ));
            }
        }

        // 112.4.5 Except for String objects, the result will be translated to an array of primitive types.
        if(m_type.equals("String")) {
            m_value = valueList.toArray( new String[valueList.size()] );
        }
        else if(m_type.equals("Long")) {
            long[] array = new long[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Long) valueList.get(i)).longValue();
            }
            m_value = array;
        }
        else if(m_type.equals("Double")) {
            double[] array = new double[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Double) valueList.get(i)).doubleValue();
            }
            m_value = array;
        }
        else if(m_type.equals("Float")) {
            float[] array = new float[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Float) valueList.get(i)).floatValue();
            }
            m_value = array;
        }
        else if(m_type.equals("Integer")) {
            int[] array = new int[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Integer) valueList.get(i)).intValue();
            }
            m_value = array;
        }
        else if(m_type.equals("Byte")) {
            byte[] array = new byte[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Byte) valueList.get(i)).byteValue();
            }
            m_value = array;
        }
        else if(m_type.equals("Char")) {
            //TODO: verify if this is adequate for Characters
            char[] array = new char[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Character) valueList.get(i)).charValue();
            }
            m_value = array;
        }
        else if(m_type.equals("Boolean")) {
            boolean[] array = new boolean[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Boolean) valueList.get(i)).booleanValue();
            }
            m_value = array;
        }
        else if(m_type.equals("Short")) {
            short[] array = new short[valueList.size()];
            for (int i=0; i < array.length; i++) {
                array[i] = ((Short) valueList.get(i)).shortValue();
            }
            m_value = array;
        }
        else {
            throw new IllegalArgumentException("Undefined property type '"+m_type+"'");
        }
    }

    /**
     * Get the name of the property
     *
     * @return the name of the property
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get the type of the property
     *
     * @return the type of the property
     */
    public String getType() {
        return m_type;
    }

    /**
     * Get the value of the property
     *
     * @return the value of the property as an Object
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Method used to verify if the semantics of this metadata are correct
     */
    public void validate( ComponentMetadata componentMetadata )
    {
        if ( m_name == null )
        {
            throw componentMetadata.validationFailure( "Property name attribute is mandatory" );
        }
    }

    private Object toType(String value) {
        // 112.4.5 Parsing of the value is done by the valueOf(String) method (P. 291)
        // Should the type accept lowercase too?
        if(m_type.equals("String")) {
            return String.valueOf(value);
        }
        else if(m_type.equals("Long")) {
            return Long.valueOf(value);
        }
        else if(m_type.equals("Double")) {
            return Double.valueOf(value);
        }
        else if(m_type.equals("Float")) {
            return Float.valueOf(value);
        }
        else if(m_type.equals("Integer")) {
            return Integer.valueOf(value);
        }
        else if(m_type.equals("Byte")) {
            return Byte.valueOf(value);
        }
        else if(m_type.equals("Char")) {
            char c = ( value.length() > 0 ) ? value.charAt( 0 ) : 0;
            return new Character( c );
        }
        else if(m_type.equals("Boolean")) {
            return Boolean.valueOf(value);
        }
        else if(m_type.equals("Short")) {
            return Short.valueOf(value);
        }
        else {
            throw new IllegalArgumentException("Undefined property type '"+m_type+"'");
        }
    }
}
