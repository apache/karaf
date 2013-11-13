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
package org.apache.karaf.instance.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

public class ExecuteTest extends TestCase {
    private String userDir;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Execute.exitAllowed = false;
        userDir = System.getProperty("user.dir");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Execute.exitAllowed = true;
        System.setProperty("user.dir", userDir);
    }

    public void testListCommands() throws Exception {
        PrintStream oldOut = System.out;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream capturedOut = new PrintStream(baos); 
        System.setOut(capturedOut);

        try {
            Execute.main(new String [] {});            
        } catch (RuntimeException re) {
            assertEquals("0", re.getMessage());

            String s = new String(baos.toByteArray());            
            assertTrue(s.contains("list"));
            assertTrue(s.contains("create"));
            assertTrue(s.contains("destroy"));
        } finally {
            System.setOut(oldOut);
        }
    }
    
    public void testNonexistingCommand() throws Exception {
        try {
            Execute.main(new String [] {"bheuaark"});
        } catch (RuntimeException re) {
            assertEquals("-1", re.getMessage());
        }
    }
    
    public void testNoStorageFile() throws Exception {
        PrintStream oldErr = System.err;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream capturedErr = new PrintStream(baos); 
        System.setErr(capturedErr);

        try {
            Execute.main(new String [] {"create"});            
        } catch (RuntimeException re) {
            assertEquals("-2", re.getMessage());
            
            String s = new String(baos.toByteArray());            
            assertTrue(s.contains("karaf.instances"));
            assertTrue(s.contains("instance.properties"));
        } finally {
            System.setErr(oldErr);
        } 
    }
    
    public void testSetDir() throws Exception {
        Properties oldProps = (Properties) System.getProperties().clone();
        final File tempFile = createTempDir(getName());
        assertFalse("Precondition failed", 
            tempFile.getParentFile().getParentFile().getCanonicalPath().equals(System.getProperty("user.dir")));

        System.setProperty("karaf.instances", tempFile.getCanonicalPath());
        try {
            Execute.main(new String [] {"list"});            
            assertTrue(tempFile.getParentFile().getParentFile().getCanonicalPath().equals(System.getProperty("user.dir")));
        } finally {
            System.setProperties(oldProps);
            assertNull("Postcondition failed", System.getProperty("karaf.instances"));
            delete(tempFile);
        }        
    }
    
    private static File createTempDir(String name) throws IOException {
        final File tempFile = File.createTempFile(name, null);
        tempFile.delete();
        tempFile.mkdirs();
        return tempFile.getCanonicalFile();
    }

    private static void delete(File tmp) {
        if (tmp.isDirectory()) {
            for (File f : tmp.listFiles()) {
                delete(f);
            }
        }
        tmp.delete();
    }

}
