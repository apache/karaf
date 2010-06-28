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
package org.apache.felix.karaf.admin.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.felix.karaf.admin.AdminService;
import org.apache.felix.karaf.admin.command.AdminCommandSupport;
import org.apache.felix.karaf.admin.internal.AdminServiceImpl;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

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
            assertEquals("-1", re.getMessage());
            
            String s = new String(baos.toByteArray());            
            assertTrue(s.contains("storage.location"));
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

        System.setProperty("storage.location", tempFile.getCanonicalPath());
        try {
            Execute.main(new String [] {"list"});            
            assertTrue(tempFile.getParentFile().getParentFile().getCanonicalPath().equals(System.getProperty("user.dir")));
        } finally {
            System.setProperties(oldProps);
            assertNull("Postcondition failed", System.getProperty("storage.location"));
            delete(tempFile);
        }        
    }
    
    public void testExecute() throws Exception {
        final File tempFile = createTempDir(getName());
        Properties p = new Properties();
        p.setProperty("port", "1302");
        FileOutputStream fos = new FileOutputStream(new File(tempFile, AdminServiceImpl.STORAGE_FILE));
        p.store(fos, "");
        fos.close();

        final List<AdminServiceImpl> admins = new ArrayList<AdminServiceImpl>();
        try {
            AdminCommandSupport mockCommand = EasyMock.createStrictMock(AdminCommandSupport.class);
            mockCommand.setAdminService((AdminService) EasyMock.anyObject());
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    AdminServiceImpl svc = (AdminServiceImpl) EasyMock.getCurrentArguments()[0];
                    assertEquals(tempFile, svc.getStorageLocation());
                    admins.add(svc);
                    return null;
                }
            });
            
            EasyMock.expect(mockCommand.execute(null)).andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    // The Admin Service should be initialized at this point.
                    // One way to find this out is by reading out the port number
                    AdminServiceImpl admin = admins.get(0);
                    Field field = AdminServiceImpl.class.getDeclaredField("defaultPortStart");
                    field.setAccessible(true);
                    assertEquals(1302, field.get(admin));
                    return null;
                }
            });
            EasyMock.replay(mockCommand);            
            
            Execute.execute(mockCommand, tempFile, new String [] {"test"});
            
            EasyMock.verify(mockCommand);
        } finally {
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
