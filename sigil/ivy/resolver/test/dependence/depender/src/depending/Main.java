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

package depending;

/**
 * TODO write javadoc
 */
public class Main {
    public static void main(String[] args) {
        String standaloneVersion = standalone.Main.getVersion();
        if (standaloneVersion!=null) {
            System.out.println("you are using version " + standaloneVersion + " of class " + standalone.Main.class.getName());
        } else {
            System.err.println("failed to get version of " + standalone.Main.class.getName());
        }
        String message = "i am " + Main.class.getName() + " and " + standalone.Main.class.getName() + " will do the job for me";
        System.out.println("standard message : " + message);
        System.out.println("capitalized message : " + standalone.Main.capitalizeWords(message));
    }
}
