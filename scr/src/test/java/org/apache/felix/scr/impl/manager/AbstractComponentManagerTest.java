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
package org.apache.felix.scr.impl.manager;

import java.util.Hashtable;
import junit.framework.TestCase;

public class AbstractComponentManagerTest extends TestCase
{

    public void test_copyTo_withoutExclusions()
    {
        final Hashtable ht = new Hashtable();
        ht.put( "p1", "v1" );
        ht.put( "p.2", "v2" );
        ht.put( ".p3", "v3" );
        final Hashtable dict = (Hashtable) AbstractComponentManager.copyTo( null, ht, true );
        assertNotNull( "Copy result is not null", dict );
        assertEquals( "Number of items", 3, dict.size() );
        assertEquals( "Value for key p1", "v1", dict.get( "p1" ) );
        assertEquals( "Value for key p.2", "v2", dict.get( "p.2" ) );
        assertEquals( "Value for key .p3", "v3", dict.get( ".p3" ) );
    }

    public void test_copyTo_excludingStartingWithDot()
    {
        final Hashtable ht = new Hashtable();
        ht.put( "p1", "v1" );
        ht.put( "p.2", "v2" );
        ht.put( ".p3", "v3" );
        final Hashtable dict = (Hashtable) AbstractComponentManager.copyTo( null, ht, false );
        assertNotNull( "Copy result is not null", dict );
        assertEquals( "Number of items", 2, dict.size() );
        assertEquals( "Value for key p1", "v1", dict.get( "p1" ) );
        assertEquals( "Value for key p.2", "v2", dict.get( "p.2" ) );
    }

}
