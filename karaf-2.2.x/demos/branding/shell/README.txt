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

BRANDING SHELL EXAMPLE
======================

Purpose
-------
Demonstrate how to change the shell console branding.


Prerequisites for Running this Example
--------------------------------------
You must have the following installed on your machine:

   - JDK 1.5 or higher.

   - Apache Maven 2.0.9 or higher.


Building and Deploying
----------------------
This example will produce a branding jar, containing a branding properties
file which is used to generate the shell console branding.

To build the demo branding type the following command:

  mvn install

To deploy the branding copy the resulting jar file to karaf/lib folder.

  cp target/org.apache.karaf.demos.branding.shell-${version}.jar $KARAF_HOME/lib

In order for Karaf to pick up the branding jar please edit the
$KARAF_HOME/etc/custom.properties file to include the following:

  org.osgi.framework.system.packages.extra = \
    org.apache.karaf.branding; \
    com.sun.org.apache.xalan.internal.xsltc.trax; \
    com.sun.org.apache.xerces.internal.dom; \
    com.sun.org.apache.xerces.internal.jaxp; \
    com.sun.org.apache.xerces.internal.xni

To see the new branding please restart Karaf:

  cd $KARAF_HOME/bin
  ./karaf

The shell console should now display the content of the branding
properties file.

NOTES
=====
Most projects automate this process. One such project is Apache Servicemix
NMR, see its branding and assembly poms for a guide line.
