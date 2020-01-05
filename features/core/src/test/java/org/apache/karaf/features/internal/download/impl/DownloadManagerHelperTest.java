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
package org.apache.karaf.features.internal.download.impl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import static junit.framework.TestCase.assertEquals;

public class DownloadManagerHelperTest {

  @Test
  public void testSetExtraProtocols(){
    assertEquals("^(jar|war|war-i|warref|webbundle|wrap|spring|blueprint):.*$", DownloadManagerHelper.getIgnoredProtocolPattern().toString());

    List<String> extraProtocols = new ArrayList<>();
    extraProtocols.add( "extra1" );
    extraProtocols.add( "extra2" );
    DownloadManagerHelper.setExtraProtocols( extraProtocols );

    assertEquals("^(jar|war|war-i|warref|webbundle|wrap|spring|blueprint|extra1|extra2):.*$", DownloadManagerHelper.getIgnoredProtocolPattern().toString());
  }
}
