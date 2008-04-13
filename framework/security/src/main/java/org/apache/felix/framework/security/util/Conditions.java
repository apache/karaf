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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import org.apache.felix.framework.security.verifier.SignerMatcher;
import org.apache.felix.framework.util.SecureAction;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.BundleSignerCondition;
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

    private final Map m_cache = new WeakHashMap();
    
    private final Bundle m_bundle;
    private final String[] m_signers;

    private final ConditionInfo[] m_conditionInfos;
    private final Condition[] m_conditions;
    private final SecureAction m_action;

    public Conditions(SecureAction action)
    {
        this(null, null, null, action);
    }
    
    private Conditions(Bundle bundle, String[] signers,
        ConditionInfo[] conditions, SecureAction action)
    {
        m_bundle = bundle;
        m_signers = signers;
        m_conditionInfos = conditions;
        m_conditions = ((conditions != null) && (bundle != null)) ? new Condition[m_conditionInfos.length] : null;
        m_action = action;
    }

    public Conditions getConditions(Bundle bundle, String[] signers,
        ConditionInfo[] conditions)
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
            if (bundle != null)
            {
                result = (Conditions) index.get(bundle);
            }
        }
        
        if (result == null)
        {
            result = new Conditions(bundle, signers, conditions, m_action);
            synchronized (index)
            {
                index.put(bundle, result);
            }
        }
        
        return result;
    }

    // See whether the given list is satisfied or not
    public boolean isSatisfied(List posts)
    {
        for (int i = 0; i < m_conditions.length; i++)
        {
            if (m_bundle == null)
            {
                if (!m_conditionInfos[i].getType().equals(
                    BundleSignerCondition.class.getName()))
                {
                    return false;
                }
                String[] args = m_conditionInfos[i].getArgs();

                boolean match = false;
                if (args.length == 0)
                {
                    for (int j = 0; j < m_signers.length; j++)
                    {
                        if (SignerMatcher.match(args[0], m_signers[j]))
                        {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match)
                {
                    return false;
                }
                continue;
            }
            try
            {
                Condition condition = null;
                boolean add = false;
                Class clazz = Class.forName(m_conditionInfos[i].getType());
                
                synchronized (m_conditionInfos)
                {
                    condition = m_conditions[i];
                }
                
                if (condition == null)
                {
                    add = true;
                    condition = createCondition(m_bundle, clazz, m_conditionInfos[i]);
                }
                
                if (condition.isPostponed())
                {
                    posts.add(condition);
                    if (add)
                    {
                        synchronized (m_conditionInfos)
                        {
                            if (m_conditions[i] == null)
                            {
                                m_conditions[i] = condition;
                            }
                        }
                    }
                }
                else
                {
                    Object current = m_conditionStack.get();

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
                        boolean result = condition.isSatisfied();

                        if (!condition.isMutable() && ((condition != Condition.TRUE) && (condition != Condition.FALSE)))
                        {
                            synchronized (m_conditionInfos)
                            {
                                m_conditions[i] = result ? Condition.TRUE : Condition.FALSE;
                            }
                        }
                        else
                        {
                            synchronized (m_conditionInfos)
                            {
                                m_conditions[i] = condition;
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
        return _evalRecursive(entries, 0, new ArrayList(), new HashMap());
    }

    private boolean _evalRecursive(List entries, int pos, List acc, Map contexts)
    {
        if (pos == entries.size())
        {
            // we need to group by type by tuple
            Map conditions = new HashMap();
            for (Iterator iter = acc.iterator(); iter.hasNext();)
            {
                for (Iterator iter2 = ((List) iter.next()).iterator(); iter2
                    .hasNext();)
                {
                    Object entry = iter2.next();
                    Set group = (Set) conditions.get(entry.getClass());

                    if (group == null)
                    {
                        group = new HashSet();
                    }
                    group.add(entry);

                    conditions.put(entry.getClass(), group);
                }
            }

            // and then eval per group
            for (Iterator iter = conditions.entrySet().iterator(); iter.hasNext();)
            {
                Entry entry = (Entry) iter.next();
                Class key = (Class) entry.getKey();
                
                Hashtable context = (Hashtable) contexts.get(key);
                if (context == null)
                {
                    context = new Hashtable();
                    contexts.put(key, context);
                }
                Set set = (Set) entry.getValue();
                Condition[] current =
                    (Condition[]) set.toArray(new Condition[set.size()]);

                // We must be catching recursive evaluation as per spec, hence use a thread
                // local stack to do so
                Object currentCond = m_conditionStack.get();

                if (currentCond == null)
                {
                    m_conditionStack.set(key);
                }
                else
                {
                    if (currentCond instanceof HashSet)
                    {
                        if (((HashSet) currentCond).contains(key))
                        {
                            return false;
                        }
                        ((HashSet) currentCond).add(key);
                    }
                    else
                    {
                        if (currentCond == key)
                        {
                            return false;
                        }
                        HashSet frame = new HashSet();
                        frame.add(current);
                        frame.add(key);
                        m_conditionStack.set(frame);
                        currentCond = frame;
                    }
                }
                try
                {
                    if (!current[0].isSatisfied(current, context))
                    {
                        return false;
                    }
                }
                finally
                {
                    if (currentCond == null)
                    {
                        m_conditionStack.set(null);
                    }
                    else
                    {
                        ((HashSet) currentCond).remove(key);
                        if (((HashSet) currentCond).isEmpty())
                        {
                            m_conditionStack.set(null);
                        }
                    }
                }
            }
            return true;
        }

        List entry = (List) entries.get(pos);

        for (int i = 0; i < entry.size(); i++)
        {
            acc.add(entry.get(i));

            if (_evalRecursive(entries, pos + 1, acc, contexts))
            {
                return true;
            }

            acc.remove(acc.size() - 1);
        }

        return false;
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
