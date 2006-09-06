/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.scr;

import java.lang.reflect.Method;
import java.util.*;

import org.osgi.framework.*;

/**
 * This activator is used to cover requirement described in section 112.8.1 @@ -27,14
 * 37,202 @@ in active bundles.
 * 
 */
public class Activator implements BundleActivator, SynchronousBundleListener
{

    // map of GenericActivator instances per Bundle indexed by Bundle symbolic
    // name
    private Map m_componentBundles;

    /**
     * Registers this instance as a (synchronous) bundle listener and loads the
     * components of already registered bundles.
     * 
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    public void start(BundleContext context) throws Exception
    {
        m_componentBundles = new HashMap();

        // register for bundle updates
        context.addBundleListener(this);

        // 112.8.2 load all components of active bundles
        loadAllComponents(context);
    }

    /**
     * Unregisters this instance as a bundle listener and unloads all components
     * which have been registered during the active life time of the SCR
     * implementation bundle.
     * 
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
        // unregister as bundle listener
        context.removeBundleListener(this);

        // 112.8.2 dispose off all active components
        disposeAllComponents();
    }

    // ---------- BundleListener Interface -------------------------------------

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     * 
     * @param event The <code>BundleEvent</code> representing the bundle state
     *      change.
     */
    public void bundleChanged(BundleEvent event)
    {
        if (event.getType() == BundleEvent.STARTED)
        {
            loadComponents(event.getBundle());
        }
        else if (event.getType() == BundleEvent.STOPPING)
        {
            disposeComponents(event.getBundle());
        }
    }

    // ---------- Component Management -----------------------------------------

    // Loads the components of all bundles currently active.
    private void loadAllComponents(BundleContext context)
    {
        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            Bundle bundle = bundles[i];
            if (bundle.getState() == Bundle.ACTIVE)
            {
                loadComponents(bundle);
            }
        }
    }

    /**
     * Loads the components of the given bundle. If the bundle has no
     * <i>Service-Component</i> header, this method has no effect. The
     * fragments of a bundle are not checked for the header (112.4.1).
     * <p>
     * This method calls the {@link #getBundleContext(Bundle)} method to find
     * the <code>BundleContext</code> of the bundle. If the context cannot be
     * found, this method does not load components for the bundle.
     */
    private void loadComponents(Bundle bundle)
    {
        if (bundle.getHeaders().get("Service-Component") == null)
        {
            // no components in the bundle, abandon
            return;
        }

        // there should be components, load them with a bundle context
        BundleContext context = getBundleContext(bundle);
        if (context == null)
        {
            GenericActivator.error("Cannot get BundleContext of bundle "
                + bundle.getSymbolicName());
            return;
        }

        GenericActivator ga = new GenericActivator();
        try
        {
            ga.start(context);
            m_componentBundles.put(bundle.getSymbolicName(), ga);
        }
        catch (Exception e)
        {
            GenericActivator.exception("Error while loading components "
                + "of bundle " + bundle.getSymbolicName(), null, e);
        }
    }

    /**
     * Unloads components of the given bundle. If no components have been loaded
     * for the bundle, this method has no effect.
     */
    private void disposeComponents(Bundle bundle)
    {
        String name = bundle.getSymbolicName();
        GenericActivator ga = (GenericActivator) m_componentBundles.remove(name);
        if (ga != null)
        {
            try
            {
                ga.dispose();
            }
            catch (Exception e)
            {
                GenericActivator.exception("Error while disposing components "
                    + "of bundle " + name, null, e);
            }
        }
    }

    // Unloads all components registered with the SCR
    private void disposeAllComponents()
    {
        for (Iterator it = m_componentBundles.values().iterator(); it.hasNext();)
        {
            GenericActivator ga = (GenericActivator) it.next();
            try
            {
                ga.dispose();
            }
            catch (Exception e)
            {
                GenericActivator.exception(
                    "Error while disposing components of bundle "
                        + ga.getBundleContext().getBundle().getSymbolicName(),
                    null, e);
            }
            it.remove();
        }
    }

    /**
     * Returns the <code>BundleContext</code> of the bundle.
     * <p>
     * This method assumes a <code>getContext</code> method returning a
     * <code>BundleContext</code> instance to be present in the class of the
     * bundle or any of its parent classes.
     * 
     * @param bundle The <code>Bundle</code> whose context is to be returned.
     * 
     * @return The <code>BundleContext</code> of the bundle or
     *         <code>null</code> if no <code>getContext</code> method
     *         returning a <code>BundleContext</code> can be found.
     */
    private BundleContext getBundleContext(Bundle bundle)
    {
        for (Class clazz = bundle.getClass(); clazz != null; clazz = clazz.getSuperclass())
        {
            try
            {
                Method m = clazz.getDeclaredMethod("getContext", null);
                if (m.getReturnType().equals(BundleContext.class))
                {
                    m.setAccessible(true);
                    return (BundleContext) m.invoke(bundle, null);
                }
            }
            catch (NoSuchMethodException nsme)
            {
                // don't actually care, just try super class
            }
            catch (Throwable t)
            {
                GenericActivator.exception("Cannot get BundleContext for "
                    + bundle.getSymbolicName(), null, t);
            }
        }

        // fall back to nothing
        return null;
    }
}