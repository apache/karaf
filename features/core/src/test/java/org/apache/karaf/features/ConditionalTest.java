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
package org.apache.karaf.features;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.junit.Test;


public class ConditionalTest {

    @Test
    public void testLoad() throws Exception {
        RepositoryImpl r = new RepositoryImpl(getClass().getResource("internal/service/f06.xml").toURI());
        Feature[] features = r.getFeatures();
        assertEquals(1, features.length);
        Feature feature = features[0];

        assertEquals(2,feature.getConditional().size());

        Conditional conditional1 = feature.getConditional().get(0);
        assertThat(conditional1.getCondition(), contains("http"));
        assertEquals(1, conditional1.getBundles().size());

        Conditional conditional2 = feature.getConditional().get(1);
        assertThat(conditional2.getCondition(), contains("req:osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(!(version>=1.7)))\""));
    }
    
}
