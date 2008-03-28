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
package org.apache.felix.ipojo.test.log;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class DefaultLogImpl implements LogService {
    

    public void log(int arg0, String arg1) {
        dispatch(arg0, arg1);
    }

    public void log(int arg0, String arg1, Throwable arg2) {
        dispatch(arg0, arg1 + " (" + arg2.getMessage() + ")");
    }

    public void log(ServiceReference arg0, int arg1, String arg2) {
        dispatch(arg1, arg2 + " (" + arg0.toString() + ")");
    }

    public void log(ServiceReference arg0, int arg1, String arg2, Throwable arg3) {
        dispatch(arg1, arg2 + " (" + arg0.toString() + ")" + " (" + arg3.getMessage() + ")");
    }
    
    
    private void dispatch(int level, String message) {
        switch (level) {
            case LogService.LOG_DEBUG:
                System.out.println("[DEBUG] " + message);
                break;
            case LogService.LOG_INFO:
                System.out.println("[INFO] " + message);
                break;
            case LogService.LOG_WARNING:
                System.out.println("[WARNING] " + message);
                break; 
            case LogService.LOG_ERROR:
                System.out.println("[ERROR] " + message);
                break;
            default:
                System.out.println("[" + level + "] " + message);
                break;
        }
    }


}
