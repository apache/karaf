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
package org.apache.felix.scr.annotations;

import java.lang.annotation.*;

/**
 * The <code>Dectivate</code> annotation defines the method which is used
 * to deactivate the component.
 * Please note that this annotation only marks the method name which is
 * put into the configuration. The DS implementation will still use its
 * search strategy to find the method based on the name.
 */
@Target( { ElementType.METHOD })
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Deactivate {

}
