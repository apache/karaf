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

public class DirectoryContent implements IContent
{
    private static final int BUFSIZE = 4096;

    private File m_dir = null;
    private boolean m_opened = false;

    public DirectoryContent(File dir)
    {
        m_dir = dir;
    }

    protected void finalize()
    {
    }

    public void open()
    {
        m_opened = true;
    }

    public synchronized void close()
    {
        m_opened = false;
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return new File(m_dir, name).exists();
    }

    public synchronized byte[] getEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        // Get the embedded resource.
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try
        {
            is = new BufferedInputStream(new FileInputStream(new File(m_dir, name)));
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
            throw new IllegalStateException("DirectoryContent is not open");
        }

        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return new FileInputStream(new File(m_dir, name));
    }

    public synchronized Enumeration getEntryPaths(String path)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

        if ((path.length() > 0) && (path.charAt(0) == '/'))
        {
            path = path.substring(1);
        }

        return new GetEntryPathsEnumeration(m_dir, path);
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new FindEntriesEnumeration(
            m_dir, path, filePattern, recurse);
        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    private static class GetEntryPathsEnumeration implements Enumeration
    {
        private File m_refDir = null;
        private File m_listDir = null;
        private File[] m_children = null;
        private int m_counter = 0;

        public GetEntryPathsEnumeration(File refDir, String path)
        {
            m_refDir = refDir;
            m_listDir = new File(refDir, path);
            if (m_listDir.isDirectory())
            {
                m_children = m_listDir.listFiles();
            }
        }

        public boolean hasMoreElements()
        {
            return (m_children != null) && (m_counter < m_children.length);
        }

        public Object nextElement()
        {
            if ((m_children == null) || (m_counter >= m_children.length))
            {
                throw new NoSuchElementException("No more entry paths.");
            }
            // Remove the leading path of the reference directory, since the
            // entry paths are supposed to be relative to the root.
            StringBuffer sb = new StringBuffer(m_children[m_counter].getAbsolutePath());
            sb.delete(0, m_refDir.getAbsolutePath().length() + 1);
            // Add a '/' to the end of directory entries.
            if (m_children[m_counter].isDirectory())
            {
                sb.append('/');
            }
            m_counter++;
            return sb.toString();
        }
    }

    private static class FindEntriesEnumeration implements Enumeration
    {
        private File m_refDir = null;
        private File m_listDir = null;
        private String[] m_filePattern = null;
        private boolean m_recurse = false;
        private File[] m_children = null;
        private int m_counter = 0;
        private Object m_next = null;

        public FindEntriesEnumeration(File refDir, String path, String filePattern, boolean recurse)
        {
            m_refDir = refDir;
            m_listDir = new File(refDir, path);
            m_filePattern = parseSubstring(filePattern);
            m_recurse = recurse;
            if (m_listDir.isDirectory())
            {
                if (m_recurse)
                {
                    m_children = listFilesRecursive(m_listDir);
                }
                else
                {
                    m_children = m_listDir.listFiles();
                }
            }
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
            if ((m_children == null) || (m_counter >= m_children.length))
            {
                return null;
            }

            // NOTE: We assume here that directories are not returned,
            // unlike getEntryPaths() above, where directories are returned;
            // this may or may not be the correct spec interpretation.

            // Ignore directories and file that do not match the file pattern.
            while ((m_counter < m_children.length) &&
                (m_children[m_counter].isDirectory() ||
                !checkSubstring(m_filePattern, m_children[m_counter].getName())))
            {
                m_counter++;
            }

            // Return null if there is no more matches.
            if (m_counter >= m_children.length)
            {
                return null;
            }

            // Remove the leading path of the reference directory, since the
            // entry paths are supposed to be relative to the root.
            StringBuffer sb = new StringBuffer(m_children[m_counter].getAbsolutePath());
            sb.delete(0, m_refDir.getAbsolutePath().length() + 1);
            m_counter++;

            return sb.toString();
        }

        public File[] listFilesRecursive(File dir)
        {
            File[] children = dir.listFiles();
            File[] combined = children;
            for (int i = 0; i < children.length; i++)
            {
                if (children[i].isDirectory())
                {
                    File[] grandchildren = listFilesRecursive(children[i]);
                    if (grandchildren.length > 0)
                    {
                        File[] tmp = new File[combined.length + grandchildren.length];
                        System.arraycopy(combined, 0, tmp, 0, combined.length);
                        System.arraycopy(grandchildren, 0, tmp, combined.length, grandchildren.length);
                        combined = tmp;
                    }
                }
            }
            return combined;
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