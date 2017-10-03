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
package org.apache.karaf.scr.management.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;

public class ScrService
{

    private final BundleContext context;
    private final ServiceComponentRuntime runtime;


    public ScrService(BundleContext context, final ServiceComponentRuntime runtime)
    {
        // we always use the system bundle to avoid problems if subsystems etc.
        // are used and the SCR implemented extends those "invisible" bundles
        this.context = context.getBundle(0).getBundleContext();
        this.runtime = runtime;
    }


    // ScrService

    public Component[] getComponents()
    {
        List<Component> result = new ArrayList<>();

        final Collection<ComponentDescriptionDTO> descriptions = this.runtime.getComponentDescriptionDTOs();
        for(final ComponentDescriptionDTO descDTO : descriptions )
        {
            final Collection<ComponentConfigurationDTO> configs = this.runtime.getComponentConfigurationDTOs(descDTO);
            ComponentConfigurationDTO configDTO = null;
            if ( !configs.isEmpty() )
            {
                configDTO = configs.iterator().next();
            }
            result.add(new Component(this.context, this.runtime, descDTO, configDTO));
        }

        return result.toArray( new Component[result.size()] );
    }


    public Component[] getComponents(final String componentName )
    {
        List<Component> result = new ArrayList<Component>();

        final Collection<ComponentDescriptionDTO> descriptions = this.runtime.getComponentDescriptionDTOs();
        for(final ComponentDescriptionDTO descDTO : descriptions )
        {
            if ( descDTO.name.equals(componentName) ) {
                final Collection<ComponentConfigurationDTO> configs = this.runtime.getComponentConfigurationDTOs(descDTO);
                ComponentConfigurationDTO configDTO = null;
                if ( !configs.isEmpty() )
                {
                    configDTO = configs.iterator().next();
                }
                result.add(new Component(this.context, this.runtime, descDTO, configDTO));
            }
        }

        return result.isEmpty() ? null : result.toArray( new Component[result.size()] );
    }


    public static final class Component
    {
        /**
         * The Component has just been created and is still disabled or it has
         * been disabled by calling the {@link #disable()} method (value is 1).
         */
        public static final int STATE_DISABLED = 1;

        /**
         * The Component is being enabled (value is 512). After the component has
         * been enabled it enters the {@link #STATE_UNSATISFIED} state.
         * @since 1.2
         * @deprecated since 1.8.0
         */
        @Deprecated
        public static final int STATE_ENABLING = 512;

        /**
         * The Component has been enabled and is now going to be activated (value
         * is 2).
         * @deprecated as of version 1.2 the enabled state is collapsed into the
         *      {@link #STATE_UNSATISFIED} state. This status code is never returned
         *      from the {@link #getState()} method.
         */
        @Deprecated
        public static final int STATE_ENABLED = 2;

        /**
         * The Component activation failed because any dependency is not satisfied
         * (value is 4).
         */
        public static final int STATE_UNSATISFIED = 4;

        /**
         * The Component is currently being activated either because it has been
         * enabled or because any dependency which was previously unsatisfied has
         * become satisfied (value is 8).
         * @deprecated since 1.8.0 transient states are no longer used
         */
        @Deprecated
        public static final int STATE_ACTIVATING = 8;

        /**
         * The Component has successfully been activated and is fully functional
         * (value is 16). This is the state of immediate components after
         * successful activation. Delayed and Service Factory Components enter
         * this state when the service instance has actually been instantiated because
         * the service has been acquired.
         */
        public static final int STATE_ACTIVE = 16;

        /**
         * The Component has successfully been activated but is a Delayed or Service
         * Factory Component pending instantiation on first use (value is 32).
         */
        public static final int STATE_REGISTERED = 32;

        /**
         * The Component is a Component Factory ready to create Component instances
         * with the <code>ComponentFactory.newInstance(Dictionary)</code> method
         * or (if enabled with the <code>ds.factory.enabled</code> configuration) to
         * manage Component instances from configuration data received from the
         * Configuration Admin Service (value is 64).
         */
        public static final int STATE_FACTORY = 64;

        /**
         * The Component is being deactivated either because it is being disabled
         * or because a dependency is not satisfied any more (value is 128). After
         * deactivation the Component enters the {@link #STATE_UNSATISFIED} state.
         * @deprecated since 1.8.0 transient states are no longer used
         */
        @Deprecated
        public static final int STATE_DEACTIVATING = 128;

        /**
         * The Component is being disabled (value is 1024). After the component has
         * been disabled it enters the {@link #STATE_DISABLED} state.
         * @since 1.2
         * @deprecated since 1.8.0 transient states are no longer used
         */
        @Deprecated
        public static final int STATE_DISABLING = 1024;

        /**
         * The Component is being disposed off (value is 2048). After the component
         * has been disposed off it enters the {@link #STATE_DESTROYED} state.
         * @since 1.2
         * @deprecated since 1.8.0 transient states are no longer used
         */
        @Deprecated
        public static final int STATE_DISPOSING = 2048;

        /**
         * The Component has been destroyed and cannot be used any more (value is
         * 256). This state is only used when the bundle declaring the component
         * is being stopped and all components have to be removed.
         * @deprecated as of version 1.2 this constant has been renamed to
         *      {@link #STATE_DISPOSED}.
         */
        @Deprecated
        public static final int STATE_DESTROYED = 256;

        /**
         * The Component has been disposed off and cannot be used any more (value is
         * 256). This state is used when the bundle declaring the component
         * is being stopped and all components have to be removed. This status is
         * also the final status of a component after the
         * <code>ComponentInstance.dispose()</code> method has been called.
         * @since 1.2
         */
        public static final int STATE_DISPOSED = 256;

        private final ComponentDescriptionDTO description;

        private final ComponentConfigurationDTO configuration;

        private final BundleContext bundleContext;

        private final ServiceComponentRuntime runtime;

        public Component(final BundleContext bundleContext,
                         final ServiceComponentRuntime runtime,
                         final ComponentDescriptionDTO description,
                         final ComponentConfigurationDTO configuration)
        {
            this.bundleContext = bundleContext;
            this.description = description;
            this.configuration = configuration;
            this.runtime = runtime;
        }

        public long getId()
        {
            return configuration != null ? configuration.id : -1;
        }

        public String getName()
        {
            return description.name;
        }

        public int getState()
        {
            if ( configuration == null )
            {
                return STATE_UNSATISFIED; // TODO Check!
            }
            final int s = configuration.state;
            switch ( s )
            {
                case ComponentConfigurationDTO.ACTIVE : return STATE_ACTIVE;
                case ComponentConfigurationDTO.SATISFIED : return STATE_ENABLED;
                case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION : return STATE_UNSATISFIED;
                case ComponentConfigurationDTO.UNSATISFIED_REFERENCE : return STATE_UNSATISFIED;
                default: // satisfied
                    return STATE_ENABLED;
            }
        }

        public Bundle getBundle()
        {
            return this.bundleContext.getBundle(this.description.bundle.id);
        }

        public String getFactory()
        {
            return this.description.factory;
        }

        public boolean isServiceFactory()
        {
            return !"singleton".equals(this.description.scope);
        }

        public String getClassName()
        {
            return this.description.implementationClass;
        }

        public boolean isDefaultEnabled()
        {
            return this.description.defaultEnabled;
        }

        public boolean isImmediate()
        {
            return this.description.immediate;
        }

        public String[] getServices()
        {
            return this.description.serviceInterfaces.length == 0 ? null : this.description.serviceInterfaces;
        }

        public Dictionary getProperties()
        {
            return new Hashtable<>(this.description.properties);
        }

        public Reference[] getReferences()
        {
            if ( this.configuration == null )
            {
                return null;
            }

            final List<Reference> result = new ArrayList<Reference>();
            for(final ReferenceDTO dto : this.description.references)
            {
                SatisfiedReferenceDTO sRef = null;
                for(final SatisfiedReferenceDTO r : this.configuration.satisfiedReferences)
                {
                    if ( r.name.equals(dto.name) )
                    {
                        sRef = r;
                        break;
                    }
                }
                result.add(new Reference(this.bundleContext, dto, sRef));
            }

            if ( result.isEmpty() )
            {
                return null;
            }

            return result.toArray(new Reference[result.size()]);
        }

        public ComponentInstance getComponentInstance()
        {
            // returning null as we should have never returned this in the first place
            return null;
        }

        public String getActivate()
        {
            return this.description.activate;
        }

        public boolean isActivateDeclared()
        {
            return this.description.activate != null;
        }

        public String getDeactivate()
        {
            return this.description.deactivate;
        }

        public boolean isDeactivateDeclared()
        {
            return this.description.deactivate != null;
        }

        public String getModified()
        {
            return this.description.modified;
        }

        public String getConfigurationPolicy()
        {
            return this.description.configurationPolicy;
        }

        public String getConfigurationPid()
        {
            final String[] pids = this.description.configurationPid;
            return pids[0];
        }

        public boolean isConfigurationPidDeclared()
        {
            return true;
        }

        public void enable()
        {
            // noop as the old model was broken
        }

        public void disable()
        {
            // noop as the old model was broken
        }
    }

    public static final class Reference
    {
        // constant for option single reference - 0..1
        private static final String CARDINALITY_0_1 = "0..1";

        // constant for option multiple reference - 0..n
        private static final String CARDINALITY_0_N = "0..n";

        // constant for required multiple reference - 1..n
        private static final String CARDINALITY_1_N = "1..n";

        // constant for static policy
        private static final String POLICY_STATIC = "static";

        // constant for reluctant policy option
        private static final String POLICY_OPTION_RELUCTANT = "reluctant";

        private final ReferenceDTO dto;

        private final SatisfiedReferenceDTO satisfiedDTO;

        private final BundleContext bundleContext;

        public Reference(
                final BundleContext bundleContext,
                final ReferenceDTO dto,
                final SatisfiedReferenceDTO satisfied)
        {
            this.bundleContext = bundleContext;
            this.dto = dto;
            this.satisfiedDTO = satisfied;
        }

        public String getName()
        {
            return dto.name;
        }

        public String getServiceName()
        {
            return dto.interfaceName;
        }

        public ServiceReference[] getServiceReferences()
        {
            if ( this.satisfiedDTO == null )
            {
                return null;
            }
            final List<ServiceReference<?>> refs = new ArrayList<ServiceReference<?>>();
            for(ServiceReferenceDTO dto : this.satisfiedDTO.boundServices)
            {
                try
                {
                    final ServiceReference<?>[] serviceRefs = this.bundleContext.getServiceReferences((String)null,
                            "(" + Constants.SERVICE_ID + "=" + String.valueOf(dto.id) + ")");
                    if ( serviceRefs != null && serviceRefs.length > 0 )
                    {
                        refs.add(serviceRefs[0]);
                    }
                }
                catch ( final InvalidSyntaxException ise)
                {
                    // ignore
                }
            }
            return refs.toArray(new ServiceReference<?>[refs.size()]);
        }

        public ServiceReference<?>[] getBoundServiceReferences()
        {
            return this.getServiceReferences();
        }

        public boolean isSatisfied()
        {
            return this.satisfiedDTO != null;
        }

        public boolean isOptional()
        {
            return CARDINALITY_0_1.equals(dto.cardinality) || CARDINALITY_0_N.equals(dto.cardinality);
        }

        public boolean isMultiple()
        {
            return CARDINALITY_1_N.equals(dto.cardinality) || CARDINALITY_0_N.equals(dto.cardinality);
        }

        public boolean isStatic()
        {
            return POLICY_STATIC.equals(dto.policy);
        }

        public boolean isReluctant()
        {
            return POLICY_OPTION_RELUCTANT.equals(dto.policyOption);
        }

        public String getTarget()
        {
            return this.dto.target;
        }

        public String getBindMethodName()
        {
            return this.dto.bind;
        }

        public String getUnbindMethodName()
        {
            return this.dto.unbind;
        }

        public String getUpdatedMethodName()
        {
            return this.dto.unbind;
        }
    }
}
