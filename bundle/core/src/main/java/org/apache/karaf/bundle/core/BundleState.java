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
package org.apache.karaf.bundle.core;

/**
 * Bundle status including framework status.
 *
 * <p>The combined status will be the worst status from all frameworks this bundle uses.
 *
 * <p>e.g. On OSGi level the BundleState is Active, on Blueprint level it is Waiting then the status
 * should be Waiting.
 */
public enum BundleState {
    Installed,
    Resolved,
    Unknown,
    GracePeriod,
    Waiting,
    Starting,
    Active,
    Stopping,
    Failure,
}
