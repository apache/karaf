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
package org.apache.osgi.framework.installer.artifact;

import java.io.InputStream;
import java.util.Map;

import org.apache.osgi.framework.installer.*;

public abstract class AbstractFileArtifact extends AbstractArtifact
{
    private StringProperty m_destName = null;

    public AbstractFileArtifact(StringProperty sourceName)
    {
        this(sourceName, sourceName);
    }

    public AbstractFileArtifact(StringProperty sourceName, StringProperty destName)
    {
        this(sourceName, destName, null);
    }

    public AbstractFileArtifact(
        StringProperty sourceName, StringProperty destName, StringProperty destDir)
    {
        this(sourceName, destName, destDir, false);
    }

    public AbstractFileArtifact(
        StringProperty sourceName, StringProperty destName,
        StringProperty destDir, boolean localize)
    {
        super(sourceName, destDir, localize);
        m_destName = destName;
    }

    public StringProperty getDestinationName()
    {
        return m_destName;
    }

    public boolean process(Status status, Map propMap)
    {
        String installDir =
            ((StringProperty) propMap.get(Install.INSTALL_DIR)).getStringValue();

        try
        {
            InputStream is = getInputStream(status);

            if (is == null)
            {
                return true;
            }

            if (localize())
            {
                status.setText("Copying and configuring "
                    + getSourceName().getStringValue());
                copyAndLocalize(
                    is,
                    installDir,
                    getDestinationName().getStringValue(),
                    getDestinationDirectory().getStringValue(),
                    propMap);
            }
            else
            {
                status.setText("Copying " + getSourceName().getStringValue());
                copy(
                    is,
                    installDir,
                    getDestinationName().getStringValue(),
                    getDestinationDirectory().getStringValue());
            }

            is.close();

        }
        catch (Exception ex)
        {
            System.err.println(ex);
            return false;
        }

        return true;
    }
}