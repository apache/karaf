/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.shell.log.completers;

import java.util.LinkedList;
import java.util.List;

import org.apache.felix.karaf.shell.log.Level;

import junit.framework.TestCase;

/**
 * Test cases for {@link LogLevelCompleter}
 */
public class LogLevelCompleterTest extends TestCase {
    
    private final LogLevelCompleter completer = new LogLevelCompleter();
    
    public void testComplete() throws Exception {
        assertCompletions("I", Level.INFO.name());
        assertCompletions("D", Level.DEBUG.name(), Level.DEFAULT.name());
    }
    
    public void testCompleteLowerCase() throws Exception {
        assertCompletions("i", Level.INFO.name());
        assertCompletions("d", Level.DEBUG.name(), Level.DEFAULT.name());
    }
    
    public void testCompleteWithNullBuffer() throws Exception {
        // an empty buffer should return all available options
        assertCompletions(null, Level.strings());
    }

    private void assertCompletions(String buffer, String... results) {
        List<String> candidates = new LinkedList<String>();
        assertEquals("Completer should have found a match", 0, completer.complete(buffer, 0, candidates));
        assertEquals(results.length, candidates.size());
        for (String result : results) {
            assertContains(result, candidates);
        }
    }
    
    private void assertContains(String value, List<String> values) {
        for (String element : values) {
            if (value.trim().equals(element.trim())) {
                return;
            }
        }
        fail("Element " + value + " not found in array");
    }
}
