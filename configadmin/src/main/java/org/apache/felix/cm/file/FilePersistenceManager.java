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
import java.util.Hashtable;
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
 * The configuration directory may be set by using the
 * {@link #FilePersistenceManager(String)} naming the path to the directry. When
 * this persistence manager is used by the Configuration Admin Service, the
 * location may be configured using the {@link #CM_CONFIG_DIR} bundle context
 * property.
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
 * <tr><td><code>sample.fl�che</code><td><code>sample/fl%00e8che.config</code></tr>
 * </table>
 *
 * @author fmeschbe
 */
public class FilePersistenceManager implements PersistenceManager
{

    /**
     * The extension of the configuration files.
     */
    private static final String FILE_EXT = ".config";

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
     * Creates an instance of this persistence manager using the given location
     * as the directory to store and retrieve the configuration files.
     * 
     * @param location The configuration file location. If this is
     *      <code>null</code> the <code>config</code> directory below the current
     *      working directory is used.
     * 
     * @throws IllegalArgumentException If the location exists but is not a
     *      directory or does not exist and cannot be created.
     */
    public FilePersistenceManager( String location )
    {
        if ( location == null )
        {
            location = System.getProperty( "user.dir" ) + "/config";
        }

        // check the location
        File locationFile = new File( location );
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
        try
        {
            File cfgFile = getFile( pid );
            
            // ensure parent path
            cfgFile.getParentFile().mkdirs();

            
            out = new FileOutputStream( cfgFile );
            ConfigurationHandler.write( out, props );
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

        return new File( location, pid + FILE_EXT );
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
                    if ( cfgFile.isFile() )
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
