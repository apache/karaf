/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.util;

import java.io.File;
import java.io.IOException;

public final class PathUtils {

    private PathUtils() {
    }

    /**
     * Check whether {@code child} resolves to a location inside {@code parent}, following symbolic
     * links and normalizing {@code ..} segments through canonical paths.
     */
    public static boolean isWithin(File parent, File child) throws IOException {
        return child.getCanonicalFile().toPath().startsWith(parent.getCanonicalFile().toPath());
    }

    /**
     * Ensure that {@code child} resolves to a location inside {@code parent}, throwing an
     * {@link IOException} otherwise.
     *
     * <p>Use this to stop a caller-controlled file name or path from escaping the directory it is
     * meant to stay within (path traversal, CWE-22) -- a plain {@code contains("..")} check does
     * not catch absolute paths or symbolic links.</p>
     */
    public static void checkWithin(File parent, File child) throws IOException {
        if (!isWithin(parent, child)) {
            throw new IOException("For security reasons, " + child + " has to be located inside "
                    + parent.getCanonicalFile());
        }
    }

}
