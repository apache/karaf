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


/**
 * Specialized ternary tree for method metadata information. <p>
 * In JMX methods are referred to with the method name and the String[] representing the signature.
 * One can decide to cache method information using as key a concatenation of method name + signature,
 * but the cost of concatenation is very high, while hashmap access is fast.
 * Ternary trees avoid string concatenation, and result to be 10x faster than concatenation + hashmap.
 * However, the signature of a standard TernaryTree would be <code>Object get(Object[] key)</code> and
 * <code>void put(Object[] key, Object value)</code>. Unfortunately normalizing method name + signature
 * into a single array is also very expensive. <br>
 * This version leaves method name and signature separated to have the fastest access possible to
 * method information.
 * See <a href="http://www.ddj.com/documents/s=921/ddj9804a/9804a.htm">here</a> for further information
 * on TernaryTrees.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class MethodTernaryTree
{
	private Node m_root;

	/**
	 * Returns the method information given the method name and its signature.
	 * @see #put
	 */
	public Object get(String methodName, String[] signature)
	{
		if (signature == null) {throw new IllegalArgumentException();}
		return search(methodName, signature);
	}

	/**
	 * Inserts in this TernaryTree the given method information, using as key the method name and its signature
	 * @see #get
	 */
	public void put(String methodName, String[] signature, Object information)
	{
		if (signature == null) {throw new IllegalArgumentException();}
		m_root = insert(m_root, methodName, signature, signature.length, information);
	}

	private Object search(String methodName, String[] signature)
	{
		Node node = m_root;
		int index = 0;
		while (node != null)
		{
			Object key = index == 0 ? methodName : signature[index - 1];
			if (key == null) {throw new IllegalArgumentException();}

			int split = splitFunction(key);
			if (split < node.splitValue)
			{
				node = node.left;
			}
			else if (split == node.splitValue)
			{
				if (index == signature.length)
				{
					// Two objects may return the same split, because the splitFunction is not perfect
					// (ie does not always yield different values for different objects, eg hash functions)
					if (node.keys == null) {return null;}
					for (int i = 0; i < node.keys.length; ++i)
					{
						if (node.keys[i].equals(key))
						{
							return node.values[i];
						}
					}
					return null;
				}
				else
				{
					++index;
					node = node.middle;
				}
			}
			else
			{
				node = node.right;
			}
		}
		return null;
	}

	private Node insert(Node node, String methodName, String[] signature, int length, Object value)
	{
		Object key = methodName;
		if (key == null) {throw new IllegalArgumentException();}

		int split = splitFunction(key);
		if (node == null)
		{
			node = new Node();
			node.splitValue = split;
		}

		if (split < node.splitValue)
		{
			node.left = insert(node.left, methodName, signature, length, value);
		}
		else if (split == node.splitValue)
		{
			// Two objects may return the same split, because the splitFunction is not perfect
			// (ie does not always yield different values for different objects, eg hash functions)
			if (length == 0)
			{
				if (node.keys == null)
				{
					node.keys = new Object[1];
					node.values = new Object[1];
					node.keys[0] = key;
					node.values[0] = value;
				}
				else
				{
					// Loop to see if the key already exists
					boolean found = false;
					for (int i = 0; i < node.keys.length; ++i)
					{
						if (node.keys[i].equals(key))
						{
							// Already present, replace the value
							node.keys[i] = key;
							node.values[i] = value;
							found = true;
							break;
						}
					}
					// Not present, add it
					if (!found)
					{
						int len = node.keys.length;
						Object[] olds = node.keys;
						node.keys = new Object[len + 1];
						System.arraycopy(olds, 0, node.keys, 0, len);
						node.keys[len] = key;

						olds = node.values;
						node.values = new Object[len + 1];
						System.arraycopy(olds, 0, node.values, 0, len);
						node.values[len] = value;
					}
				}
			}
			else
			{
				node.middle = insert(node.middle, signature[signature.length - length], signature, length - 1, value);
			}
		}
		else
		{
			node.right = insert(node.right, methodName, signature, length, value);
		}

		return node;
	}

	protected int splitFunction(Object obj)
	{
		return obj.hashCode();
	}

	private class Node
	{
		private int splitValue;
		private Node right;
		private Node middle;
		private Node left;
		private Object[] keys;
		private Object[] values;
	}
}
