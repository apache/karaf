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
package org.apache.felix.dependencymanager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


/**
 * Default null object implementation. Uses a dynamic proxy. Null objects are used
 * as placeholders for services that are not available.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class DefaultNullObject implements InvocationHandler {
    private static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;
    private static final Byte DEFAULT_BYTE = new Byte((byte) 0);
    private static final Short DEFAULT_SHORT = new Short((short) 0);
    private static final Integer DEFAULT_INT = new Integer(0);
    private static final Long DEFAULT_LONG = new Long(0);
    private static final Float DEFAULT_FLOAT = new Float(0.0f);
    private static final Double DEFAULT_DOUBLE = new Double(0.0);
    
    /**
     * Invokes a method on this null object. The method will return a default
     * value without doing anything.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class returnType = method.getReturnType();
        if (returnType.equals(Boolean.class) || returnType.equals(Boolean.TYPE)) {
            return DEFAULT_BOOLEAN;
        }
        else if (returnType.equals(Byte.class) || returnType.equals(Byte.TYPE)) {
            return DEFAULT_BYTE;
        } 
        else if (returnType.equals(Short.class) || returnType.equals(Short.TYPE)) {
            return DEFAULT_SHORT;
        } 
        else if (returnType.equals(Integer.class) || returnType.equals(Integer.TYPE)) {
            return DEFAULT_INT;
        } 
        else if (returnType.equals(Long.class) || returnType.equals(Long.TYPE)) {
            return DEFAULT_LONG;
        } 
        else if (returnType.equals(Float.class) || returnType.equals(Float.TYPE)) {
            return DEFAULT_FLOAT;
        } 
        else if (returnType.equals(Double.class) || returnType.equals(Double.TYPE)) {
            return DEFAULT_DOUBLE;
        } 
        else {
            return null;
        }
    }
}