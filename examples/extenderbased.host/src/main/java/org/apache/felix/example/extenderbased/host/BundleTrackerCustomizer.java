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
package org.apache.felix.example.extenderbased.host;

import org.osgi.framework.Bundle;

/**
 * This interface class can be used to customize the bundle tracker
 * handling of bundle notifications when adding/removing bundles
 * to/from the active set of bundles.
**/
public interface BundleTrackerCustomizer
{
    /**
     * Called when an inactive bundle becomes active.
     * @param bundle The activated bundle.
    **/
    public void addedBundle(Bundle bundle);

    /**
     * Called when an active bundle becomes inactive.
     * @param bundle The inactivated bundle.
    **/
    public void removedBundle(Bundle bundle);
}