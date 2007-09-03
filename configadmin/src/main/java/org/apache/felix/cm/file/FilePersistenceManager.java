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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;


/**
 * The <code>FilePersistenceManager</code> class stores configuration data in
 * properties-like files inside a given directory. All configuration files are
 * located in the same directory.
 * <p>
 * The configuration directory is set by either the
 * {@link #FilePersistenceManager(String)} constructor or the
 * {@link #FilePersistenceManager(BundleContext, String)} constructor. Refer
 * to the respective JavaDocs for more information.
 * <p>
 * When this persistence manager is used by the Configuration Admin Service,
 * the location may be configured using the
 * {@link org.apache.felix.cm.impl.ConfigurationManager#CM_CONFIG_DIR} bundle
 * context property. That is the Configuration Admin Service creates an instance
 * of this class calling
 * <code>new FilePersistenceManager(bundleContext, bundleContext.getProperty(CM_CONFIG_DIR))</code>.
 * <p>
 * If the location is not set, the <code>config</code> directory in the current
 * working directory (as set in the <code>user.dir</code> system property) is
 * used. If the the location is set but, no such directory exists, the directory
 * and any missing parent directories are created. If a file exists at the given
 * location, the constructor fails.
 * <p>
 * Configuration files are created in the configuration directory by appending
 * the extension <code>.config</code> to the PID of the configuration. The PID
 * is converted into a relative path name by replacing enclosed dots to slashes.
 * Non-<code>symbolic-name</code> characters in the PID are encoded with their
 * Unicode character code in hexadecimal.
 * <p>
 * <table border="0" cellspacing="3" cellpadding="0">
 * <tr><td colspan="2"><b>Examples of PID to name conversion:</td></tr>
 * <tr><th>PID</th><th>Configuration File Name</th></tr>
 * <tr><td><code>sample</code><td><code>sample.config</code></tr>
 * <tr><td><code>org.apache.felix.log.LogService</code><td><code>org/apache/felix/log/LogService.config</code></tr>
 * <tr><td><code>sample.fl&auml;che</code><td><code>sample/fl%00e8che.config</code></tr>
 * </table>
 * <p>
 * <b>Mulithreading Issues</b>
 * <p>
 * In a multithreaded environment the {@link #store(String, Dictionary)} and
 * {@link #load(File)} methods may be called at the the quasi-same time for the
 * same configuration PID. It may no happen, that the store method starts
 * writing the file and the load method might at the same time read from the
 * file currently being written and thus loading corrupt data (if data is
 * available at all).
 * <p>
 * To prevent this situation from happening, the methods use synchronization
 * and temporary files as follows:
 * <ul>
 * <li>The {@link #store(String, Dictionary)} method writes a temporary file
 * with file extension <code>.tmp</code>. When done, the file is renamed to
 * actual configuration file name as implied by the PID. This last step of
 * renaming the file is synchronized on the FilePersistenceManager instance.</li>
 * <li>The {@link #load(File)} method is completeley synchronized on the
 * FilePersistenceManager instance such that the {@link #store} method might
 * inadvertantly try to replace the file while it is being read.</li>
 * <li>Finally the <code>Iterator</code> returned by {@link #getDictionaries()}
 * is implemented such that any temporary configuration file is just ignored.</li>
 * </ul>
 *
 * @author fmeschbe
 */
public class FilePersistenceManager implements PersistenceManager
{

    /**
     * The default configuration data directory if no location is configured
     * (value is "config").
     */
    public static final String DEFAULT_CONFIG_DIR = "config";

    /**
     * The extension of the configuration files.
     */
    private static final String FILE_EXT = ".config";

    /**
     * The extension of the configuration files, while they are being written
     * (value is ".tmp").
     *
     * @see #store(String, Dictionary)
     */
    private static final String TMP_EXT = ".tmp";

    private static final BitSet VALID_PATH_CHARS;

    /**
     * The abstract path name of the configuration files.
     */
    private final File location;

    // sets up this class defining the set of valid characters in path
    // set getFile(String) for details.
    static
    {
        VALID_PATH_CHARS = new BitSet();

        for ( int i = 'a'; i <= 'z'; i++ )
        {
            VALID_PATH_CHARS.set( i );
        }
        for ( int i = 'A'; i <= 'Z'; i++ )
        {
            VALID_PATH_CHARS.set( i );
        }
        for ( int i = '0'; i <= '9'; i++ )
        {
            VALID_PATH_CHARS.set( i );
        }
        VALID_PATH_CHARS.set( File.separatorChar );
        VALID_PATH_CHARS.set( ' ' );
        VALID_PATH_CHARS.set( '-' );
        VALID_PATH_CHARS.set( '_' );
    }


    /**
     * Encodes a Service PID to a filesystem path as described in the class
     * JavaDoc above.
     * <p>
     * This method is not part of the API of this class and is declared package
     * private to enable JUnit testing on it. This method may be removed or
     * modified at any time without notice.
     *
     * @param pid The Service PID to encode into a relative path name.
     *
     * @return The relative path name corresponding to the Service PID.
     */
    static String encodePid( String pid )
    {

        // replace dots by File.separatorChar
        pid = pid.replace( '.', File.separatorChar );

        // replace slash by File.separatorChar if different
        if ( File.separatorChar != '/' )
        {
            pid = pid.replace( '/', File.separatorChar );
        }

        // scan for first non-valid character (if any)
        int first = 0;
        while ( first < pid.length() && VALID_PATH_CHARS.get( pid.charAt( first ) ) )
        {
            first++;
        }

        // check whether we exhausted
        if ( first < pid.length() )
        {
            StringBuffer buf = new StringBuffer( pid.substring( 0, first ) );

            for ( int i = first; i < pid.length(); i++ )
            {
                char c = pid.charAt( i );
                if ( VALID_PATH_CHARS.get( c ) )
                {
                    buf.append( c );
                }
                else
                {
                    String val = "000" + Integer.toHexString( c );
                    buf.append( '%' );
                    buf.append( val.substring( val.length() - 4 ) );
                }
            }

            pid = buf.toString();
        }

        return pid;
    }


    /**
     * Creates an instance of this persistence manager using the given location
     * as the directory to store and retrieve the configuration files.
     * <p>
     * This constructor resolves the configuration file location as follows:
     * <ul>
     * <li>If <code>location</code> is <code>null</code>, the <code>config</code>
     * directory in the current working directory as specified in the
     * <code>user.dir</code> system property is assumed.</li>
     * <li>Otherwise the named directory is used.</li>
     * <li>If the directory name resolved in the first or second step is not an
     * absolute path, it is resolved to an absolute path calling the
     * <code>File.getAbsoluteFile()</code> method.</li>
     * <li>If a non-directory file exists as the location found in the previous
     * step or the named directory (including any parent directories) cannot be
     * created, an <code>IllegalArgumentException</code> is thrown.</li>
     * </ul>
     * <p>
     * This constructor is equivalent to calling
     * {@link #FilePersistenceManager(BundleContext, String)} with a
     * <code>null</code> <code>BundleContext</code>.
     *
     * @param location The configuration file location. If this is
     *      <code>null</code> the <code>config</code> directory below the current
     *      working directory is used.
     *
     * @throws IllegalArgumentException If the <code>location</code> exists but
     *      is not a directory or does not exist and cannot be created.
     */
    public FilePersistenceManager( String location )
    {
        this( null, location );
    }


    /**
     * Creates an instance of this persistence manager using the given location
     * as the directory to store and retrieve the configuration files.
     * <p>
     * This constructor resolves the configuration file location as follows:
     * <ul>
     * <li>If <code>location</code> is <code>null</code>, the <code>config</code>
     * directory in the persistent storage area of the bundle identified by
     * <code>bundleContext</code> is used.</li>
     * <li>If the framework does not support persistent storage area for bundles
     * in the filesystem or if <code>bundleContext</code> is <code>null</code>,
     * the <code>config</code> directory in the current working directory as
     * specified in the <code>user.dir</code> system property is assumed.</li>
     * <li>Otherwise the named directory is used.</li>
     * <li>If the directory name resolved in the first, second or third step is
     * not an absolute path and a <code>bundleContext</code> is provided which
     * provides access to persistent storage area, the directory name is
     * resolved as being inside the persistent storage area. Otherwise the
     * directory name is resolved to an absolute path calling the
     * <code>File.getAbsoluteFile()</code> method.</li>
     * <li>If a non-directory file exists as the location found in the previous
     * step or the named directory (including any parent directories) cannot be
     * created, an <code>IllegalArgumentException</code> is thrown.</li>
     * </ul>
     *
     * @param bundleContext The <code>BundleContext</code> to optionally get
     *      the data location for the configuration files. This may be
     *      <code>null</code>, in which case this constructor acts exactly the
     *      same as calling {@link #FilePersistenceManager(String)}.
     * @param location The configuration file location. If this is
     *      <code>null</code> the <code>config</code> directory below the current
     *      working directory is used.
     *
     * @throws IllegalArgumentException If the location exists but is not a
     *      directory or does not exist and cannot be created.
     * @throws IllegalStateException If the <code>bundleContext</code> is not
     *      valid.
     */
    public FilePersistenceManager( BundleContext bundleContext, String location )
    {
        // no configured location, use the config dir in the bundle persistent
        // area
        if ( location == null && bundleContext != null )
        {
            File locationFile = bundleContext.getDataFile( DEFAULT_CONFIG_DIR );
            if ( locationFile != null )
            {
                location = locationFile.getAbsolutePath();
            }
        }

        // fall back to the current working directory if the platform does
        // not support filesystem based data area
        if ( location == null )
        {
            location = System.getProperty( "user.dir" ) + "/config";
        }

        // ensure the file is absolute
        File locationFile = new File( location );
        if ( !locationFile.isAbsolute() )
        {
            if ( bundleContext != null )
            {
                File bundleLocationFile = bundleContext.getDataFile( locationFile.getPath() );
                if ( bundleLocationFile != null )
                {
                    locationFile = bundleLocationFile;
                }
            }

            // ensure the file object is an absolute file object
            locationFile = locationFile.getAbsoluteFile();
        }

        // check the location
        if ( !locationFile.isDirectory() )
        {
            if ( locationFile.exists() )
            {
                throw new IllegalArgumentException( location + " is not a directory" );
            }

            if ( !locationFile.mkdirs() )
            {
                throw new IllegalArgumentException( "Cannot create directory " + location );
            }
        }

        this.location = locationFile;
    }


    /**
     * Returns the directory in which the configuration files are written as
     * a <code>File</code> object.
     *
     * @return The configuration file location.
     */
    public File getLocation()
    {
        return location;
    }


    /**
     * Loads configuration data from the configuration location and returns
     * it as <code>Dictionary</code> objects.
     * <p>
     * This method is a lazy implementation, which is just one configuration
     * file ahead of the current enumeration location.
     *
     * @return an enumeration of configuration data returned as instances of
     *      the <code>Dictionary</code> class.
     */
    public Enumeration getDictionaries()
    {
        return new DictionaryEnumeration();
    }


    /**
     * Deletes the file for the given identifier.
     *
     * @param pid The identifier of the configuration file to delete.
     */
    public void delete( String pid )
    {
        getFile( pid ).delete();
    }


    /**
     * Returns <code>true</code> if a (configuration) file exists for the given
     * identifier.
     *
     * @param pid The identifier of the configuration file to check.
     *
     * @return <code>true</code> if the file exists
     */
    public boolean exists( String pid )
    {
        return getFile( pid ).isFile();
    }


    /**
     * Reads the (configuration) for the given identifier into a
     * <code>Dictionary</code> object.
     *
     * @param pid The identifier of the configuration file to delete.
     *
     * @return The configuration read from the file. This <code>Dictionary</code>
     *      may be empty if the file contains no configuration information
     *      or is not properly formatted.
     */
    public Dictionary load( String pid ) throws IOException
    {
        return load( getFile( pid ) );
    }


    /**
     * Stores the contents of the <code>Dictionary</code> in a file denoted
     * by the given identifier.
     *
     * @param pid The identifier of the configuration file to which to write
     *      the configuration contents.
     * @param props The configuration data to write.
     *
     * @throws IOException If an error occurrs writing the configuration data.
     */
    public void store( String pid, Dictionary props ) throws IOException
    {
        OutputStream out = null;
        File tmpFile = null;
        try
        {
            File cfgFile = getFile( pid );

            // ensure parent path
            File cfgDir = cfgFile.getParentFile();
            cfgDir.mkdirs();

            // write the configuration to a temporary file
            tmpFile = File.createTempFile( cfgFile.getName(), TMP_EXT, cfgDir );
            out = new FileOutputStream( tmpFile );
            ConfigurationHandler.write( out, props );
            out.close();

            // after writing the file, rename it but ensure, that no other
            // might at the same time open the new file
            // see load(File)
            synchronized (this) {
                // make sure the cfg file does not exists (just for sanity)
                if (cfgFile.exists()) {
                    cfgFile.delete();
                }

                // rename the temporary file to the new file
                tmpFile.renameTo( cfgFile );
            }
        }
        finally
        {
            if ( out != null )
            {
                try
                {
                    out.close();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }

            if (tmpFile != null && tmpFile.exists())
            {
                tmpFile.delete();
            }
        }
    }


    /**
     * Loads the contents of the <code>cfgFile</code> into a new
     * <code>Dictionary</code> object.
     *
     * @param cfgFile The file from which to load the data.
     *
     * @return A new <code>Dictionary</code> object providing the file contents.
     *
     * @throws java.io.FileNotFoundException If the given file does not exist.
     * @throws IOException If an error occurrs reading the configuration file.
     */
    private Dictionary load( File cfgFile ) throws IOException
    {
        // synchronize this instance to make at least sure, the file is
        // not at the same time accessed by another thread (see store())
        // we have to synchronize the complete load time as the store
        // method might want to replace the file while we are reading and
        // still have the file open. This might be a problem e.g. in Windows
        // environments, where files may not be removed which are still open
        synchronized ( this )
        {
            InputStream ins = null;
            try
            {
                ins = new FileInputStream( cfgFile );
                return ConfigurationHandler.read( ins );
            }
            finally
            {
                if ( ins != null )
                {
                    try
                    {
                        ins.close();
                    }
                    catch ( IOException ioe )
                    {
                        // ignore
                    }
                }
            }
        }
    }


    /**
     * Creates an abstract path name for the <code>pid</code> encoding it as
     * follows:
     * <ul>
     * <li>Dots (<code>.</code>) are replaced by <code>File.separatorChar</code>
     * <li>Characters not matching [a-zA-Z0-9 _-] are encoded with a percent
     *  character (<code>%</code>) and a 4-place hexadecimal unicode value.
     * </ul>
     * Before returning the path name, the parent directory and any ancestors
     * are created.
     *
     * @param pid The identifier for which to create the abstract file name.
     *
     * @return The abstract path name, which the parent directory path created.
     */
    private File getFile( String pid )
    {
        return new File( location, encodePid( pid ) + FILE_EXT );
    }

    /**
     * The <code>DictionaryEnumeration</code> class implements the
     * <code>Enumeration</code> returning configuration <code>Dictionary</code>
     * objects on behalf of the {@link FilePersistenceManager#getDictionaries()}
     * method.
     * <p>
     * This enumeration loads configuration lazily with a look ahead of one
     * dictionary.
     *
     * @author fmeschbe
     */
    private class DictionaryEnumeration implements Enumeration
    {
        private Stack dirStack;
        private File[] fileList;
        private int idx;
        private Dictionary next;


        DictionaryEnumeration()
        {
            dirStack = new Stack();
            fileList = null;
            idx = 0;

            dirStack.push( location );
            next = seek();
        }


        public boolean hasMoreElements()
        {
            return next != null;
        }


        public Object nextElement()
        {
            if ( next == null )
            {
                throw new NoSuchElementException();
            }

            Dictionary toReturn = next;
            next = seek();
            return toReturn;
        }


        private Dictionary seek()
        {
            while ( ( fileList != null && idx < fileList.length ) || !dirStack.isEmpty() )
            {
                if ( fileList == null || idx >= fileList.length )
                {
                    File dir = ( File ) dirStack.pop();
                    fileList = dir.listFiles();
                    idx = 0;
                }
                else
                {

                    File cfgFile = fileList[idx++];
                    if ( cfgFile.isFile() && !cfgFile.getName().endsWith( TMP_EXT ))
                    {
                        try
                        {
                            Dictionary dict =  load( cfgFile );

                            // use the dictionary if it has no PID or the PID
                            // derived file name matches the source file name
                            if ( dict.get( Constants.SERVICE_PID ) == null
                                || cfgFile.equals( getFile( ( String ) dict.get( Constants.SERVICE_PID ) ) ) )
                            {
                                return dict;
                            }
                        }
                        catch ( IOException ioe )
                        {
                            // ignore, check next file
                        }
                    }
                    else if ( cfgFile.isDirectory() )
                    {
                        dirStack.push( cfgFile );
                    }
                }
            }

            // exhausted
            return null;
        }
    }
}
