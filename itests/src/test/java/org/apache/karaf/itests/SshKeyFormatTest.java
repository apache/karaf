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


package org.apache.karaf.itests;

import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;


/**
 * Test use of PEM keys.
 */
public class SshKeyFormatTest extends SshCommandTestBase {

    @Configuration
    public Option[] config() {
        File keyFile = new File("src/test/resources/test.pem");
        return options(composite(super.config()),
                editConfigurationFilePut("org.apache.karaf.shell.cfg", "hostKey", keyFile.getAbsolutePath()),
                editConfigurationFilePut("org.apache.karaf.shell.cfg", "hostKeyFormat", "PEM")
                );
    }


    @Test
    public void usePemKey() throws Exception {
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        ConnectFuture future = client.connect("karaf", "localhost", 8101).await();
        ClientSession session = future.getSession();
        Map<Object, Object> metadata = session.getMetadataMap();
        session.close(true);
    }
}
