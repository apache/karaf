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
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.felix.framework.installer.*;

public abstract class AbstractJarArtifact extends AbstractArtifact
{
    public AbstractJarArtifact(StringProperty sourceName)
    {
        this(sourceName, null);
    }

    public AbstractJarArtifact(StringProperty sourceName, StringProperty destDir)
    {
        this(sourceName, destDir, false);
    }

    public AbstractJarArtifact(
        StringProperty sourceName, StringProperty destDir, boolean localize)
    {
        super(sourceName, destDir, localize);
    }

    public boolean process(Status status, Map propMap)
    {
        try
        {
            InputStream is = getInputStream(status);

            if (is == null)
            {
                return true;
            }

            JarInputStream jis = new JarInputStream(is);
            status.setText("Extracting...");
            unjar(jis, propMap);
            jis.close();
        }
        catch (Exception ex)
        {
            System.err.println(this);
            System.err.println(ex);
            return false;
        }

        return true;
    }

    protected void unjar(JarInputStream jis, Map propMap)
        throws IOException
    {
        String installDir =
            ((StringProperty) propMap.get(Install.INSTALL_DIR)).getStringValue();

        // Loop through JAR entries.
        for (JarEntry je = jis.getNextJarEntry();
             je != null;
             je = jis.getNextJarEntry())
        {
            if (je.getName().startsWith("/"))
            {
                throw new IOException("JAR resource cannot contain absolute paths.");
            }

            File target =
                new File(installDir, getDestinationDirectory().getStringValue());
            target = new File(target, je.getName());

            // Check to see if the JAR entry is a directory.
            if (je.isDirectory())
            {
                if (!target.exists())
                {
                    if (!target.mkdirs())
                    {
                        throw new IOException("Unable to create target directory: "
                            + target);
                    }
                }
                // Just continue since directories do not have content to copy.
                continue;
            }

            int lastIndex = je.getName().lastIndexOf('/');
            String name = (lastIndex >= 0) ?
                je.getName().substring(lastIndex + 1) : je.getName();
            String destination = (lastIndex >= 0) ?
                je.getName().substring(0, lastIndex) : "";

            // JAR files use '/', so convert it to platform separator.
            destination = destination.replace('/', File.separatorChar);

            if (localize())
            {
                copyAndLocalize(jis, installDir, name, destination, propMap);
            }
            else
            {
                copy(jis, installDir, name, destination);
            }
        }
    }
}