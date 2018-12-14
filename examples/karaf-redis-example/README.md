  <!--
      Licensed to the Apache Software Foundation (ASF) under one
      or more contributor license agreements.  See the NOTICE file
      distributed with this work for additional information
      regarding copyright ownership.  The ASF licenses this file
      to you under the Apache License, Version 2.0 (the
      "License"); you may not use this file except in compliance
      with the License.  You may obtain a copy of the License at
  
        http://www.apache.org/licenses/LICENSE-2.0
  
      Unless required by applicable law or agreed to in writing,
      software distributed under the License is distributed on an
      "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
      KIND, either express or implied.  See the License for the
      specific language governing permissions and limitations
      under the License.
  -->
  # Apache Karaf Redis example
  
  ## Abstract
  
  This example shows you how to use Redis in karaf framework.
  
  ## Artifacts
  
  * **karaf-redis-example-api** is a main container object and service interface.
  * **karaf-redis-example-command** is a main container console command for using service.
  * **karaf-redis-example-provider** is a implementation of service interface and connect to redis. 
  
  ## Build
  
  The build uses Apache Maven. Simply use:
  
  ```
  mvn clean install
  ```
  
  ## Features and Deployment
  
  On a running Karaf instance, register the features repository:
  
  
  # Install Application
  
  Add repository:
  
  ``
  karaf@root()>feature:repo-add mvn:org.apache.karaf.examples/karaf-redis-example-feature/4.2.2-SNAPSHOT/xml
  ``
  
  Then you can install the service provider feature:
  
  ``
  karaf@root()>feature:install karaf-redis-example
  ``
  
  ## Usage
  
  for using this application Redis service must be installed on OS.
  
  * save data in the RAM:
  ```
  karaf@root()> user:add alireza khatamiDoost +989194018087
  ```
  
  * save data in the Redis:
  ```
  karaf@root()> user:add-redis alireza khatamiDoost +989194018087
  ```
  
  * show data that saved in RAM:
  
  ```
  karaf@root()> user:list
  ```
  
  * show data that saved in Redis:
    
  ```
  karaf@root()> user:list-redis
  ```
  
  * remove data from RAM:
  
  ```
  karaf@root()> user:remove <Press-Tab-to-show-IDs>
  ```
  * remove data from RAM:
    
  ```
  karaf@root()> user:remove-redis <Press-Tab-to-show-IDs>
  ```
  
  * remove all data from RAM:
  
  ```
  karaf@root()> user:remove-all
  ```
  
  * remove all data from Redis:
    
  ```
  karaf@root()> user:remove-all-redis
  ```
  