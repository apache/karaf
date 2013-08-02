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
package org.apache.karaf.shell.scr.support;

import org.apache.felix.scr.Component;

import java.util.Comparator;

public class IdComparator implements Comparator<Component> {
    public int compare(Component left, Component right) {
        if (left.getId() < right.getId()) {
            return -1;
        } else if (left.getId() == right.getId()) {
            return 0;
        } else {
            return 1;
        }
    }
}
