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
        assertFpm(fpm, new File(FilePersistenceManager.DEFAULT_CONFIG_DIR).getAbsoluteFile() );
        
        // with a relative path
        relPath = "test";
        fpm = new FilePersistenceManager(relPath);
        assertFpm(fpm, new File(relPath).getAbsoluteFile() );
        
        // with an absolute path
        absPath = new File("test").getAbsolutePath();
        fpm = new FilePersistenceManager(absPath);
        assertFpm(fpm, new File(absPath).getAbsoluteFile() );
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
        
        // first suite: no BundleContext at all
        
        // with null
        fpm = new FilePersistenceManager(null);
        assertFpm(fpm, new File(FilePersistenceManager.DEFAULT_CONFIG_DIR).getAbsoluteFile() );
        
        // with a relative path
        relPath = "test";
        fpm = new FilePersistenceManager(relPath);
        assertFpm(fpm, new File(relPath).getAbsoluteFile() );
        
        // with an absolute path
        absPath = new File("test").getAbsolutePath();
        fpm = new FilePersistenceManager(absPath);
        assertFpm(fpm, new File(absPath).getAbsoluteFile() );
        
        
        // second suite: BundleContext without data file
        bundleContext = new FilePersistenceManagerBundleContext(null);
        
        // with null
        fpm = new FilePersistenceManager(bundleContext, null);
        assertFpm(fpm, new File(FilePersistenceManager.DEFAULT_CONFIG_DIR).getAbsoluteFile() );
        
        // with a relative path
        relPath = "test";
        fpm = new FilePersistenceManager(bundleContext, relPath);
        assertFpm(fpm, new File(relPath).getAbsoluteFile() );
        
        // with an absolute path
        absPath = new File("test").getAbsolutePath();
        fpm = new FilePersistenceManager(bundleContext, absPath);
        assertFpm(fpm, new File(absPath).getAbsoluteFile() );

        
        // third suite: BundleContext with data file
        File dataArea = new File("bundleData");
        bundleContext = new FilePersistenceManagerBundleContext(dataArea);
        
        // with null
        fpm = new FilePersistenceManager(bundleContext, null);
        assertFpm(fpm, new File(dataArea, FilePersistenceManager.DEFAULT_CONFIG_DIR).getAbsoluteFile() );
        
        // with a relative path
        relPath = "test";
        fpm = new FilePersistenceManager(bundleContext, relPath);
        assertFpm(fpm, new File(dataArea, relPath).getAbsoluteFile() );
        
        // with an absolute path
        absPath = new File("test").getAbsolutePath();
        fpm = new FilePersistenceManager(bundleContext, absPath);
        assertFpm(fpm, new File(absPath).getAbsoluteFile() );
    }

    
    private void assertFpm(FilePersistenceManager fpm, File expected) {
        assertEquals( expected, fpm.getLocation() );
    }
    
    private static final class FilePersistenceManagerBundleContext extends MockBundleContext {

        private File dataArea;
        
        private FilePersistenceManagerBundleContext( File dataArea )
        {
            this.dataArea = (dataArea != null) ? dataArea.getAbsoluteFile() : null;
        }
        
        public File getDataFile( String path )
        {
            return (dataArea != null) ? new File(dataArea, path) : null;
        }
    }
}
