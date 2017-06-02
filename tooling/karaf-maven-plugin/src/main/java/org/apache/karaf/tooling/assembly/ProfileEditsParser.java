/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.tooling.assembly;

import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * Read and Parse Profile Edits.
 */
class ProfileEditsParser {

    private final KarafPropertyInstructionsModelStaxReader profileEditsReader;

    ProfileEditsParser(
            final KarafPropertyInstructionsModelStaxReader profileEditsReader
                      ) {
        this.profileEditsReader = profileEditsReader;
    }

    Optional<KarafPropertyEdits> parse(final AssemblyMojo mojo) throws FileNotFoundException {
        return Optional.ofNullable(mojo.getPropertyFileEdits())
                       .map(File::new)
                       .filter(File::exists)
                       .map(this::openFile)
                       .flatMap(this::readFile);
    }

    private FileInputStream openFile(final File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<KarafPropertyEdits> readFile(final FileInputStream is) {
        try {
            return Optional.of(profileEditsReader.read(is, true));
        } catch (IOException | XMLStreamException e) {
            return Optional.empty();
        }
    }

}
