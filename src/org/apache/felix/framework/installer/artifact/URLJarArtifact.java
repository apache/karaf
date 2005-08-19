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
import java.net.URL;
import java.net.URLConnection;

import org.apache.felix.framework.installer.Status;
import org.apache.felix.framework.installer.StringProperty;

public class URLJarArtifact extends AbstractJarArtifact
{
    public URLJarArtifact(StringProperty sourceName)
    {
        this(sourceName, null);
    }

    public URLJarArtifact(StringProperty sourceName, StringProperty destDir)
    {
        this(sourceName, destDir, false);
    }

    public URLJarArtifact(
        StringProperty sourceName, StringProperty destDir, boolean localize)
    {
        super(sourceName, destDir, localize);
    }

    public InputStream getInputStream(Status status)
        throws IOException
    {
        String fileName = getSourceName().getStringValue();
        fileName = (fileName.lastIndexOf('/') > 0)
            ? fileName.substring(fileName.lastIndexOf('/') + 1)
            : fileName;
        
        status.setText("Connecting...");

        File file = File.createTempFile("felix-install.tmp", null);
        file.deleteOnExit();

        OutputStream os = new FileOutputStream(file);
        URLConnection conn = new URL(getSourceName().getStringValue()).openConnection();
        int total = conn.getContentLength();
        InputStream is = conn.getInputStream();

        int count = 0;
        for (int len = is.read(s_buffer); len > 0; len = is.read(s_buffer))
        {
            count += len;
            os.write(s_buffer, 0, len);
            if (total > 0)
            {
                status.setText("Downloading " + fileName
                    + " ( " + count + " bytes of " + total + " ).");
            }
            else
            {
                status.setText("Downloading " + fileName + " ( " + count + " bytes ).");
            }
        }

        os.close();
        is.close();

        return new FileInputStream(file);
    }

    public String toString()
    {
        return "URL JAR: " + getSourceName().getStringValue();
    }
}