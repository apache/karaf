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

DUMP PROVIDER EXAMPLE
======================

Purpose
-------
This demo provider creates images in dump destination which contains 
screenshots from all attached displays.


Prerequisites for Running this Example
--------------------------------------
You must have the following installed on your machine:

   - JDK 1.6 or higher.

   - Apache Maven 3.0.3 or higher.


Building and Deploying
----------------------

To build the dump provider demo type the following command:

  mvn install

This will create in the target folder dump-${version}.jar.

To install, copy dump-${version}.jar into Karaf's deploy folder.

To execute the demo issue the create-dump command as follows:

  karaf@root> create-dump
  Diagnostic dump created.
  karaf@root> 

This will create a time stamped zip folder which contains the
Karaf log, a list of bundles, threads, features, and a subfolder
containing screenshots of attached displays.

Unzipping a sample dump file produces:

  unzip 2012-02-06_185732.zip 
  Archive:  2012-02-06_185732.zip
  inflating: log/karaf.log           
  inflating: bundles.txt             
  inflating: screenshot/display_0.png  
  inflating: screenshot/display_1.png  
  inflating: threads.txt             
  inflating: features.txt

Note: The above system had two displays.
