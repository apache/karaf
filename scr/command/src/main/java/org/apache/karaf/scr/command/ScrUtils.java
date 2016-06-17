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
package org.apache.karaf.scr.command;

import java.lang.reflect.Array;

import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

public class ScrUtils {
    
    @SuppressWarnings("unchecked")
    public static <T> T[] emptyIfNull(Class<T> clazz, T[] objects) {
        return objects == null ? (T[])Array.newInstance(clazz,0): objects;
    }

    public static String getState(int componentState) {
        String retVal = null;

        switch (componentState) {
        case ComponentConfigurationDTO.ACTIVE:
            retVal = "ACTIVE";
            break;
        case ComponentConfigurationDTO.SATISFIED:
            retVal = "SATISFIED";
            break;
        case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
        case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
            retVal = "UNSATISFIED";
            break;

        default:
            break;
        }

        return retVal;
    }

}
