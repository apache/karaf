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
package org.apache.karaf.features.internal.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.io.File;

public class FeatureConfigInstallerTest {
    
    private void substEqual(final String src, final String subst) {
        assertEquals(FeatureConfigInstaller.substFinalName(src), subst);
    }

    @Test
    public void testSubstFinalName() {
        final String karafBase = "/tmp/karaf.base";
        final String karafEtc = karafBase + "/etc";
        final String foo = "/foo";
        
        System.setProperty("karaf.base", karafBase);
        System.setProperty("karaf.etc", karafEtc);
        System.setProperty("foo", foo);
        
        substEqual("etc/test.cfg", karafBase + File.separator + "etc/test.cfg");
        substEqual("/etc/test.cfg", karafBase + File.separator + "/etc/test.cfg");
        substEqual("${karaf.etc}/test.cfg", karafEtc + "/test.cfg");
        substEqual("${karaf.base}/etc/test.cfg", karafBase + "/etc/test.cfg");
        substEqual("etc/${foo}/test.cfg", karafBase + File.separator + "etc/" + foo + "/test.cfg");
        substEqual("${foo}/test.cfg", foo + "/test.cfg");
        substEqual("etc${bar}/${bar}test.cfg", karafBase + File.separator + "etc/test.cfg");
        substEqual("${bar}/etc/test.cfg${bar}", karafBase + File.separator + "/etc/test.cfg");
        substEqual("${karaf.base}${bar}/etc/test.cfg", karafBase + "/etc/test.cfg");
        substEqual("etc${}/${foo}/test.cfg", karafBase + File.separator + "etc//test.cfg");
        substEqual("${foo}${bar}/${bar}${foo}", foo + "/" + foo);
    }

}
