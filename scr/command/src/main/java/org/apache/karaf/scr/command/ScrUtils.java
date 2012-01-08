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

import org.apache.felix.scr.Component;

public class ScrUtils {
    
    @SuppressWarnings("unchecked")
    public static <T> T[] emptyIfNull(Class<T> clazz, T[] objects) {
        return objects == null ? (T[])Array.newInstance(clazz,0): objects;
    }

    public static String getState(int componentState) {
        String retVal = null;

        switch (componentState) {
        case Component.STATE_ACTIVE:
            retVal = "ACTIVE";
            break;
        case Component.STATE_ACTIVATING:
            retVal = "ACTIVATING";
            break;
        case Component.STATE_DEACTIVATING:
            retVal = "DEACTIVATING";
            break;
        case Component.STATE_DISABLED:
            retVal = "DISABLED";
            break;
        case Component.STATE_DISABLING:
            retVal = "DISABLING";
            break;
        case Component.STATE_DISPOSED:
            retVal = "DISPOSED";
            break;
        case Component.STATE_DISPOSING:
            retVal = "DISPOSING";
            break;
        case Component.STATE_ENABLING:
            retVal = "ENABLING";
            break;
        case Component.STATE_FACTORY:
            retVal = "FACTORY";
            break;
        case Component.STATE_REGISTERED:
            retVal = "REGISTERED";
            break;
        case Component.STATE_UNSATISFIED:
            retVal = "UNSATISFIED";
            break;

        default:
            break;
        }

        return retVal;
    }
}
