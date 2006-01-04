/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.felix.framework.installer.artifact;

import java.io.*;
import java.util.Map;

import org.apache.felix.framework.installer.*;
import org.apache.felix.framework.installer.property.StringPropertyImpl;

public abstract class AbstractArtifact implements Artifact
{
    private StringProperty m_sourceName = null;
    private StringProperty m_destDir = null;
    private boolean m_localize = false;

    // This following shared buffer assumes that there is
    // no concurrency when processing resources.
    protected static byte[] s_buffer = new byte[2048];

    public AbstractArtifact(
        StringProperty sourceName, StringProperty destDir, boolean localize)
    {
        if (destDir == null)
        {
            destDir = new StringPropertyImpl("empty", "");
        }
        m_sourceName = sourceName;
        m_destDir = destDir;
        m_localize = localize;
    }

    public StringProperty getSourceName()
    {
        return m_sourceName;
    }

    public StringProperty getDestinationDirectory()
    {
        return m_destDir;
    }

    public boolean localize()
    {
        return m_localize;
    }

    protected static void copy(
        InputStream is, String installDir, String destName, String destDir)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(installDir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int count = 0;
        while ((count = is.read(s_buffer)) > 0)
        {
            bos.write(s_buffer, 0, count);
        }
        bos.close();
    }

    protected static void copyAndLocalize(
        InputStream is, String installDir, String destName,
        String destDir, Map propMap)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(installDir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int i = 0;
        while ((i = is.read()) > 0)
        {
            // Parameters start with "%%", so check to see if
            // we have a parameter.
            if ((char)i == '%')
            {
                // One of three possibilities, we have a parameter,
                // we have an end of file, or we don't have a parameter.
                int i2 = is.read();
                if ((char) i2 == '%')
                {
                    Object obj = readParameter(is);

                    // If the byte sequence was not a parameter afterall,
                    // then a byte array is returned, otherwise a string
                    // containing the parameter m_name is returned.
                    if (obj instanceof byte[])
                    {
                        bos.write(i);
                        bos.write(i2);
                        bos.write((byte[]) obj);
                    }
                    else
                    {
                        Property prop = (Property) propMap.get(obj);
                        String value = (prop == null) ? "" : prop.toString();
                        bos.write(value.getBytes());
                    }
                }
                else if (i2 == -1)
                {
                    bos.write(i);
                }
                else
                {
                    bos.write(i);
                    bos.write(i2);
                }
            }
            else
            {
                bos.write(i);
            }
        }
        bos.close();
    }

    protected static Object readParameter(InputStream is)
        throws IOException
    {
        int count = 0;
        int i = 0;
        while ((count < s_buffer.length) && ((i = is.read()) > 0))
        {
            if ((char) i == '%')
            {
                // One of three possibilities, we have the end of
                // the parameter, we have an end of file, or we
                // don't have the parameter end.
                int i2 = is.read();
                if ((char) i2 == '%')
                {
                    return new String(s_buffer, 0, count);
                }
                else if (i2 == -1)
                {
                    s_buffer[count] = (byte) i;
                    byte[] b = new byte[count];
                    for (int j = 0; j < count; j++)
                        b[j] = s_buffer[j];
                    return b;
                }
                else
                {
                    s_buffer[count++] = (byte) i;
                    s_buffer[count++] = (byte) i2;
                }
            }
            else
            {
                s_buffer[count++] = (byte) i;
            }
        }

        byte[] b = new byte[count - 1];
        for (int j = 0; j < (count - 1); j++)
            b[j] = s_buffer[j];

        return b;
    }

    public static String getPath(String s, char separator)
    {
        return (s.lastIndexOf(separator) < 0)
            ? "" : s.substring(0, s.lastIndexOf(separator));
    }

    public static String getPathHead(String s, char separator)
    {
        return (s.lastIndexOf(separator) < 0)
            ? s : s.substring(s.lastIndexOf(separator) + 1);
    }
}