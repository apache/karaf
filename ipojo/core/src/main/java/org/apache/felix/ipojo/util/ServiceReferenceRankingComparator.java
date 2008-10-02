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
package org.apache.felix.ipojo.util;

import java.util.Comparator;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Service Reference Comparator.
 * This comparator follows OSGi Ranking policy.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceReferenceRankingComparator implements Comparator {

    /**
     * Compares two service reference.
     * @param ref1 the reference 1
     * @param ref2 the reference 2
     * @return <code>-1</code> if the reference 1 
     * is 'higher' than the reference 2, <code>1</code> otherwise. 
     * (higher is term of ranking means a lower index)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object ref1, Object ref2) {
        if (ref1.equals(ref2)) { return 0; }

        if (ref1 instanceof ServiceReference && ref2 instanceof ServiceReference) {
            Object property1 = ((ServiceReference) ref1).getProperty(Constants.SERVICE_RANKING);
            Object property2 = ((ServiceReference) ref2).getProperty(Constants.SERVICE_RANKING);

            int rank1 = 0;
            int rank2 = 0;
            if (property1 instanceof Integer) {
                rank1 = ((Integer) property1).intValue();
            }
            if (property2 instanceof Integer) {
                rank2 = ((Integer) property2).intValue();
            }

            if (rank1 == rank2) {
                // Check service.id
                Object sid1 = ((ServiceReference) ref1).getProperty(Constants.SERVICE_ID);
                Object sid2 = ((ServiceReference) ref2).getProperty(Constants.SERVICE_ID);

                long rankId1 = ((Long) sid1).longValue();
                long rankId2 = ((Long) sid2).longValue();

                if (rankId1 == rankId2) {
                    return 0;
                } else if (rankId1 < rankId2) {
                    return -1;
                } else {
                    return 1;
                }

            } else if (rank1 > rank2) {
                return -1;
            } else {
                return 1;
            }

        } else {
            return 0;
        }
    }
}
