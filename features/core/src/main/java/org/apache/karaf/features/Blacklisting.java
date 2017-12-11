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
package org.apache.karaf.features;

/**
 * <p>Indication that given object may be <em>blacklisted</em>.</p>
 * <p>Blacklisted item is available for query, but can't be used to alter system (e.g., blacklisted feature
 * can't be installed)</p>
 */
public interface Blacklisting {

    /**
     * Returns <code>true</code> if this item is <em>blacklisted</em>.
     * @return
     */
    boolean isBlacklisted();

}
