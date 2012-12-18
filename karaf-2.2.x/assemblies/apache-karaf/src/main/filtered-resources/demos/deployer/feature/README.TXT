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

Karaf Feature EXAMPLE
=====================

Purpose
-------
Demonstrate how to build a Feature.

A feature is a useful tool for OSGi developers to 
specify multiple bundles (and possibly other features)
into a larger deployment group. In this way a feature
can be thought of as configuring together components
into an application. 

Please note that a feature file only specifies resources
to be deployed together, for a deployable artifact that
contains all resources please see Apache Karaf Archives. 

Prerequisites for Running this Example
--------------------------------------
You must have the following installed on your machine:

   - JDK 1.6 or higher.

   - Apache Maven 2.2.1 or higher.


Building and Deploying
----------------------
This example will produce a feature xml file. This feature
picks up one bundle, commons-collections, for deployment. 

To build the demo feature invoke the following command:

  mvn install

To deploy the feature you may copy the feature xml file
generated in target/classes folder to the Karaf deploy
folder OR, you may invoke the following commands from 
the Karaf console:

  karaf@root> features:addurl file:/Path/To/Karaf/demos/deployer/feature/target/classess/feature.xml
  karaf@root> features:list | grep commons-collections
  [uninstalled] [0.0.0          ] commons-collections           repo-0   
  karaf@root> features:install commons-collection

To verify the feature has been succesfully installed issue
the following command:

  karaf@root> features:list | grep commons-collections
  [installed  ] [0.0.0          ] commons-collections           repo-0 

To verify the commons-cllections bundle is active invoke:

  karaf@root> list
  START LEVEL 100 , List Threshold: 50
     ID   State         Blueprint      Level  Name
  [  50] [Active     ] [            ] [   60] Commons Collections (3.2.1)


Note: Using Karaf archetype to generate a feature project:
---------------------------------------------------------

The karaf-feature-archetype creates a Maven skeleton project
that will generate a feature xml file.

On the command line issue the following command to create
a feature project:

  mvn archetype:generate \
      -DarchetypeGroupId=org.apache.karaf.archetypes \ 
      -DarchetypeArtifactId=karaf-feature-archetype \ 
      -DarchetypeVersion=${version} \ 
      -DgroupId=org.myorg \
      -DartifactId=my-feature \ 
      -Dversion=1.0.0-SNAPSHOT \ 
      -Dpackage=org.myorg.package

For more information on using the karaf-bundle-archetype
please visit: http://karaf.apache.org/manual/latest-2.2.x/developers-guide/archetypes.html
