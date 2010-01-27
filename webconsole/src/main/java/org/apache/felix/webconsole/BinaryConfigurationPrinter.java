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
package org.apache.felix.webconsole;


import java.io.IOException;
import java.io.OutputStream;


/**
 * This is an optional extension of the {@link ConfigurationPrinter}.
 * If a configuration printer implements this interface, the printer
 * can add additional files to the output of the configuration rendering.
 *
 * Currently this is only supported for the ZIP mode.
 *
 * A service implementing this method must still register itself
 * as a {@link ConfigurationPrinter} but not as a
 * {@link BinaryConfigurationPrinter} service.
 * @since 3.0
 */
public interface BinaryConfigurationPrinter
    extends ConfigurationPrinter
{

    /**
     * Return a list of filename for the render mode.
     * The returned list should just contain filenames without
     * any path information.
     * @return An array of filenames or null.
     */
    String[] getFilenames(String mode);

    /**
     * Write the contents of the file to the output stream.
     * @param name The filename.
     * @param out The output stream.
     * @throws IOException If any problem occurs during writing.
     * @throws IllegalArgumentException If the name is null or not
     *         one from the list returned by {@link #getFilenames(String)}.
     */
    void writeFile(String name, String mode,  OutputStream out)
    throws IOException;
}
