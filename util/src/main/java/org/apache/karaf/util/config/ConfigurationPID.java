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
package org.apache.karaf.util.config;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ConfigurationPID {

    private final String pid;

    private final String factoryPid;

    private final String name;

    private static final String TILDE = "~";

    private static final String DASH = "-";

    public ConfigurationPID(@NotNull final String pid) {
        this.pid = pid;
        this.factoryPid = null;
        this.name = null;
    }

    public ConfigurationPID(@NotNull final String pid, @NotNull final String factoryPid, @Nullable final String name) {
        this.pid = pid;
        this.factoryPid = factoryPid;
        this.name = name;
    }

    @NotNull
    public String getPid() {
        return pid;
    }

    @Nullable
    public String getFactoryPid() {
        return factoryPid;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public boolean isFactory() {
        return Objects.nonNull(factoryPid);
    }

    public boolean isR7() {
        return pid.contains(TILDE);
    }

    /**
     * @param pid the OSGi Persistent Identity (PID)
     * @return the ConfigurationPID parsed from pid
     */
    @NotNull
    public static ConfigurationPID parsePid(@NotNull final String pid) {
        final int index = pid.contains(TILDE) ? pid.indexOf(TILDE) : pid.indexOf(DASH);
        if (index > 0) {
            final String factoryPid = pid.substring(0, index);
            final String name = pid.substring(index + 1);
            return new ConfigurationPID(pid, factoryPid, name);
        } else {
            return new ConfigurationPID(pid);
        }
    }

    /**
     * @param filename a filename of a Configuration with a single extension
     * @return the ConfigurationPID parsed from filename
     */
    @NotNull
    public static ConfigurationPID parseFilename(@NotNull final String filename) {
        final String pid = filename.substring(0, filename.lastIndexOf('.'));
        return parsePid(pid);
    }

    /**
     * @param filename  a filename of a Configuration
     * @param extension a filename extension without leading dot, e.g. <code>cfg</code>, <code>config</code>, <code>json</code>, <code>cfg.json</code> or empty string (no extension)
     * @return the ConfigurationPID parsed from filename
     * @throws IllegalArgumentException if filename does not end with given extension
     */
    @NotNull
    public static ConfigurationPID parseFilename(@NotNull final String filename, @NotNull final String extension) throws IllegalArgumentException {
        final String pid;
        if (extension.isEmpty()) {
            pid = filename;
        } else {
            final String ending = String.format(".%s", extension);
            if (filename.endsWith(ending)) {
                pid = filename.substring(0, filename.length() - ending.length());
            } else {
                final String message = String.format("Parsing filename failed. Filename '%s' does not have given extension '%s'.", filename, extension);
                throw new IllegalArgumentException(message);
            }
        }
        return parsePid(pid);
    }

}
