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

import org.apache.karaf.util.bundles.BundleUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

public class BundleUtilsTest {

    @Test
    public void testJavaxMail() throws Exception {
        String url = getClass().getClassLoader().getResource("com/sun/mail/util/ASCIIUtility.class").toString();
        url = url.substring("jar:file:".length(), url.indexOf("!/"));
        File file = new File(url);
        BundleUtils.fixBundleWithUpdateLocation(new FileInputStream(file), "mvn:javax.mail/mail/1.4.7");
    }
}
