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
package org.apache.felix.scr.impl.metadata.instances;


import java.util.Map;

import org.apache.felix.scr.impl.metadata.instances2.Level2Object;
import org.osgi.service.component.ComponentContext;


public class Level3Object extends Level2Object
{

    private void activate_comp_map( ComponentContext ctx, Map map )
    {
        setCalledMethod("activate_comp_map");
    }


    // this method should not be found, since the method taking a
    // Map has higher precedence
    public void activate_collision()
    {
        setCalledMethod("not_expected_to_be_found");
    }


    public void activate_collision( Map map )
    {
        setCalledMethod("activate_collision");
    }
}
