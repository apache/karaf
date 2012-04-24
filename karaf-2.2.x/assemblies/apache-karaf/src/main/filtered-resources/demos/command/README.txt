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

EXTEND CONSOLE COMMAND EXAMPLE
==============================

Purpose
-------
Demonstrate how to extend a console command. 


Prerequisites for Running this Example
--------------------------------------
You must have the following installed on your machine:

   - JDK 1.5 or higher.

   - Apache Maven 2.2.1 or higher.


Building and Deploying
----------------------
This example will produce a bundle, containing a custom command
and its command completer.

To build the demo console command invoke the following command:

  mvn install

To deploy the console command invoke the following command on the Karaf
console: 

  karaf@root> osgi:install -s mvn:org.apache.karaf.demos/org.apache.karaf.demos.command/${pom.version}

Upon successful installation the bundle ID will be presented.

To test the custom command type the following on the Karaf console:

  karaf@root> mycommand:hello 
  Executing My Command Demo
  karaf@root>

To test the command completer press tab after typing the first few
characters of 'mycommand'.

For more information on Extending Karaf Console Commands please visit:
http://karaf.apache.org/manual/latest-2.2.x/developers-guide/extending-console.html
