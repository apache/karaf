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
package org.apache.felix.cm.impl;

import junit.framework.TestCase;

public class CaseInsensitiveDictionaryTest extends TestCase
{

    public void testValidKeys() {
        CaseInsensitiveDictionary.checkKey( "a" );
        CaseInsensitiveDictionary.checkKey( "1" );
        CaseInsensitiveDictionary.checkKey( "-" );
        CaseInsensitiveDictionary.checkKey( "_" );
        CaseInsensitiveDictionary.checkKey( "A" );
        CaseInsensitiveDictionary.checkKey( "a.b.c" );
        CaseInsensitiveDictionary.checkKey( "a.1.c" );
        CaseInsensitiveDictionary.checkKey( "a-sample.dotted_key.end" );
    }
    
    public void testKeyDots() {
        testFailingKey( "." );
        testFailingKey( ".a.b.c" );
        testFailingKey( "a.b.c." );
        testFailingKey( ".a.b.c." );
        testFailingKey( "a..b" );
    }
    
    public void testKeyIllegalCharacters() {
        testFailingKey( " " );
        testFailingKey( "§" );
        testFailingKey( "${yikes}" );
        testFailingKey( "a key with spaces" );
        testFailingKey( "fail:key" );
    }
    
    private void testFailingKey(String key) {
        try {
            CaseInsensitiveDictionary.checkKey( key );
            fail("Expected IllegalArgumentException for key [" + key + "]");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
