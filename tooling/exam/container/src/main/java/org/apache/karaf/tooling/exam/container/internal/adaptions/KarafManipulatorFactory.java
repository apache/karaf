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
package org.apache.karaf.tooling.exam.container.internal.adaptions;

import org.osgi.framework.Version;

/**
 * Factory returning the correct version of the manipulator depending on the added Karaf version.
 */
public final class KarafManipulatorFactory {

    private KarafManipulatorFactory() {
        // Not required for a final class
    }

    public static KarafManipulator createManipulator(String karafVersion) {
        int dots = 0;
        int i = 0;
        while ((i = karafVersion.indexOf('.', i)) != -1) {
            dots++;
            i++;
        }
        Version version;
        if (dots < 3) {           
            version = new Version(karafVersion.replaceFirst("-", "."));
        } else {
            version = new Version(karafVersion);
        }        
        if (version.getMajor() < 2 || version.getMajor() == 2 && version.getMinor() < 2) {
            throw new IllegalArgumentException("Karaf versions < 2.2.0 are not supported");
        }
        return new KarafManipulatorStartingFrom220();
    }

}
