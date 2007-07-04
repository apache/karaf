/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.felix.cm.file;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

import org.apache.felix.cm.MockBundleContext;
import org.apache.felix.cm.impl.ConfigurationManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import junit.framework.TestCase;

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
