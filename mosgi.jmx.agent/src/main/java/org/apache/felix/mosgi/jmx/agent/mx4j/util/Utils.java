/*
 * Copyright (C) MX4J.
 * All rights reserved.
 *
 * This software is distributed under the terms of the MX4J License version 1.0.
 * See the terms of the MX4J License in the documentation provided with this software.
 */
/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.agent.mx4j.util;


import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Several utility functions for the JMX implementation
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class Utils
{
	/**
	 * This methods load a class given the classloader and the name of the class, and work for
	 * extended names of primitive types. <p>
	 * If you try to do ClassLoader.loadClass("boolean") it barfs it cannot find the class,
	 * so this method cope with this problem.
	 */
	public static Class loadClass(ClassLoader loader, String name) throws ClassNotFoundException
	{
		if (name == null) throw new ClassNotFoundException("null");

		name = name.trim();
		if (name.equals("boolean")) return boolean.class;
		else if (name.equals("byte")) return byte.class;
		else if (name.equals("char")) return char.class;
		else if (name.equals("short")) return short.class;
		else if (name.equals("int")) return int.class;
		else if (name.equals("long")) return long.class;
		else if (name.equals("float")) return float.class;
		else if (name.equals("double")) return double.class;
		else if (name.equals("java.lang.String")) return String.class;
		else if (name.equals("java.lang.Object")) return Object.class;
		else if (name.startsWith("["))
		{
			// It's an array, figure out how many dimensions
			int dimension = 0;
			while (name.charAt(dimension) == '[')
			{
				++dimension;
			}
			char type = name.charAt(dimension);
			Class cls = null;
			switch (type)
			{
				case 'Z':
					cls = boolean.class;
					break;
				case 'B':
					cls = byte.class;
					break;
				case 'C':
					cls = char.class;
					break;
				case 'S':
					cls = short.class;
					break;
				case 'I':
					cls = int.class;
					break;
				case 'J':
					cls = long.class;
					break;
				case 'F':
					cls = float.class;
					break;
				case 'D':
					cls = double.class;
					break;
				case 'L':
					// Strip the semicolon at the end
					String n = name.substring(dimension + 1, name.length() - 1);
					cls = loadClass(loader, n);
					break;
			}

			if (cls == null)
			{
				throw new ClassNotFoundException(name);
			}
			else
			{
				int[] dim = new int[dimension];
				return Array.newInstance(cls, dim).getClass();
			}
		}
		else
		{
         if (loader != null)
			   return loader.loadClass(name);
         else
            return Class.forName(name, false, null);
		}
	}

	/**
	 * Returns the classes whose names are specified by the <code>names</code> argument, loaded with the
	 * specified classloader.
	 */
	public static Class[] loadClasses(ClassLoader loader, String[] names) throws ClassNotFoundException
	{
		int n = names.length;
		Class[] cls = new Class[n];
		for (int i = 0; i < n; ++i)
		{
			String name = names[i];
			cls[i] = loadClass(loader, name);
		}
		return cls;
	}

	/**
	 * Returns true is the given method is a JMX attribute getter method
	 */
	public static boolean isAttributeGetter(Method m)
	{
		if (m == null) return false;

		String name = m.getName();
		Class retType = m.getReturnType();
		Class[] params = m.getParameterTypes();
		if (retType != Void.TYPE && params.length == 0)
		{
			if (name.startsWith("get") && name.length() > 3) return true;
			else if (name.startsWith("is") && retType == Boolean.TYPE) return true;
		}
		return false;
	}

	/**
	 * Returns true if the method is a JMX attribute setter method
	 */
	public static boolean isAttributeSetter(Method m)
	{
		if (m == null) return false;

		String name = m.getName();
		Class retType = m.getReturnType();
		Class[] params = m.getParameterTypes();
		if (retType == Void.TYPE && params.length == 1 && name.startsWith("set") && name.length() > 3)
		{
			return true;
		}
		return false;
	}

	public static boolean wildcardMatch(String pattern, String string)
	{
		int stringLength = string.length();
		int stringIndex = 0;
		for (int patternIndex = 0; patternIndex < pattern.length(); ++patternIndex)
		{
			char c = pattern.charAt(patternIndex);
			if (c == '*')
			{
				// Recurse with the pattern without this '*' and the actual string, until
				// match is found or we inspected the whole string
				while (stringIndex < stringLength)
				{
					if (wildcardMatch(pattern.substring(patternIndex + 1), string.substring(stringIndex)))
					{
						return true;
					}
					// No match found, try a shorter string, since we are matching '*'
					++stringIndex;
				}
			}
			else if (c == '?')
			{
				// Increment the string index, since '?' match a single char in the string
				++stringIndex;
				if (stringIndex > stringLength)
				{
					return false;
				}
			}
			else
			{
				// A normal character in the pattern, must match the one in the string
				if (stringIndex >= stringLength || c != string.charAt(stringIndex))
				{
					return false;
				}
				++stringIndex;
			}
		}

		// I've inspected the whole pattern, but not the whole string
		return stringIndex == stringLength;
	}

	public static boolean arrayEquals(Object[] arr1, Object[] arr2)
	{
		if (arr1 == null && arr2 == null) return true;
		if (arr1 == null ^ arr2 == null) return false;
		if (!arr1.getClass().equals(arr2.getClass())) return false;
		if (arr1.length != arr2.length) return false;

		for (int i = 0; i < arr1.length; ++i)
		{
			Object obj1 = arr1[i];
			Object obj2 = arr2[i];
			if (obj1 == null ^ obj2 == null) return false;
			if (obj1 != null && !obj1.equals(obj2)) return false;
		}
		return true;
	}

	public static boolean arrayEquals(byte[] arr1, byte[] arr2)
	{
		if (arr1 == null && arr2 == null) return true;
		if (arr1 == null ^ arr2 == null) return false;
		if (!arr1.getClass().equals(arr2.getClass())) return false;
		if (arr1.length != arr2.length) return false;

		for (int i = 0; i < arr1.length; ++i)
		{
			byte b1 = arr1[i];
			byte b2 = arr2[i];
			if (b1 != b2) return false;
		}
		return true;
	}

	public static int arrayHashCode(Object[] arr)
	{
		int hash = 0;
		if (arr != null)
		{
			// Avoid that 2 arrays of length 0 but different classes return same hash
			hash ^= arr.getClass().hashCode();
			for (int i = 0; i < arr.length; ++i)
			{
				hash ^= arr[i] == null ? 0 : arr[i].hashCode();
			}
		}
		return hash;
	}

	public static int arrayHashCode(byte[] arr)
	{
		int hash = 0;
		if (arr != null)
		{
			// Avoid that 2 arrays of length 0 but different classes return same hash
			hash ^= arr.getClass().hashCode();
			for (int i = 0; i < arr.length; ++i)
			{
				hash ^= arr[i];
			}
		}
		return hash;
	}

	public static char[] arrayCopy(char[] chars)
	{
		if (chars == null) return null;
		char[] copy = new char[chars.length];
		System.arraycopy(chars, 0, copy, 0, chars.length);
		return copy;
	}
}
