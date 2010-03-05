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

package org.apache.felix.sigil.obr.impl;

import org.apache.felix.sigil.common.osgi.LDAPExpr;
import org.apache.felix.sigil.common.osgi.LDAPParser;
import org.apache.felix.sigil.common.osgi.VersionRange;
import org.osgi.framework.Version;

import junit.framework.TestCase;

public class VersionRangeHelperTest extends TestCase
{
    public void testRange1() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(&(version>=1.0.0)(version<=2.0.0))" );
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("[1.0.0,2.0.0]"), range );
    }
    
    public void testRange2() {
        LDAPExpr expr;
        VersionRange range;
            
        expr = LDAPParser.parseExpression("(&(version>1.0.0)(version<2.0.0))");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("(1.0.0,2.0.0)"), range );        
    }
    
    public void testRange3() {
        LDAPExpr expr;
        VersionRange range;

        expr = LDAPParser.parseExpression("(&(!(version<1.0.0))(!(version>2.0.0)))");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("[1.0.0,2.0.0]"), range );
    }
    
    public void testRange4() {
        LDAPExpr expr;
        VersionRange range;

        expr = LDAPParser.parseExpression("(&(!(version<=1.0.0))(!(version>=2.0.0)))");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("(1.0.0,2.0.0)"), range );
    }
    
    public void testRange5() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(version=1.0.0)");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("[1.0.0,1.0.0]"), range );
    }
    
    public void testRange6() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(version>=1.0.0)");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( new VersionRange(false, new Version(1,0,0), VersionRange.INFINITE_VERSION, true ), range );
    }
    
    public void testRange7() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(version<=2.0.0)");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("[0,2.0.0]"), range );
    }
    
    public void testRange8() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(version>1.0.0)");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( new VersionRange(true, new Version(1,0,0), VersionRange.INFINITE_VERSION, true ), range );
    }
    
    public void testRange9() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(version<2.0.0)");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("[0,2.0.0)"), range );
    }
    
    public void testRange10() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(!(version>2.0.0))");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("[0,2.0.0]"), range );
    }
    
    public void testRange11() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(!(version<1.0.0))");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("1.0.0"), range );
    }
    
    public void testRange12() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(!(version>=2.0.0))");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( VersionRange.parseVersionRange("[0,2.0.0)"), range );
    }
    
    public void testRange13() {
        LDAPExpr expr;
        VersionRange range;
        
        expr = LDAPParser.parseExpression("(!(version<=1.0.0))");
        range = VersionRangeHelper.decodeVersions( expr );
        assertEquals( new VersionRange(true, new Version(1,0,0), VersionRange.INFINITE_VERSION, true ), range );
    }
}
