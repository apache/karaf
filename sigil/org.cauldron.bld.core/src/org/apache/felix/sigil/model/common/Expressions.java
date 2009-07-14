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

package org.apache.felix.sigil.model.common;

import java.util.Map;

public class Expressions {

    public static LDAPExpr and(LDAPExpr... terms) {
        return And.apply(terms);
    }

    public static LDAPExpr or(LDAPExpr... terms) {
        return Or.apply(terms);
    }

    public static LDAPExpr not(LDAPExpr e) {
        return Not.apply(e);
    }

    public static LDAPExpr T = Bool.TRUE;
    public static LDAPExpr F = Bool.FALSE;

    // supports direct use of wildcards for ease of testing, but not literal *s
    public static SimpleTerm ex(String name, Ops op, String rhs) {

        rhs = rhs.replace('*', SimpleTerm.WILDCARD);
        return new SimpleTerm(name, op, rhs);
    }

}

class Bool implements LDAPExpr {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final Bool TRUE = new Bool(true);
    public static final Bool FALSE = new Bool(false);

    private boolean bool;

    public Bool(boolean bool) {
        this.bool = bool;
    }

    public boolean eval(Map<String, ?> map) {
        return bool;
    }

    public void visit(ExprVisitor v) {
    }

    public LDAPExpr[] getChildren() {
        return CHILDLESS;
    }

    public String toString() {
        return "(" + bool + ")";
    }
}
