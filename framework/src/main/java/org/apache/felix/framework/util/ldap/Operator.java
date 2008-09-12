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
package org.apache.felix.framework.util.ldap;

import java.util.Stack;

public abstract class Operator
{
    public abstract void execute(Stack operands, Mapper mapper)
        throws EvaluationException;

    public abstract String toString();

    public abstract void buildTree(Stack operands); // re-build the parsetree
    public abstract void toStringInfix(StringBuffer b); // convert to canonical string

    // Place to store the reconstructed parsetree
    // Vector -> ArrayList is using jdk1.2 or later
    public volatile Operator[] children = null;
}