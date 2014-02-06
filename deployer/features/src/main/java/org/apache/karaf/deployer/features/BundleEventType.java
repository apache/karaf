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
package org.apache.karaf.deployer.features;

import org.osgi.framework.BundleEvent;

/**
 * Convenience OSGI event representation.
 */
public enum BundleEventType {

	INSTALLED(BundleEvent.INSTALLED), //
	RESOLVED(BundleEvent.RESOLVED), //
	LAZY_ACTIVATION(BundleEvent.LAZY_ACTIVATION), //
	STARTING(BundleEvent.STARTING), //
	STARTED(BundleEvent.STARTED), //
	STOPPING(BundleEvent.STOPPING), //
	STOPPED(BundleEvent.STOPPED), //
	UPDATED(BundleEvent.UPDATED), //
	UNRESOLVED(BundleEvent.UNRESOLVED), //
	UNINSTALLED(BundleEvent.UNINSTALLED), //

	UNKNOWN(0), //

	;

	public final int code;

	BundleEventType(final int code) {
		this.code = code;
	}

	public static BundleEventType from(final BundleEvent event) {
		return from(event.getType());
	}

	public static BundleEventType from(final int code) {
		for (final BundleEventType known : BundleEventType.values()) {
			if (known.code == code) {
				return known;
			}
		}
		return UNKNOWN;
	}

}
