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
package org.apache.felix.fileinstall.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * This class is used to cache vital information of a jar file
 * that is used during later processing. It also overrides hashCode and
 * equals methods so that it can be used in various Set operations.
 * It uses file's path as the primary key. 
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class Jar
{
    private String path;
    private long lastModified = -1;
    private long bundleId = -1;

    Jar(File file)
    {
        // Convert to a URI because the location of a bundle
        // is typically a URI. At least, that's the case for
        // autostart bundles.
        // Normalisation is needed to ensure that we don't treat (e.g.)
        // /tmp/foo and /tmp//foo differently.
        path = file.toURI().normalize().getPath();
        lastModified = file.lastModified();
    }

    Jar(Bundle b) throws URISyntaxException
    {
        // Convert to a URI because the location of a bundle
        // is typically a URI. At least, that's the case for
        // autostart bundles and bundles installed by fileinstall.
        // Normalisation is needed to ensure that we don't treat (e.g.)
        // /tmp/foo and /tmp//foo differently.
        String location = b.getLocation();
        if (location != null &&
                !location.equals(Constants.SYSTEM_BUNDLE_LOCATION))
        {
            URI uri;
            try
            {
                uri = new URI(b.getLocation()).normalize();
            }
            catch (URISyntaxException e)
            {
                // Let's try to interpret the location as a file path
                uri = new File(location).toURI().normalize();
            }
            path = uri.getPath();
        }
        lastModified = b.getLastModified();
        bundleId = b.getBundleId();
    }

    public String getPath()
    {
        return path;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public boolean isNewer(Jar other)
    {
        return (getLastModified() > other.getLastModified());
    }

    // Override hashCode and equals as this object is used in Set
    public int hashCode()
    {
        return path.hashCode();
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof Jar)
        {
            return this.path.equals(((Jar) obj).path);
        }
        return false;
    }
}
