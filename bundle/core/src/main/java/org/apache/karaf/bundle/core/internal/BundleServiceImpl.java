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
package org.apache.karaf.bundle.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

public class BundleServiceImpl implements BundleService {

    private final BundleContext bundleContext;
    private final List<BundleStateService> stateServices;

    public BundleServiceImpl(BundleContext bundleContext, List<BundleStateService> stateServices) {
        this.bundleContext = bundleContext;
        this.stateServices = stateServices;
    }

    @Override
    public List<Bundle> selectBundles(List<String> ids, boolean defaultAllBundles) {
        return new BundleSelectorImpl(bundleContext).selectBundles(ids, defaultAllBundles);
    }

    @Override
    public BundleInfo getInfo(Bundle bundle) {
        BundleState combinedState = BundleState.Unknown;
        for (BundleStateService stateService : this.stateServices) {
            BundleState extState = stateService.getState(bundle);
            if (extState != BundleState.Unknown) {
                combinedState = extState;
            }
        }
        return  new BundleInfoImpl(bundle, combinedState);
    }

    @Override
    public String getDiag(Bundle bundle) {
        StringBuilder message = new StringBuilder();
        for (BundleStateService bundleStateService : stateServices) {
            String part = bundleStateService.getDiag(bundle);
            if (part != null) {
                message.append(bundleStateService.getName() + part);
            }
        }
        if (bundle.getState() == Bundle.INSTALLED) {
            System.out.println("Unsatisfied Requirements:");
            List<BundleRequirement> reqs = getUnsatisfiedRquirements(bundle, null);
            for (BundleRequirement req : reqs) {
                System.out.println(req);
            }
        }
        return message.toString();
    }
    
    @Override
    public List<BundleRequirement> getUnsatisfiedRquirements(Bundle bundle, String namespace) {
        List<BundleRequirement> result = new ArrayList<BundleRequirement>();
        BundleRevision rev = bundle.adapt(BundleRevision.class);
        if (rev != null) {
            List<BundleRequirement> reqs = rev.getDeclaredRequirements(namespace);
            for (BundleRequirement req : reqs) {
                if (!canBeSatisfied(req)) {
                    result.add(req);
                }
            }
        }
        return result;
    }
    
    private boolean canBeSatisfied(BundleRequirement req) {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            if (wiring != null) {
                List<BundleCapability> caps = wiring.getCapabilities(null);
                for (BundleCapability cap : caps) {
                    if (req.matches(cap)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
