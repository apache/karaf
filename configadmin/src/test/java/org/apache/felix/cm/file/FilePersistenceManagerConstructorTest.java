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
package org.apache.felix.cm.file;

import java.io.File;

import junit.framework.TestCase;

import org.apache.felix.cm.MockBundleContext;
import org.osgi.framework.BundleContext;

/**
 * The <code>FilePersistenceManagerConstructorTest</code> TODO
 */
public class FilePersistenceManagerConstructorTest extends TestCase
{

    /** The current working directory for the tests */
    private static final String TEST_WORKING_DIRECTORY = "target";

    /** The Bundle Data Area directory for testing */
    private static final String BUNDLE_DATA = "bundleData";

    /** The Configuration location path for testing */
    private static final String LOCATION_TEST = "test";

    /** the previous working directory to return to on tearDown */
    private String oldWorkingDirectory;

    protected void setUp() throws Exception
    {
        super.setUp();

        String testDir = new File(TEST_WORKING_DIRECTORY).getAbsolutePath();

        oldWorkingDirectory = System.getProperty( "user.dir" );
        System.setProperty( "user.dir", testDir );
    }

    protected void tearDown() throws Exception
    {
        System.setProperty( "user.dir", oldWorkingDirectory );

        super.tearDown();
    }

    /**
     * Test method for {@link org.apache.felix.cm.file.FilePersistenceManager#FilePersistenceManager(java.lang.String)}.
     */
    public void testFilePersistenceManagerString()
    {
        // variables used in these tests
        FilePersistenceManager fpm;
        String relPath;
        String absPath;

        // with null
        fpm = new FilePersistenceManager(null);
        assertFpm(fpm, new File(FilePersistenceManager.DEFAULT_CONFIG_DIR) );

        // with a relative path
        relPath = LOCATION_TEST;
        fpm = new FilePersistenceManager(relPath);
        assertFpm(fpm, new File(relPath) );

        // with an absolute path
        absPath = new File(LOCATION_TEST).getAbsolutePath();
        fpm = new FilePersistenceManager(absPath);
        assertFpm(fpm, new File(absPath) );
    }


    /**
     * Test method for {@link org.apache.felix.cm.file.FilePersistenceManager#FilePersistenceManager(org.osgi.framework.BundleContext, java.lang.String)}.
     */
    public void testFilePersistenceManagerBundleContextString()
    {
        // variables used in these tests
        BundleContext bundleContext;
        FilePersistenceManager fpm;
        String relPath;
        String absPath;
        File dataArea;

        // first suite: no BundleContext at all

        // with null
        fpm = new FilePersistenceManager(null);
        assertFpm(fpm, new File(FilePersistenceManager.DEFAULT_CONFIG_DIR) );

        // with a relative path
        relPath = LOCATION_TEST;
        fpm = new FilePersistenceManager(relPath);
        assertFpm(fpm, new File(relPath) );

        // with an absolute path
        absPath = new File(LOCATION_TEST).getAbsolutePath();
        fpm = new FilePersistenceManager(absPath);
        assertFpm(fpm, new File(absPath) );


        // second suite: BundleContext without data file
        bundleContext = new FilePersistenceManagerBundleContext(null);

        // with null
        fpm = new FilePersistenceManager(bundleContext, null);
        assertFpm(fpm, new File(FilePersistenceManager.DEFAULT_CONFIG_DIR) );

        // with a relative path
        relPath = LOCATION_TEST;
        fpm = new FilePersistenceManager(bundleContext, relPath);
        assertFpm(fpm, new File(relPath) );

        // with an absolute path
        absPath = new File(LOCATION_TEST).getAbsolutePath();
        fpm = new FilePersistenceManager(bundleContext, absPath);
        assertFpm(fpm, new File(absPath) );


        // third suite: BundleContext with relative data file
        dataArea = new File(BUNDLE_DATA);
        bundleContext = new FilePersistenceManagerBundleContext(dataArea);

        // with null
        fpm = new FilePersistenceManager(bundleContext, null);
        assertFpm(fpm, new File(dataArea, FilePersistenceManager.DEFAULT_CONFIG_DIR) );

        // with a relative path
        relPath = LOCATION_TEST;
        fpm = new FilePersistenceManager(bundleContext, relPath);
        assertFpm(fpm, new File(dataArea, relPath) );

        // with an absolute path
        absPath = new File(LOCATION_TEST).getAbsolutePath();
        fpm = new FilePersistenceManager(bundleContext, absPath);
        assertFpm(fpm, new File(absPath) );

        // fourth suite: BundleContext with absolute data file
        dataArea = new File(BUNDLE_DATA).getAbsoluteFile();
        bundleContext = new FilePersistenceManagerBundleContext(dataArea);

        // with null
        fpm = new FilePersistenceManager(bundleContext, null);
        assertFpm(fpm, new File(dataArea, FilePersistenceManager.DEFAULT_CONFIG_DIR) );

        // with a relative path
        relPath = LOCATION_TEST;
        fpm = new FilePersistenceManager(bundleContext, relPath);
        assertFpm(fpm, new File(dataArea, relPath) );

        // with an absolute path
        absPath = new File(LOCATION_TEST).getAbsolutePath();
        fpm = new FilePersistenceManager(bundleContext, absPath);
        assertFpm(fpm, new File(absPath) );
    }


    private void assertFpm(FilePersistenceManager fpm, File expected) {
        assertEquals( expected.getAbsoluteFile(), fpm.getLocation() );
    }

    private static final class FilePersistenceManagerBundleContext extends MockBundleContext {

        private File dataArea;

        private FilePersistenceManagerBundleContext( File dataArea )
        {
            this.dataArea = dataArea;
        }

        public File getDataFile( String path )
        {
            return (dataArea != null) ? new File(dataArea, path) : null;
        }
    }
}
