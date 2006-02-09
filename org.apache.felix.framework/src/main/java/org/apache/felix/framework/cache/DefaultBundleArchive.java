/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.cache;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.*;
import org.apache.felix.moduleloader.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

/**
 * <p>
 * This class, combined with <tt>DefaultBundleCache</tt>, implements the
 * default file system-based bundle cache for Felix.
 * </p>
 * @see org.apache.felix.framework.util.DefaultBundleCache
**/
public class DefaultBundleArchive implements BundleArchive
{
    private static final transient String BUNDLE_JAR_FILE = "bundle.jar";
    private static final transient String BUNDLE_LOCATION_FILE = "bundle.location";
    private static final transient String BUNDLE_STATE_FILE = "bundle.state";
    private static final transient String BUNDLE_START_LEVEL_FILE = "bundle.startlevel";
    private static final transient String REFRESH_COUNTER_FILE = "refresh.counter";
    private static final transient String BUNDLE_ACTIVATOR_FILE = "bundle.activator";

    private static final transient String REVISION_DIRECTORY = "version";
    private static final transient String EMBEDDED_DIRECTORY = "embedded";
    private static final transient String LIBRARY_DIRECTORY = "lib";
    private static final transient String DATA_DIRECTORY = "data";

    private static final transient String ACTIVE_STATE = "active";
    private static final transient String INSTALLED_STATE = "installed";
    private static final transient String UNINSTALLED_STATE = "uninstalled";

    private Logger m_logger = null;
    private long m_id = -1;
    private File m_dir = null;
    private String m_location = null;
    private int m_persistentState = -1;
    private int m_startLevel = -1;
    private Map m_currentHeader = null;

    private long m_refreshCount = -1;
    private int m_revisionCount = -1;

    public DefaultBundleArchive(
        Logger logger, File dir, long id, String location, InputStream is)    
        throws Exception
    {
        this(logger, dir, id);
        m_location = location;

        // Try to save and pre-process the bundle JAR.
        try
        {
            initialize(is);
        }
        catch (Exception ex)
        {
            if (!deleteDirectoryTree(dir))
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Unable to delete the archive directory: " + id);
            }
            throw ex;
        }
    }

    public DefaultBundleArchive(Logger logger, File dir, long id)
    {
        m_logger = logger;
        m_dir = dir;
        m_id = id;
        if (m_id <= 0)
        {
            throw new IllegalArgumentException(
                "Bundle ID cannot be less than or equal to zero.");
        }
    }

    private void initialize(InputStream is)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.INITIALIZE_ACTION, this, is));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            initializeUnchecked(is);
        }
    }

    private void initializeUnchecked(InputStream is)
        throws Exception
    {
        FileWriter fw = null;
        BufferedWriter bw = null;

        try
        {
            // Create archive directory.
            if (!m_dir.mkdir())
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "DefaultBundleArchive: Unable to create archive directory.");
                throw new IOException("Unable to create archive directory.");
            }

            // Save location string.
            File file = new File(m_dir, BUNDLE_LOCATION_FILE);
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write(m_location, 0, m_location.length());

            // Create version/revision directory for bundle JAR.
            // Since this is only called when the bundle JAR is
            // first saved, the update and revision will always
            // be "0.0" for the directory name.
            File revisionDir = new File(m_dir, REVISION_DIRECTORY + "0.0");
            if (!revisionDir.mkdir())
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "DefaultBundleArchive: Unable to create revision directory.");
                throw new IOException("Unable to create revision directory.");
            }

            // Save the bundle jar file.
            file = new File(revisionDir, BUNDLE_JAR_FILE);
            copy(is, file);

            // This will always be revision zero.
            preprocessBundleJar(0, revisionDir);

        }
        finally
        {
            if (is != null) is.close();
            if (bw != null) bw.close();
            if (fw != null) fw.close();
        }
    }

    public File getDirectory()
    {
        return m_dir;
    }

    public long getId()
    {
        return m_id;
    }

    public String getLocation()
        throws Exception
    {
        if (m_location != null)
        {
            return m_location;
        }
        else if (System.getSecurityManager() != null)
        {
            try
            {
                return (String) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.GET_LOCATION_ACTION, this));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            return getLocationUnchecked();
        }
    }

    private String getLocationUnchecked()
        throws Exception
    {
        // Get bundle location file.
        File locFile = new File(m_dir, BUNDLE_LOCATION_FILE);

        // Read bundle location.
        FileReader fr = null;
        BufferedReader br = null;
        try
        {
            fr = new FileReader(locFile);
            br = new BufferedReader(fr);
            m_location = br.readLine();
            return m_location;
        }
        finally
        {
            if (br != null) br.close();
            if (fr != null) fr.close();
        }
    }

    public int getPersistentState()
        throws Exception
    {
        if (m_persistentState >= 0)
        {
            return m_persistentState;
        }
        else if (System.getSecurityManager() != null)
        {
            try
            {
                return ((Integer) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.GET_PERSISTENT_STATE_ACTION, this))).intValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            return getPersistentStateUnchecked();
        }
    }

    private int getPersistentStateUnchecked()
        throws Exception
    {
        // Get bundle state file.
        File stateFile = new File(m_dir, BUNDLE_STATE_FILE);

        // If the state file doesn't exist, then
        // assume the bundle was installed.
        if (!stateFile.exists())
        {
            return Bundle.INSTALLED;
        }

        // Read the bundle state.
        FileReader fr = null;
        BufferedReader br= null;
        try
        {
            fr = new FileReader(stateFile);
            br = new BufferedReader(fr);
            String s = br.readLine();
            if (s.equals(ACTIVE_STATE))
            {
                m_persistentState = Bundle.ACTIVE;
            }
            else if (s.equals(UNINSTALLED_STATE))
            {
                m_persistentState = Bundle.UNINSTALLED;
            }
            else
            {
                m_persistentState = Bundle.INSTALLED;
            }
            return m_persistentState;
        }
        finally
        {
            if (br != null) br.close();
            if (fr != null) fr.close();
        }
    }

    public void setPersistentState(int state)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.SET_PERSISTENT_STATE_ACTION, this, state));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            setPersistentStateUnchecked(state);
        }
    }

    private void setPersistentStateUnchecked(int state)
        throws Exception
    {
        // Get bundle state file.
        File stateFile = new File(m_dir, BUNDLE_STATE_FILE);

        // Write the bundle state.
        FileWriter fw = null;
        BufferedWriter bw= null;
        try
        {
            fw = new FileWriter(stateFile);
            bw = new BufferedWriter(fw);
            String s = null;
            switch (state)
            {
                case Bundle.ACTIVE:
                    s = ACTIVE_STATE;
                    break;
                case Bundle.UNINSTALLED:
                    s = UNINSTALLED_STATE;
                    break;
                default:
                    s = INSTALLED_STATE;
                    break;
            }
            bw.write(s, 0, s.length());
            m_persistentState = state;
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "DefaultBundleArchive: Unable to record state: " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (fw != null) fw.close();
        }
    }

    public int getStartLevel()
        throws Exception
    {
        if (m_startLevel >= 0)
        {
            return m_startLevel;
        }
        else if (System.getSecurityManager() != null)
        {
            try
            {
                return ((Integer) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.GET_START_LEVEL_ACTION, this))).intValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            return getStartLevelUnchecked();
        }
    }

    private int getStartLevelUnchecked()
        throws Exception
    {
        // Get bundle start level file.
        File levelFile = new File(m_dir, BUNDLE_START_LEVEL_FILE);

        // If the start level file doesn't exist, then
        // return an error.
        if (!levelFile.exists())
        {
            return -1;
        }

        // Read the bundle start level.
        FileReader fr = null;
        BufferedReader br= null;
        try
        {
            fr = new FileReader(levelFile);
            br = new BufferedReader(fr);
            m_startLevel = Integer.parseInt(br.readLine());
            return m_startLevel;
        }
        finally
        {
            if (br != null) br.close();
            if (fr != null) fr.close();
        }
    }

    public void setStartLevel(int level)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.SET_START_LEVEL_ACTION, this, level));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            setStartLevelUnchecked(level);
        }
    }

    private void setStartLevelUnchecked(int level)
        throws Exception
    {
        // Get bundle start level file.
        File levelFile = new File(m_dir, BUNDLE_START_LEVEL_FILE);

        // Write the bundle start level.
        FileWriter fw = null;
        BufferedWriter bw = null;
        try
        {
            fw = new FileWriter(levelFile);
            bw = new BufferedWriter(fw);
            String s = Integer.toString(level);
            bw.write(s, 0, s.length());
            m_startLevel = level;
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "DefaultBundleArchive: Unable to record start leel: " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (fw != null) fw.close();
        }
    }

    public File getDataFile(String fileName)
        throws Exception
    {
        // Do some sanity checking.
        if ((fileName.length() > 0) && (fileName.charAt(0) == File.separatorChar))
            throw new IllegalArgumentException("The data file path must be relative, not absolute.");
        else if (fileName.indexOf("..") >= 0)
            throw new IllegalArgumentException("The data file path cannot contain a reference to the \"..\" directory.");

        // Get bundle data directory.
        File dataDir = new File(m_dir, DATA_DIRECTORY);

        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.CREATE_DATA_DIR_ACTION, this, dataDir));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            createDataDirectoryUnchecked(dataDir);
        }

        // Return the data file.
        return new File(dataDir, fileName);
    }

    private void createDataDirectoryUnchecked(File dir)
        throws Exception
    {
        // Create data directory if necessary.
        if (!dir.exists())
        {
            if (!dir.mkdir())
            {
                throw new IOException("Unable to create bundle data directory.");
            }
        }
    }

    public BundleActivator getActivator(IContentLoader contentLoader)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                return (BundleActivator) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.GET_ACTIVATOR_ACTION, this, contentLoader));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            return getActivatorUnchecked(contentLoader);
        }
    }

    private BundleActivator getActivatorUnchecked(IContentLoader contentLoader)
        throws Exception
    {
        // Get bundle activator file.
        File activatorFile = new File(m_dir, BUNDLE_ACTIVATOR_FILE);
        // If the activator file doesn't exist, then
        // assume there isn't one.
        if (!activatorFile.exists())
            return null;

        // Deserialize the activator object.
        InputStream is = null;
        ObjectInputStreamX ois = null;
        try
        {
            is = new FileInputStream(activatorFile);
            ois = new ObjectInputStreamX(is, contentLoader);
            Object o = ois.readObject();
            return (BundleActivator) o;
        }
        catch (Exception ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "DefaultBundleArchive: Trying to deserialize - " + ex);
        }
        finally
        {
            if (ois != null) ois.close();
            if (is != null) is.close();
        }

        return null;
    }

    public void setActivator(Object obj)
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.SET_ACTIVATOR_ACTION, this, obj));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            setActivatorUnchecked(obj);
        }
    }

    private void setActivatorUnchecked(Object obj)
        throws Exception
    {
        if (!(obj instanceof Serializable))
        {
            return;
        }

        // Get bundle activator file.
        File activatorFile = new File(m_dir, BUNDLE_ACTIVATOR_FILE);

        // Serialize the activator object.
        OutputStream os = null;
        ObjectOutputStream oos = null;
        try
        {
            os = new FileOutputStream(activatorFile);
            oos = new ObjectOutputStream(os);
            oos.writeObject(obj);
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "DefaultBundleArchive: Unable to serialize activator - " + ex);
            throw ex;
        }
        finally
        {
            if (oos != null) oos.close();
            if (os != null) os.close();
        }
    }

    public int getRevisionCount()
        throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                return ((Integer) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.GET_REVISION_COUNT_ACTION, this))).intValue();
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            return getRevisionCountUnchecked();
        }
    }

    public int getRevisionCountUnchecked()
    {
        // We should always have at least one revision
        // directory, so try to count them if the value
        // has not been initialized yet.
        if (m_revisionCount <= 0)
        {
            m_revisionCount = 0;
            File[] children = m_dir.listFiles();
            for (int i = 0; (children != null) && (i < children.length); i++)
            {
                if (children[i].getName().startsWith(REVISION_DIRECTORY))
                {
                    m_revisionCount++;
                }
            }
        }
        return m_revisionCount;
    }

    public Map getManifestHeader(int revision)
        throws Exception
    {
        // If the request is for the current revision header,
        // then return the cached copy if it is present.
        if ((revision == (getRevisionCount() - 1)) && (m_currentHeader != null))
        {
            return m_currentHeader;
        }

        // Get the revision directory.
        File revisionDir = new File(
            m_dir, REVISION_DIRECTORY + getRefreshCount() + "." + revision);

        // Get the embedded resource.
        JarFileX jarFile = null;

        try
        {
            // Open bundle JAR file.
            jarFile = openBundleJar(revisionDir);
            // Error if no jar file.
            if (jarFile == null)
            {
                throw new IOException("No JAR file found.");
            }
            // Get manifest.
            Manifest mf = jarFile.getManifest();
            // Create a case insensitive map of manifest attributes.
            Map map = new StringMap(mf.getMainAttributes(), false);
            // If the request is for the current revision's header,
            // then cache it.
            if (revision == (getRevisionCount() - 1))
            {
                m_currentHeader = map;
            }
            return map;

        }
        finally
        {
            if (jarFile != null) jarFile.close();
        }
    }

    private JarFileX openBundleJarUnchecked(File revisionDir)
        throws Exception
    {
        // Get bundle jar file.
        File bundleJar = new File(revisionDir, BUNDLE_JAR_FILE);
        // Get bundle jar file.
        return new JarFileX(bundleJar);
    }

    private JarFileX openBundleJar(File revisionDir) throws Exception
    {
        // Create JarFile object using privileged block.
        if (System.getSecurityManager() != null)
        {
            try
            {
                return (JarFileX) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.OPEN_BUNDLE_JAR_ACTION, this, revisionDir));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }

        return openBundleJarUnchecked(revisionDir);
    }

    private IContent getContentUnchecked(int revision)
        throws Exception
    {
        // Get the revision directory.
        File revisionDir = new File(
            m_dir, REVISION_DIRECTORY + getRefreshCount() + "." + revision);
        // Return a content object for the bundle itself.
        return new JarContent(new File(revisionDir, BUNDLE_JAR_FILE));
    }

    public IContent getContent(int revision)
        throws Exception
    {
        IContent content = null;
    
        if (System.getSecurityManager() != null)
        {
            try
            {
                content = (IContent) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.GET_CONTENT_ACTION, this, revision));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            content = getContentUnchecked(revision);
        }
    
        return content;
    }

    private IContent[] getContentPathUnchecked(int revision)
        throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        // Get the revision directory.
        File revisionDir = new File(
            m_dir, REVISION_DIRECTORY + getRefreshCount() + "." + revision);
        File embedDir = new File(revisionDir, EMBEDDED_DIRECTORY);

        // Get the bundle's manifest header.
        Map map = getManifestHeader(revision);
        if (map == null)
        {
            map = new HashMap();
        }

        // Find class path meta-data.
        String classPathString = null;
        Iterator iter = map.entrySet().iterator();
        while ((classPathString == null) && iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getKey().toString().toLowerCase().equals(
                FelixConstants.BUNDLE_CLASSPATH.toLowerCase()))
            {
                classPathString = entry.getValue().toString();
            }
        }

        // Parse the class path into strings.
        String[] classPathStrings = Util.parseDelimitedString(
            classPathString, FelixConstants.CLASS_PATH_SEPARATOR);

        if (classPathStrings == null)
        {
            classPathStrings = new String[0];
        }

        // Create the bundles class path.
        JarFileX bundleJar = null;
        try
        {
            bundleJar = openBundleJar(revisionDir);
            IContent self = new JarContent(new File(revisionDir, BUNDLE_JAR_FILE));
            IContent[] classPath = new IContent[classPathStrings.length];
            for (int i = 0; i < classPathStrings.length; i++)
            {
                if (classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
                {
                    classPath[i] = self;
                }
                else
                {
                    // Determine if the class path entry is a file or directory
                    // in the bundle JAR file.
                    ZipEntry entry = bundleJar.getEntry(classPathStrings[i]);
                    if ((entry != null) && entry.isDirectory())
                    {
                        classPath[i] = new ContentDirectoryContent(self, classPathStrings[i]);
                    }
                    else
                    {
                        classPath[i] = new JarContent(new File(embedDir, classPathStrings[i]));
                    }
                }
            }
    
            // If there is nothing on the class path, then include
            // "." by default, as per the spec.
            if (classPath.length == 0)
            {
                classPath = new IContent[] { self };
            }

            return classPath;
        }
        finally
        {
            if (bundleJar != null) bundleJar.close();
        }
    }

    public IContent[] getContentPath(int revision)
        throws Exception
    {
        IContent[] contents = null;

        if (System.getSecurityManager() != null)
        {
            try
            {
                contents = (IContent[]) AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.GET_CONTENT_PATH_ACTION, this, revision));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            contents = getContentPathUnchecked(revision);
        }

        return contents;
    }

//  TODO: This will need to consider security.
    public String findLibrary(int revision, String libName)
        throws Exception
    {
        return findLibraryUnchecked(revision, libName);
    }

    private String findLibraryUnchecked(int revision, String libName)
        throws Exception
    {
        // Get the revision directory.
        File revisionDir = new File(
            m_dir.getAbsoluteFile(),
            REVISION_DIRECTORY + getRefreshCount() + "." + revision);

        // Get bundle lib directory.
        File libDir = new File(revisionDir, LIBRARY_DIRECTORY);
        // Get lib file.
        File libFile = new File(libDir, File.separatorChar + libName);
        // Make sure that the library's parent directory exists;
        // it may be in a sub-directory.
        libDir = libFile.getParentFile();
        if (!libDir.exists())
        {
            if (!libDir.mkdirs())
            {
                throw new IOException("Unable to create library directory.");
            }
        }
        // Extract the library from the JAR file if it does not
        // already exist.
        if (!libFile.exists())
        {
            JarFileX jarFile = null;
            InputStream is = null;

            try
            {
                jarFile = openBundleJarUnchecked(revisionDir);
                ZipEntry ze = jarFile.getEntry(libName);
                if (ze == null)
                {
                    throw new IOException("No JAR entry: " + libName);
                }
                is = new BufferedInputStream(
                    jarFile.getInputStream(ze), DefaultBundleCache.BUFSIZE);
                if (is == null)
                {
                    throw new IOException("No input stream: " + libName);
                }

                // Create the file.
                copy(is, libFile);

            }
            finally
            {
                if (jarFile != null) jarFile.close();
                if (is != null) is.close();
            }
        }

        return libFile.toString();
    }

    /**
     * This utility method is used to retrieve the current refresh
     * counter value for the bundle. This value is used when generating
     * the bundle JAR directory name where native libraries are extracted.
     * This is necessary because Sun's JVM requires a one-to-one mapping
     * between native libraries and class loaders where the native library
     * is uniquely identified by its absolute path in the file system. This
     * constraint creates a problem when a bundle is refreshed, because it
     * gets a new class loader. Using the refresh counter to generate the name
     * of the bundle JAR directory resolves this problem because each time
     * bundle is refresh, the native library will have a unique name.
     * As a result of the unique name, the JVM will then reload the
     * native library without a problem.
    **/
    private long getRefreshCount()
        throws Exception
    {
        // If we have already read the update counter file,
        // then just return the result.
        if (m_refreshCount >= 0)
        {
            return m_refreshCount;
        }

        // Get update counter file.
        File counterFile = new File(m_dir, REFRESH_COUNTER_FILE);

        // If the update counter file doesn't exist, then
        // assume the counter is at zero.
        if (!counterFile.exists())
        {
            return 0;
        }

        // Read the bundle update counter.
        FileReader fr = null;
        BufferedReader br = null;
        try
        {
            fr = new FileReader(counterFile);
            br = new BufferedReader(fr);
            long counter = Long.parseLong(br.readLine());
            return counter;
        }
        finally
        {
            if (br != null) br.close();
            if (fr != null) fr.close();
        }
    }

    /**
     * This utility method is used to retrieve the current refresh
     * counter value for the bundle. This value is used when generating
     * the bundle JAR directory name where native libraries are extracted.
     * This is necessary because Sun's JVM requires a one-to-one mapping
     * between native libraries and class loaders where the native library
     * is uniquely identified by its absolute path in the file system. This
     * constraint creates a problem when a bundle is refreshed, because it
     * gets a new class loader. Using the refresh counter to generate the name
     * of the bundle JAR directory resolves this problem because each time
     * bundle is refresh, the native library will have a unique name.
     * As a result of the unique name, the JVM will then reload the
     * native library without a problem.
    **/
    private void setRefreshCount(long counter)
        throws Exception
    {
        // Get update counter file.
        File counterFile = new File(m_dir, REFRESH_COUNTER_FILE);

        // Write the update counter.
        FileWriter fw = null;
        BufferedWriter bw = null;
        try
        {
            fw = new FileWriter(counterFile);
            bw = new BufferedWriter(fw);
            String s = Long.toString(counter);
            bw.write(s, 0, s.length());
            m_refreshCount = counter;
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "DefaultBundleArchive: Unable to write counter: " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (fw != null) fw.close();
        }
    }

    //
    // File-oriented utility methods.
    //

    protected static boolean deleteDirectoryTree(File target)
    {
        if (!target.exists())
        {
            return true;
        }

        if (target.isDirectory())
        {
            File[] files = target.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                deleteDirectoryTree(files[i]);
            }
        }

        return target.delete();
    }

    /**
     * This method copies an input stream to the specified file.
     * <p>
     * Security: This method must be called from within a <tt>doPrivileged()</tt>
     * block since it accesses the disk.
     * @param is the input stream to copy.
     * @param outputFile the file to which the input stream should be copied.
    **/
    private void copy(InputStream is, File outputFile)
        throws IOException
    {
        OutputStream os = null;

        try
        {
            os = new BufferedOutputStream(
                new FileOutputStream(outputFile), DefaultBundleCache.BUFSIZE);
            byte[] b = new byte[DefaultBundleCache.BUFSIZE];
            int len = 0;
            while ((len = is.read(b)) != -1)
            {
                os.write(b, 0, len);
            }
        }
        finally
        {
            if (is != null) is.close();
            if (os != null) os.close();
        }
    }

    /**
     * This method pre-processes a bundle JAR file making it ready
     * for use. This entails extracting all embedded JAR files and
     * all native libraries.
     * @throws java.lang.Exception if any error occurs while processing JAR file.
    **/
    private void preprocessBundleJar(int revision, File revisionDir)
        throws Exception
    {
        //
        // Create special directories so that we can avoid checking
        // for their existence all the time.
        //

        File embedDir = new File(revisionDir, EMBEDDED_DIRECTORY);
        if (!embedDir.exists())
        {
            if (!embedDir.mkdir())
            {
                throw new IOException("Could not create embedded JAR directory.");
            }
        }

        File libDir = new File(revisionDir, LIBRARY_DIRECTORY);
        if (!libDir.exists())
        {
            if (!libDir.mkdir())
            {
                throw new IOException("Unable to create native library directory.");
            }
        }

        //
        // This block extracts all embedded JAR files.
        //

        try
        {
            // Get the bundle's manifest header.
            Map map = getManifestHeader(revision);

            // Find class path meta-data.
            String classPath = (map == null)
                ? null : (String) map.get(FelixConstants.BUNDLE_CLASSPATH);

            // Parse the class path into strings.
            String[] classPathStrings = Util.parseDelimitedString(
                classPath, FelixConstants.CLASS_PATH_SEPARATOR);

            if (classPathStrings == null)
            {
                classPathStrings = new String[0];
            }

            for (int i = 0; i < classPathStrings.length; i++)
            {
                if (!classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
                {
                    extractEmbeddedJar(revisionDir, classPathStrings[i]);
                }
            }

        }
        catch (PrivilegedActionException ex)
        {
            throw ((PrivilegedActionException) ex).getException();
        }
    }

    /**
     * This method extracts an embedded JAR file from the bundle's
     * JAR file.
     * <p>
     * Security: This method must be called from within a <tt>doPrivileged()</tt>
     * block since it accesses the disk.
     * @param id the identifier of the bundle that owns the embedded JAR file.
     * @param jarPath the path to the embedded JAR file inside the bundle JAR file.
    **/
    private void extractEmbeddedJar(File revisionDir, String jarPath)
        throws Exception
    {
        // Remove leading slash if present.
        jarPath = (jarPath.charAt(0) == '/') ? jarPath.substring(1) : jarPath;

        // If JAR is already extracted, then don't re-extract it...
        File jarFile = new File(
            revisionDir, EMBEDDED_DIRECTORY + File.separatorChar + jarPath);

        if (!jarFile.exists())
        {
            JarFileX bundleJar = null;
            InputStream is = null;
            try
            {
                // Make sure class path entry is a JAR file.
                bundleJar = openBundleJarUnchecked(revisionDir);
                ZipEntry ze = bundleJar.getEntry(jarPath);
                if (ze == null)
                {
                    throw new IOException("No JAR entry: " + jarPath);
                }
                // If the zip entry is a directory, then ignore it since
                // we don't need to extact it; otherwise, it points to an
                // embedded JAR file, so extract it.
                else if (!ze.isDirectory())
                {
                    // Make sure that the embedded JAR's parent directory exists;
                    // it may be in a sub-directory.
                    File jarDir = jarFile.getParentFile();
                    if (!jarDir.exists())
                    {
                        if (!jarDir.mkdirs())
                        {
                            throw new IOException("Unable to create embedded JAR directory.");
                        }
                    }
    
                    // Extract embedded JAR into its directory.
                    is = new BufferedInputStream(bundleJar.getInputStream(ze), DefaultBundleCache.BUFSIZE);
                    if (is == null)
                    {
                        throw new IOException("No input stream: " + jarPath);
                    }
                    // Copy the file.
                    copy(is, jarFile);
                }
            }
            finally
            {
                if (bundleJar != null) bundleJar.close();
                if (is != null) is.close();
            }
        }
    }

    // INCREASES THE REVISION COUNT.    
    protected void update(InputStream is) throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.UPDATE_ACTION, this, is));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            updateUnchecked(is);
        }
    }

    // INCREASES THE REVISION COUNT.    
    private void updateUnchecked(InputStream is) throws Exception
    {
        File revisionDir = null;

        try
        {
            // Create the new revision directory.
            int revision = getRevisionCountUnchecked();
            revisionDir = new File(
                m_dir, REVISION_DIRECTORY
                + getRefreshCount() + "." + revision);
            if (!revisionDir.mkdir())
            {
                throw new IOException("Unable to create revision directory.");
            }

            // Save the new revision bundle jar file.
            File file = new File(revisionDir, BUNDLE_JAR_FILE);
            copy(is, file);

            preprocessBundleJar(revision, revisionDir);
        }
        catch (Exception ex)
        {
            if ((revisionDir != null) && revisionDir.exists())
            {
                try
                {
                    deleteDirectoryTree(revisionDir);
                }
                catch (Exception ex2)
                {
                    // There is very little we can do here.
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Unable to remove partial revision directory.", ex2);
                }
            }
            throw ex;
        }

        // If everything was successful, then update
        // the revision count.
        m_revisionCount++;
        // Clear the cached revision header, since it is
        // no longer the current revision.
        m_currentHeader = null;
    }

    // DECREASES THE REVISION COUNT.    
    protected void purge() throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.PURGE_ACTION, this));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            purgeUnchecked();
        }
    }
    
    // DECREASES THE REVISION COUNT.    
    private void purgeUnchecked() throws Exception
    {
        // Get the current update count.
        long update = getRefreshCount();
        // Get the current revision count.
        int count = getRevisionCountUnchecked();

        File revisionDir = null;
        for (int i = 0; i < count - 1; i++)
        {
            revisionDir = new File(m_dir, REVISION_DIRECTORY + update + "." + i);
            if (revisionDir.exists())
            {
                deleteDirectoryTree(revisionDir);
            }
        }
        // Increment the update count.
        setRefreshCount(update + 1);

        // Rename the current revision to be the current update.
        File currentDir = new File(m_dir, REVISION_DIRECTORY + (update + 1) + ".0");
        revisionDir = new File(m_dir, REVISION_DIRECTORY + update + "." + (count - 1));
        revisionDir.renameTo(currentDir);
        
        // If everything is successful, then set the revision
        // count to one.
        m_revisionCount = 1;
        // Although the cached current header should stay the same
        // here, clear it for consistency.
        m_currentHeader = null;
    }

    protected void remove() throws Exception
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.doPrivileged(
                    new PrivilegedAction(
                        PrivilegedAction.REMOVE_ACTION, this));
            }
            catch (PrivilegedActionException ex)
            {
                throw ((PrivilegedActionException) ex).getException();
            }
        }
        else
        {
            removeUnchecked();
        }
    }
    
    private void removeUnchecked() throws Exception
    {
        deleteDirectoryTree(m_dir);
    }

    //
    // Utility class for performing privileged actions.
    //

    private static class PrivilegedAction implements PrivilegedExceptionAction
    {
        private static final int INITIALIZE_ACTION = 0;
        private static final int UPDATE_ACTION = 1;
        private static final int PURGE_ACTION = 2;
        private static final int REMOVE_ACTION = 3;
        private static final int GET_REVISION_COUNT_ACTION = 4;
        private static final int GET_LOCATION_ACTION = 5;
        private static final int GET_PERSISTENT_STATE_ACTION = 6;
        private static final int SET_PERSISTENT_STATE_ACTION = 7;
        private static final int GET_START_LEVEL_ACTION = 8;
        private static final int SET_START_LEVEL_ACTION = 9;
        private static final int OPEN_BUNDLE_JAR_ACTION = 10;
        private static final int CREATE_DATA_DIR_ACTION = 11;
        private static final int GET_CONTENT_ACTION = 12;
        private static final int GET_CONTENT_PATH_ACTION = 13;
        private static final int GET_ACTIVATOR_ACTION = 14;
        private static final int SET_ACTIVATOR_ACTION = 15;

        private int m_action = 0;
        private DefaultBundleArchive m_archive = null;
        private InputStream m_isArg = null;
        private int m_intArg = 0;
        private File m_fileArg = null;
        private IContentLoader m_contentLoaderArg = null;
        private Object m_objArg = null;

        public PrivilegedAction(int action, DefaultBundleArchive archive)
        {
            m_action = action;
            m_archive = archive;
        }

        public PrivilegedAction(int action, DefaultBundleArchive archive, InputStream isArg)
        {
            m_action = action;
            m_archive = archive;
            m_isArg = isArg;
        }

        public PrivilegedAction(int action, DefaultBundleArchive archive, int intArg)
        {
            m_action = action;
            m_archive = archive;
            m_intArg = intArg;
        }

        public PrivilegedAction(int action, DefaultBundleArchive archive, File fileArg)
        {
            m_action = action;
            m_archive = archive;
            m_fileArg = fileArg;
        }

        public PrivilegedAction(int action, DefaultBundleArchive archive, IContentLoader contentLoaderArg)
        {
            m_action = action;
            m_archive = archive;
            m_contentLoaderArg = contentLoaderArg;
        }

        public PrivilegedAction(int action, DefaultBundleArchive archive, Object objArg)
        {
            m_action = action;
            m_archive = archive;
            m_objArg = objArg;
        }

        public Object run() throws Exception
        {
            switch (m_action)
            {
                case INITIALIZE_ACTION:
                    m_archive.initializeUnchecked(m_isArg);
                    return null;
                case UPDATE_ACTION:
                    m_archive.updateUnchecked(m_isArg);
                    return null;
                case PURGE_ACTION:
                    m_archive.purgeUnchecked();
                    return null;
                case REMOVE_ACTION:
                    m_archive.removeUnchecked();
                    return null;
                case GET_REVISION_COUNT_ACTION:
                    return new Integer(m_archive.getRevisionCountUnchecked());
                case GET_LOCATION_ACTION:
                    return m_archive.getLocationUnchecked();
                case GET_PERSISTENT_STATE_ACTION:
                    return new Integer(m_archive.getPersistentStateUnchecked());
                case SET_PERSISTENT_STATE_ACTION:
                    m_archive.setPersistentStateUnchecked(m_intArg);
                    return null;
                case GET_START_LEVEL_ACTION:
                    return new Integer(m_archive.getStartLevelUnchecked());
                case SET_START_LEVEL_ACTION:
                    m_archive.setStartLevelUnchecked(m_intArg);
                    return null;
                case OPEN_BUNDLE_JAR_ACTION:
                    return m_archive.openBundleJarUnchecked(m_fileArg);
                case CREATE_DATA_DIR_ACTION:
                    m_archive.createDataDirectoryUnchecked(m_fileArg);
                    return null;
                case GET_CONTENT_ACTION:
                    return m_archive.getContentUnchecked(m_intArg);
                case GET_CONTENT_PATH_ACTION:
                    return m_archive.getContentPathUnchecked(m_intArg);
                case GET_ACTIVATOR_ACTION:
                    return m_archive.getActivatorUnchecked(m_contentLoaderArg);
                case SET_ACTIVATOR_ACTION:
                    m_archive.setActivatorUnchecked(m_objArg);
                    return null;
            }

            throw new IllegalArgumentException("Invalid action specified.");
        }
    }
}