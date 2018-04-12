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
package org.apache.karaf.client;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ClientConfigTest {

    @Test
    public void testDefaultUser() throws Exception {

        String etc = System.getProperty("karaf.etc");

        System.setProperty("karaf.etc", "src/test/resources/etc1");
        ClientConfig cc = new ClientConfig(new String[0]);
        assertThat(cc.getUser(), equalTo("karaf"));

        cc = new ClientConfig(new String[] { "-u", "different-one" });
        assertThat(cc.getUser(), equalTo("different-one"));

        System.setProperty("karaf.etc", "src/test/resources/etc2");
        cc = new ClientConfig(new String[0]);
        assertThat(cc.getUser(), equalTo("test"));

        if (etc != null) {
            System.setProperty("karaf.etc", etc);
        }
    }

}
