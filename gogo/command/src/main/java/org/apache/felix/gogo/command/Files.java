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
package org.apache.felix.gogo.command;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;

public class Files
{
    private static final String CWD = "_cwd";

    private final BundleContext m_bc;

    public Files(BundleContext bc)
    {
        m_bc = bc;
    }

    @Descriptor("get current directory")
    public File cd(
        @Descriptor("automatically supplied shell session") CommandSession session)
    {
        try
        {
            return cd(session, null);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Unable to get current directory");
        }
    }

    @Descriptor("change current directory")
    public File cd(
        @Descriptor("automatically supplied shell session") CommandSession session,
        @Descriptor("target directory") String dir)
        throws IOException
    {
        File cwd = (File) session.get(CWD);
        if (cwd == null)
        {
            cwd = new File(".").getCanonicalFile();
            session.put(CWD, cwd);
        }
        if ((dir == null) || (dir.length() == 0))
        {
            return cwd;
        }
        cwd = new File(cwd, dir);
        if (!cwd.exists())
        {
            throw new IOException("Directory does not exist");
        }
        else if (!cwd.isDirectory())
        {
            throw new IOException("Target is not a directory");
        }
        session.put(CWD, cwd.getCanonicalFile());
        return cwd;
    }

    @Descriptor("get current directory contents")
    public File[] ls(
        @Descriptor("automatically supplied shell session") CommandSession session)
        throws IOException
    {
        return ls(session, null);
    }

    @Descriptor("get specified path contents")
    public File[] ls(
        @Descriptor("automatically supplied shell session") CommandSession session,
        @Descriptor("path with optionally wildcarded file name") String pattern)
        throws IOException
    {
        pattern = ((pattern == null) || (pattern.length() == 0)) ? "." : pattern;
        pattern = ((pattern.charAt(0) != File.separatorChar) && (pattern.charAt(0) != '.'))
            ? "./" + pattern : pattern;
        int idx = pattern.lastIndexOf(File.separatorChar);
        String parent = (idx < 0) ? "." : pattern.substring(0, idx + 1);
        String target = (idx < 0) ? pattern : pattern.substring(idx + 1);

        File actualParent = ((parent.charAt(0) == File.separatorChar)
            ? new File(parent) : new File(cd(session), parent)).getCanonicalFile();

        idx = target.indexOf(File.separatorChar, idx);
        boolean isWildcarded = (target.indexOf('*', idx) >= 0);
        File[] files;
        if (isWildcarded)
        {
            if (!actualParent.exists())
            {
                throw new IOException("File does not exist");
            }
            final List<String> pieces = parseSubstring(target);
            files = actualParent.listFiles(new FileFilter() {
                public boolean accept(File pathname)
                {
                    return compareSubstring(pieces, pathname.getName());
                }
            });
        }
        else
        {
            File actualTarget = new File(actualParent, target).getCanonicalFile();
            if (!actualTarget.exists())
            {
                throw new IOException("File does not exist");
            }
            if (actualTarget.isDirectory())
            {
                files = actualTarget.listFiles();
            }
            else
            {
                files = new File[] { actualTarget };
            }
        }
        return files;
    }

    public static List<String> parseSubstring(String value)
    {
        List<String> pieces = new ArrayList();
        StringBuffer ss = new StringBuffer();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
        boolean escaped = false;
loop:   for (;;)
        {
            if (idx >= value.length())
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

            // Read the next character and account for escapes.
            char c = value.charAt(idx++);
            if (!escaped && ((c == '(') || (c == ')')))
            {
                throw new IllegalArgumentException(
                    "Illegal value: " + value);
            }
            else if (!escaped && (c == '*'))
            {
                if (wasStar)
                {
                    // encountered two successive stars;
                    // I assume this is illegal
                    throw new IllegalArgumentException("Invalid filter string: " + value);
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
                wasStar = true;
            }
            else if (!escaped && (c == '\\'))
            {
                escaped = true;
            }
            else
            {
                escaped = false;
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
        return pieces;
    }

    public static boolean compareSubstring(List<String> pieces, String s)
    {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = true;
        int len = pieces.size();

        int index = 0;

loop:   for (int i = 0; i < len; i++)
        {
            String piece = pieces.get(i);

            // If this is the first piece, then make sure the
            // string starts with it.
            if (i == 0)
            {
                if (!s.startsWith(piece))
                {
                    result = false;
                    break loop;
                }
            }

            // If this is the last piece, then make sure the
            // string ends with it.
            if (i == len - 1)
            {
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

            // If this is neither the first or last piece, then
            // make sure the string contains it.
            if ((i > 0) && (i < (len - 1)))
            {
                index = s.indexOf(piece, index);
                if (index < 0)
                {
                    result = false;
                    break loop;
                }
            }

            // Move string index beyond the matching piece.
            index += piece.length();
        }

        return result;
    }
}