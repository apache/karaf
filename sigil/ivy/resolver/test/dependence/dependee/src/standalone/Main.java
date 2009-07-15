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

package standalone;

import java.util.Properties;

import org.apache.commons.lang.WordUtils;

/**
 * TODO write javadoc
 */
public class Main {
    /**
     * Returns the version of the project
     * @return a string representation of the version, null if the version could not be retreived
     */
    public static String getVersion() {
        Properties p = new Properties();
        try {
            p.load(Main.class.getResourceAsStream("/version.properties"));
            String version = p.getProperty("version");
            if (version != null) {
                return String.valueOf(Integer.parseInt(version));
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Return the same string with all words capitalized.
     * @param str the string conatining the words to capitalize
     * @return null if the string was null, the string with all words capitalized otherwise
     */
    public static String capitalizeWords(String str) {
        System.out.println("    [" + Main.class.getName() + "] capitalizing string \"" + str + "\" using " + WordUtils.class.getName());
        return WordUtils.capitalizeFully(str);
    }
    public static void main(String[] args) {
        String message="sentence to capitalize";
        System.out.println("standard message : " + message);
        System.out.println("capitalized message : " + capitalizeWords(message));
    }
}
