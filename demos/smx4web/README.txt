################################################################################
#
#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#
################################################################################

This is an example showing how to embed ServiceMix Kernel in a web application.
You can either:
  * build the web app using "mvn package" and deploy the web application to your favorite web container
  * run the web app from the command line using the "mvn package jetty:run" command
  
Once the web application is started, you can use the Apache ServiceMix Kernel client to connect to the running server:
  cd [smx-kernel-install-dir]
  java -jar lib/servicemix-client.jar
  
