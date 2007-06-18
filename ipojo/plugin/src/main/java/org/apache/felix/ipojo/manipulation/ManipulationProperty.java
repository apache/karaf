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
package org.apache.felix.ipojo.manipulation;

/**
 * Store properties for the manipulation process.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class ManipulationProperty {

	/**
	 * iPOJO Package name.
	 */
	protected static final String IPOJO_PACKAGE_NAME = "org.apache.felix.ipojo";

	/**
	 * Activator internal package name.
	 */
	protected static final String IPOJO_INTERNAL_PACKAGE_NAME = "org/apache/felix/ipojo/";

	/**
	 * Ipojo internal package name for internal descriptor.
	 */
	protected static final String IPOJO_INTERNAL_DESCRIPTOR = "L" + IPOJO_INTERNAL_PACKAGE_NAME;

	/**
	 * Helper array for bytecode manipulation of primitive type.
	 */
	protected static final String[][] PRIMITIVE_BOXING_INFORMATION =
		new String[][] {
		{"V", "ILLEGAL", "ILLEGAL"}, // Void type [0]
		{"Z", "java/lang/Boolean", "booleanValue"}, // boolean [1]
		{"C", "java/lang/Character", "charValue"}, // char [2]
		{"B", "java/lang/Byte", "byteValue"}, // byte [3]
		{"S", "java/lang/Short", "shortValue"}, // short [4]
		{"I", "java/lang/Integer", "intValue"}, // int [5]
		{"F", "java/lang/Float", "floatValue"}, // float [6]
		{"J", "java/lang/Long", "longValue"}, // long [7]
		{"D", "java/lang/Double", "doubleValue"} // double [8]
	};

}
