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
package org.apache.karaf.shell.ssh.keygenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.ssl.PEMItem;
import org.apache.commons.ssl.PEMUtil;

public class PemWriter {
    private File keyFile;

    public PemWriter(File keyFile) {
        this.keyFile = keyFile;
    }
    
    public void writeKeyPair(String resource, KeyPair kp) throws IOException, FileNotFoundException {
        Collection<Object> items = new ArrayList<>();
        items.add(new PEMItem(kp.getPrivate().getEncoded(), "PRIVATE KEY"));
        byte[] bytes = PEMUtil.encode(items);
        try (FileOutputStream os = new FileOutputStream(keyFile)) {
            os.write(bytes);
        }
    }
}
