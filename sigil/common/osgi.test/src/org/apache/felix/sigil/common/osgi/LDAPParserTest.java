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

package org.apache.felix.sigil.common.osgi;

import junit.framework.TestCase;

public class LDAPParserTest extends TestCase
{
    private static final SimpleTerm A_B = new SimpleTerm( "a", Ops.EQ, "b" );
    private static final SimpleTerm C_D = new SimpleTerm( "c", Ops.EQ, "d" );
    
    public void testSimple() {
        LDAPExpr expr = LDAPParser.parseExpression( "(a=b)" );
        assertEquals( expr, A_B );
    }

    public void testSimpleWhiteSpace() {
        LDAPExpr expr = LDAPParser.parseExpression( "  ( a = b )  " );
        assertEquals( expr, A_B );
    }
    
    public void testNot() {
        LDAPExpr expr = LDAPParser.parseExpression( "(!(a=b))" );
        assertEquals( expr, Not.apply(A_B));
    }
    
    public void testAnd() {
        LDAPExpr expr = LDAPParser.parseExpression( "(&(a=b)(c=d))" );
        assertEquals( expr, And.apply(A_B, C_D) );
    }
    
    public void testOr() {
        LDAPExpr expr = LDAPParser.parseExpression( "(|(a=b)(c=d))" );
        assertEquals( expr, Or.apply(A_B, C_D) );
    }
    
    public void testParseException() {
        try {
            LDAPExpr expr = LDAPParser.parseExpression( ".(a=b)" );
            fail( "Unexpectedly parsed invalid ldap expr " + expr);
        }
        catch (LDAPParseException e) {
            // expected
        }
    }
}
