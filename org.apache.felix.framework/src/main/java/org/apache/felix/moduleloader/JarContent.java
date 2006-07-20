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
package org.apache.felix.moduleloader;

import java.io.*;
import java.util.*;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;

public class JarContent implements IContent
{
    private static final int BUFSIZE = 4096;

    private File m_file = null;
    private JarFileX m_jarFile = null;
    private boolean m_opened = false;

    public JarContent(File file)
    {
        m_file = file;
    }

    protected void finalize()
    {
        if (m_jarFile != null)
        {
            try
            {
                m_jarFile.close();
            }
            catch (IOException ex)
            {
                // Not much we can do, so ignore it.
            }
        }
    }

    public void open()
    {
        m_opened = true;
    }

    public synchronized void close()
    {
        try
        {
            if (m_jarFile != null)
            {
                m_jarFile.close();
            }
        }
        catch (Exception ex)
        {
            System.err.println("JarContent: " + ex);
        }

        m_jarFile = null;
        m_opened = false;
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return false;
            }
        }

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            return ze != null;
        }
        catch (Exception ex)
        {
            return false;
        }
        finally
        {
        }
    }

    public synchronized byte[] getEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return null;
            }
        }

        // Get the embedded resource.
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            if (ze == null)
            {
                return null;
            }
            is = m_jarFile.getInputStream(ze);
            if (is == null)
            {
                return null;
            }
            baos = new ByteArrayOutputStream(BUFSIZE);
            byte[] buf = new byte[BUFSIZE];
            int n = 0;
            while ((n = is.read(buf, 0, buf.length)) >= 0)
            {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();

        }
        catch (Exception ex)
        {
            return null;
        }
        finally
        {
            try
            {
                if (baos != null) baos.close();
            }
            catch (Exception ex)
            {
            }
            try
            {
                if (is != null) is.close();
            }
            catch (Exception ex)
            {
            }
        }
    }

    public synchronized InputStream getEntryAsStream(String name)
        throws IllegalStateException, IOException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return null;
            }
        }

        // Get the embedded resource.
        InputStream is = null;

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            if (ze == null)
            {
                return null;
            }
            is = m_jarFile.getInputStream(ze);
            if (is == null)
            {
                return null;
            }
        }
        catch (Exception ex)
        {
            return null;
        }

        return is;
    }

    public synchronized Enumeration getEntryPaths(String path)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return null;
            }
        }

        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new GetEntryPathsEnumeration(m_jarFile.entries(), path);
        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    public synchronized Enumeration findEntries(
        String path, String filePattern, boolean recurse)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                System.err.println("JarContent: " + ex);
                return null;
            }
        }

        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new FindEntriesEnumeration(
            m_jarFile.entries(), path, filePattern, recurse);
        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    private void openJarFile() throws IOException
    {
        if (m_jarFile == null)
        {
            m_jarFile = new JarFileX(m_file);
        }
    }

    public String toString()
    {
        return "JAR " + m_file.getPath();
    }

    private static class GetEntryPathsEnumeration implements Enumeration
    {
        private Enumeration m_enumeration = null;
        private String m_path = null;
        private Object m_next = null;

        public GetEntryPathsEnumeration(Enumeration enumeration, String path)
        {
            m_enumeration = enumeration;
            // Add a '/' to the end if not present.
            m_path = (path.length() > 0) && (path.charAt(path.length() - 1) != '/')
                ? path + "/" : path;
            m_next = findNext();
        }

        public boolean hasMoreElements()
        {
            return (m_next != null);
        }

        public Object nextElement()
        {
            if (m_next == null)
            {
                throw new NoSuchElementException("No more entry paths.");
            }
            Object last = m_next;
            m_next = findNext();
            return last;
        }

        private Object findNext()
        {
            // This method filters the entries of the zip file, such that
            // it only displays the contents of the directory specified by
            // the path argument; much like using "ls" to list the contents
            // of a directory.
            while (m_enumeration.hasMoreElements())
            {
                // Get the next zip entry.
                ZipEntry entry = (ZipEntry) m_enumeration.nextElement();
                // Check to see if it is a descendent of the specified path.
                if (!entry.getName().equals(m_path) && entry.getName().startsWith(m_path))
                {
                    // Verify that it is a child of the path and not a
                    // grandchild by examining its remaining path length.
                    // This code uses the knowledge that zip entries
                    // corresponding to directories end in '/'. It checks
                    // to see if the next occurrence of '/' is also the
                    // end of the string, which means that this entry
                    // represents a child directory of the path.
                    int idx = entry.getName().indexOf('/', m_path.length());
                    if ((idx < 0) || (idx == (entry.getName().length() - 1)))
                    {
                        return entry.getName();
                    }
                }
            }
            return null;
        }
    }

    private static class FindEntriesEnumeration implements Enumeration
    {
        private Enumeration m_enumeration = null;
        private String m_path = null;
        private String[] m_filePattern = null;
        private boolean m_recurse = false;
        private Object m_next = null;

        public FindEntriesEnumeration(
            Enumeration enumeration, String path, String filePattern, boolean recurse)
        {
            m_enumeration = enumeration;
            // Add a '/' to the end if not present.
            m_path = (path.length() > 0) && (path.charAt(path.length() - 1) != '/')
                ? path + "/" : path;
            m_filePattern = parseSubstring(filePattern);
            m_recurse = recurse;
            m_next = findNext();
        }

        public boolean hasMoreElements()
        {
            return (m_next != null);
        }

        public Object nextElement()
        {
            if (m_next == null)
            {
                throw new NoSuchElementException("No more entry paths.");
            }
            Object last = m_next;
            m_next = findNext();
            return last;
        }

        private Object findNext()
        {
            // This method filters the entries of the zip file, such that
            // it only displays the contents of the directory specified by
            // the path argument either recursively or not; much like using
            // "ls -R" or "ls" to list the contents of a directory, respectively.
            while (m_enumeration.hasMoreElements())
            {
                // Get the next zip entry.
                ZipEntry entry = (ZipEntry) m_enumeration.nextElement();
                String entryName = entry.getName();
                // Check to see if it is a descendent of the specified path.
                if (!entryName.equals(m_path) && entryName.startsWith(m_path))
                {
                    // NOTE: We assume here that directories are not returned,
                    // unlike getEntryPaths() above, where directories are returned;
                    // this may or may not be the correct spec interpretation.

                    // If this is recursive, then simply verify that the
                    // entry is not a directory my making sure it does not
                    // end with '/'. If this is not recursive, then verify
                    // that the entry is a child of the path and not a
                    // grandchild by examining its remaining path length.
                    // This code uses the knowledge that zip entries
                    // corresponding to directories end in '/'.
                    int idx = entryName.indexOf('/', m_path.length());
                    if ((m_recurse && (entryName.charAt(entryName.length() - 1) != '/'))
                        || (idx < 0))
                    {
                        // Get the last element of the path.
                        idx = entryName.lastIndexOf('/');
                        String lastElement = entryName;
                        if (idx >= 0)
                        {
                            lastElement = entryName.substring(idx + 1);
                        }
                        // See if the file pattern matches the last element of the path.
                        if (checkSubstring(m_filePattern, lastElement))
                        {
                            return entry.getName();
                        }
                    }
                }
            }
            return null;
        }
    }

    //
    // The following substring-related code was lifted and modified
    // from the LDAP parser code.
    //

    private static String[] parseSubstring(String target)
    {
        List pieces = new ArrayList();
        StringBuffer ss = new StringBuffer();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
loop:   for (;;)
        {
            if (idx >= target.length())
            {
                if (wasStar)
                {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                }
                else
                {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break loop;
            }

            char c = target.charAt(idx++);
            if (c == '*')
            {
                if (wasStar)
                {
                    // encountered two successive stars;
                    // I assume this is illegal
                    throw new IllegalArgumentException("Invalid filter string: " + target);
                }
                if (ss.length() > 0)
                {
                    pieces.add(ss.toString()); // accumulate the pieces
                    // between '*' occurrences
                }
                ss.setLength(0);
                // if this is a leading star, then track it
                if (pieces.size() == 0)
                {
                    leftstar = true;
                }
                ss.setLength(0);
                wasStar = true;
            }
            else
            {
                wasStar = false;
                ss.append(c);
            }
        }
        if (leftstar || rightstar || pieces.size() > 1)
        {
            // insert leading and/or trailing "" to anchor ends
            if (rightstar)
            {
                pieces.add("");
            }
            if (leftstar)
            {
                pieces.add(0, "");
            }
        }
        return (String[]) pieces.toArray(new String[pieces.size()]);
    }

    private static boolean checkSubstring(String[] pieces, String s)
    {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = false;
        int len = pieces.length;

loop:   for (int i = 0; i < len; i++)
        {
            String piece = (String) pieces[i];
            int index = 0;
            if (i == len - 1)
            {
                // this is the last piece
                if (s.endsWith(piece))
                {
                    result = true;
                }
                else
                {
                    result = false;
                }
                break loop;
            }
            // initial non-star; assert index == 0
            else if (i == 0)
            {
                if (!s.startsWith(piece))
                {
                    result = false;
                    break loop;
                }
            }
            // assert i > 0 && i < len-1
            else
            {
                // Sure wish stringbuffer supported e.g. indexOf
                index = s.indexOf(piece, index);
                if (index < 0)
                {
                    result = false;
                    break loop;
                }
            }
            // start beyond the matching piece
            index += piece.length();
        }

        return result;
    }
}