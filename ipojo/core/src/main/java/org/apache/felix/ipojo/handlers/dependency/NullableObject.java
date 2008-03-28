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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Default nullable object.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class NullableObject implements InvocationHandler {

    /**
     * Default boolean value.
     */
    private static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;

    /**
     * Default byte value.
     */
    private static final Byte DEFAULT_BYTE = new Byte((byte) 0);

    /**
     * Default short value.
     */
    private static final Short DEFAULT_SHORT = new Short((short) 0);

    /**
     * Default integer value.
     */
    private static final Integer DEFAULT_INT = new Integer(0);

    /**
     * Default long value.
     */
    private static final Long DEFAULT_LONG = new Long(0);

    /**
     * Default float value.
     */
    private static final Float DEFAULT_FLOAT = new Float(0.0f);

    /**
     * Default double value.
     */
    private static final Double DEFAULT_DOUBLE = new Double(0.0);

    /**
     * Invokes a method on this null object. The method will return a default
     * value without doing anything.
     * @param proxy : proxy object
     * @param method : invoked method
     * @param args : arguments.
     * @return the returned value.
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) {
        Class returnType = method.getReturnType();
        if (Boolean.TYPE.equals(returnType)) {
            return DEFAULT_BOOLEAN;
        } else if (Byte.TYPE.equals(returnType)) {
            return DEFAULT_BYTE;
        } else if (Short.TYPE.equals(returnType)) {
            return DEFAULT_SHORT;
        } else if (Integer.TYPE.equals(returnType)) {
            return DEFAULT_INT;
        } else if (Long.TYPE.equals(returnType)) {
            return DEFAULT_LONG;
        } else if (Float.TYPE.equals(returnType)) {
            return DEFAULT_FLOAT;
        } else if (Double.TYPE.equals(returnType)) {
            return DEFAULT_DOUBLE;
        } else {
            return null;
        }
    }
}
