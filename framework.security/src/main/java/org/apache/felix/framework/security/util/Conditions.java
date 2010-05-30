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
package org.apache.felix.framework.security.util;

import java.security.Permission;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.felix.framework.security.condpermadmin.ConditionalPermissionInfoImpl;
import org.apache.felix.framework.util.SecureAction;

//import org.apache.felix.moduleloader.IModule;
import org.apache.felix.framework.resolver.Module;

import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

/**
 * This class caches conditions instances by their infos. Furthermore, it allows
 * to eval postponed condition permission tuples as per spec (see 9.45).
 */
// TODO: maybe use bundle events instead of soft/weak references.
public final class Conditions
{
    private static final ThreadLocal m_conditionStack = new ThreadLocal();
    private static final Map m_conditionCache = new WeakHashMap();

    private final Map m_cache = new WeakHashMap();

    private final Module m_module;

    private final ConditionInfo[] m_conditionInfos;
    private final Condition[] m_conditions;
    private final SecureAction m_action;

    public Conditions(SecureAction action)
    {
        this(null, null, action);
    }

    private Conditions(Module module, ConditionInfo[] conditionInfos,
        SecureAction action)
    {
        m_module = module;
        m_conditionInfos = conditionInfos;
        if ((module != null) && (conditionInfos != null))
        {
            synchronized (m_conditionCache)
            {
                Map conditionMap = (Map) m_conditionCache.get(module);
                if (conditionMap == null)
                {
                    conditionMap = new HashMap();
                    conditionMap.put(m_conditionInfos,
                        new Condition[m_conditionInfos.length]);
                    m_conditionCache.put(module, conditionMap);
                }
                Condition[] conditions = (Condition[]) conditionMap
                    .get(m_conditionInfos);
                if (conditions == null)
                {
                    conditions = new Condition[m_conditionInfos.length];
                    conditionMap.put(m_conditionInfos, conditions);
                }
                m_conditions = conditions;
            }
        }
        else
        {
            m_conditions = null;
        }
        m_action = action;
    }

    public Conditions getConditions(Module key, ConditionInfo[] conditions)
    {
        Conditions result = null;
        Map index = null;
        synchronized (m_cache)
        {
            index = (Map) m_cache.get(conditions);
            if (index == null)
            {
                index = new WeakHashMap();
                m_cache.put(conditions, index);
            }
        }
        synchronized (index)
        {
            if (key != null)
            {
                result = (Conditions) index.get(key);
            }
        }

        if (result == null)
        {
            result = new Conditions(key, conditions, m_action);
            synchronized (index)
            {
                index.put(key, result);
            }
        }

        return result;
    }

    // See whether the given list is satisfied or not
    public boolean isSatisfied(List posts, Permissions permissions,
        Permission permission)
    {
        boolean check = true;
        for (int i = 0; i < m_conditionInfos.length; i++)
        {
            if (m_module == null)
            {
                // TODO: check whether this is correct!
                break;
            }
            try
            {
                Condition condition = null;
                boolean add = false;
                Class clazz = Class.forName(m_conditionInfos[i].getType());

                synchronized (m_conditions)
                {
                    if (m_conditions[i] == null)
                    {
                        m_conditions[i] = createCondition(m_module.getBundle(),
                            clazz, m_conditionInfos[i]);
                    }
                    condition = m_conditions[i];
                }

                Object current = m_conditionStack.get();
                if (current != null)
                {
                    if (current instanceof HashSet)
                    {
                        if (((HashSet) current).contains(clazz))
                        {
                            return false;
                        }
                    }
                    else
                    {
                        if (current == clazz)
                        {
                            return false;
                        }
                    }
                }

                if (condition.isPostponed())
                {
                    if (check && !permissions.implies(permission, null))
                    {
                        return false;
                    }
                    else
                    {
                        check = false;
                    }
                    posts.add(new Object[] { condition, new Integer(i) });
                }
                else
                {

                    if (current == null)
                    {
                        m_conditionStack.set(clazz);
                    }
                    else
                    {
                        if (current instanceof HashSet)
                        {
                            if (((HashSet) current).contains(clazz))
                            {
                                return false;
                            }
                            ((HashSet) current).add(clazz);
                        }
                        else
                        {
                            if (current == clazz)
                            {
                                return false;
                            }
                            HashSet frame = new HashSet();
                            frame.add(current);
                            frame.add(clazz);
                            m_conditionStack.set(frame);
                            current = frame;
                        }
                    }
                    try
                    {
                        boolean mutable = condition.isMutable();
                        boolean result = condition.isSatisfied();

                        if (!mutable
                            && ((condition != Condition.TRUE) && (condition != Condition.FALSE)))
                        {
                            synchronized (m_conditions)
                            {
                                m_conditions[i] = result ? Condition.TRUE
                                    : Condition.FALSE;
                            }
                        }
                        if (!result)
                        {
                            return false;
                        }
                    }
                    finally
                    {
                        if (current == null)
                        {
                            m_conditionStack.set(null);
                        }
                        else
                        {
                            ((HashSet) current).remove(clazz);
                            if (((HashSet) current).isEmpty())
                            {
                                m_conditionStack.set(null);
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                // TODO: log this as per spec
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public boolean evalRecursive(List entries)
    {
        Map contexts = new HashMap();
        outer: for (Iterator iter = entries.iterator(); iter.hasNext();)
        {
            List tuples = (List) iter.next();
            inner: for (Iterator inner = tuples.iterator(); inner.hasNext();)
            {
                Object[] entry = (Object[]) inner.next();
                List conditions = (List) entry[1];
                if (conditions == null)
                {
                    if (!((ConditionalPermissionInfoImpl) entry[0]).isAllow())
                    {
                        return false;
                    }
                    continue outer;
                }
                for (Iterator iter2 = conditions.iterator(); iter2.hasNext();)
                {
                    Object[] condEntry = (Object[]) iter2.next();
                    Condition cond = (Condition) condEntry[0];
                    Dictionary context = (Dictionary) contexts.get(cond
                        .getClass());
                    if (context == null)
                    {
                        context = new Hashtable();
                        contexts.put(cond.getClass(), context);
                    }
                    Object current = m_conditionStack.get();
                    if (current == null)
                    {
                        m_conditionStack.set(cond.getClass());
                    }
                    else
                    {
                        if (current instanceof HashSet)
                        {
                            ((HashSet) current).add(cond.getClass());
                        }
                        else
                        {
                            HashSet frame = new HashSet();
                            frame.add(current);
                            frame.add(cond.getClass());
                            m_conditionStack.set(frame);
                            current = frame;
                        }
                    }
                    boolean result;
                    boolean mutable = cond.isMutable();
                    try
                    {
                        result = cond.isSatisfied(new Condition[] { cond },
                            context);
                    }
                    finally
                    {
                        if (current == null)
                        {
                            m_conditionStack.set(null);
                        }
                        else
                        {
                            ((HashSet) current).remove(cond.getClass());
                            if (((HashSet) current).isEmpty())
                            {
                                m_conditionStack.set(null);
                            }
                        }
                    }
                    if (!mutable && (cond != Condition.TRUE)
                        && (cond != Condition.FALSE))
                    {
                        synchronized (((Conditions) entry[2]).m_conditions)
                        {
                            ((Conditions) entry[2]).m_conditions[((Integer) condEntry[1])
                                .intValue()] = result ? Condition.TRUE
                                : Condition.FALSE;
                        }
                    }
                    if (!result)
                    {
                        continue inner;
                    }
                }
                if (!((ConditionalPermissionInfoImpl) entry[0]).isAllow())
                {
                    return false;
                }
                continue outer;
            }
            return false;
        }
        return true;
    }

    private Condition createCondition(final Bundle bundle, final Class clazz,
        final ConditionInfo info) throws Exception
    {
        try
        {
            return (Condition) m_action.getMethod(clazz, "getCondition",
                new Class[] { Bundle.class, ConditionInfo.class }).invoke(null,
                new Object[] { bundle, info });
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return (Condition) m_action.getConstructor(clazz,
                new Class[] { Bundle.class, ConditionInfo.class }).newInstance(
                new Object[] { bundle, info });
        }
    }
}
