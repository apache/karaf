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
package org.apache.karaf.config.command;

import java.util.Dictionary;
import java.util.Enumeration;
import org.apache.karaf.shell.commands.Command;

@Command(scope = "config", name = "property-list", description = "Lists properties from the currently edited configuration.")
public class PropListCommand extends ConfigPropertyCommandSupport {

    @SuppressWarnings("rawtypes")
    @Override
    public void propertyAction(Dictionary props) {
        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            System.out.println("   " + key + " = " + props.get(key));
        }
    }

    /**
     * List commands never requires an update, so it always returns false.
     * @param pid
     * @return
     */
    @Override
    protected boolean requiresUpdate(String pid) {
        return false;
    }
}
