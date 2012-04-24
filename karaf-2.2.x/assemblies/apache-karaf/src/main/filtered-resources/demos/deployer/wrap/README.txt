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

Creating Bundles for Third Party Dependencies EXAMPLE
=====================================================

Purpose
-------
Demonstrate how to create a wrap bundle for a third
party dependency.


Prerequisites for Running this Example
--------------------------------------
You must have the following installed on your machine:

   - JDK 1.6 or higher.

   - Apache Maven 2.2.1 or higher.


Building and Deploying
----------------------
This example will produce a bundle. 

To build the wrap demo invoke the following command:

  mvn install

In the Maven POM file a shade plugin has been configured
to take an existing jar and package it as a jar bundle. In
this case the bundle produced exports the commons lang
packages.

The generated bundle deploys as per usual syntax:

  karaf@root> osgi:install -s mvn:osgi.commons-lang/osgi.commons-lang/2.4

Once installed you may confirm its operation by invoking
the following command:

  karaf@root> list | grep -i osgi.commons-lang 
  [  50] [Active     ] [            ] [   60] Apache Karaf :: Demos :: Deployer :: Wrap Bundle osgi.commons-lang (2.4.0)

For more information on creating bundles for third party 
dependencies please visit:
http://karaf.apache.org/manual/latest-2.2.x/developers-guide/creating-bundles.html
