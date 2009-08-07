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
package org.apache.felix.karaf.webconsole.features;

/**
 * Represents a feature with a name, version and state 
 */
public class Feature {

  public enum State {
    INSTALLED, UNINSTALLED, UNKNOWN;

    @Override
    public String toString() {
      //only capitalize the first letter
      String s = super.toString();
      return s.substring( 0, 1 ) + s.substring( 1 ).toLowerCase();
    }
  };

  protected String name;

  protected String version;

  protected State state;


  public Feature(String name, String version, State state) {
    this.name = name;
    this.version = version;
    this.state = state;
  }
}
