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
package org.apache.karaf.shell.commands.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.converter.DefaultConverter;
import org.apache.karaf.shell.support.converter.GenericType;
import org.apache.karaf.shell.support.converter.ReifiedType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiate a new object
 */
@Command(scope = "shell", name = "new", description = "Creates a new java object.")
@Service
@SuppressWarnings("rawtypes")
public class NewAction implements Action {
    private static final Logger LOG = LoggerFactory.getLogger(NewAction.class);
    @Argument(name = "class", index = 0, multiValued = false, required = true, description = "FQN of the class to load")
    String clazzName;

    @Argument(name = "args", index = 1, multiValued = true, required = false, description = "Constructor arguments")
    List<Object> args;

    boolean reorderArguments;

    protected DefaultConverter converter;
    
    @Reference
    BundleContext context;

    @Override
    public Object execute() throws Exception {
        if (args == null) {
            args = Collections.emptyList();
        }
        String packageName = getPackageName(clazzName);
        Bundle bundle = getBundleOfferingPackage(packageName);
        LOG.info("Using bundle {} classloader to load {}.", bundle.getSymbolicName(), clazzName);
        ClassLoader classLoader = getClassLoader(bundle);
        converter = new DefaultConverter(classLoader);
        Class<?> clazz = (Class<?>)converter.convert(clazzName, Class.class);
        // Handle arrays
        if (clazz.isArray()) {
            Object obj = Array.newInstance(clazz.getComponentType(), args.size());
            for (int i = 0; i < args.size(); i++) {
                Array.set(obj, i, convert(args.get(i), clazz.getComponentType()));
            }
            return obj;
        }

        // Map of matching constructors
        Map<Constructor, List<Object>> matches = findMatchingConstructors(clazz, args, Arrays.asList(new ReifiedType[args.size()]));
        if (matches.size() == 1) {
            try {
                Map.Entry<Constructor, List<Object>> match = matches.entrySet().iterator().next();
                return newInstance(match.getKey(), match.getValue().toArray());
            } catch (Throwable e) {
                throw new Exception("Error when instantiating object of class " + clazz.getName(), getRealCause(e));
            }
        } else if (matches.size() == 0) {
            throw new Exception("Unable to find a matching constructor on class " + clazz.getName() + " for arguments " + args + " when instantiating object.");
        } else {
            throw new Exception("Multiple matching constructors found on class " + clazz.getName() + " for arguments " + args + " when instantiating object: " + matches.keySet());
        }
    }

    private String getPackageName(String name) {
        int nameSeperator = name.lastIndexOf(".");
        if (nameSeperator <= 0) {
            return null;
        }
        return name.substring(0, nameSeperator);
    }

    /**
     * Get class loader offering a named package. This only works if we do not care
     * which package we get in case of several package versions
     *  
     * @param reqPackageName
     * @return
     */
    private Bundle getBundleOfferingPackage(String reqPackageName) {
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            BundleRevision rev = bundle.adapt(BundleRevision.class);
            if (rev != null) {
                List<BundleCapability> caps = rev.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                for (BundleCapability cap : caps) {
                    Map<String, Object> attr = cap.getAttributes();
                    String packageName = (String)attr.get(BundleRevision.PACKAGE_NAMESPACE);
                    if (packageName.equals(reqPackageName)) {
                        return bundle;
                    }
                }
            }
        }
        return context.getBundle(0);
    }

    private ClassLoader getClassLoader(Bundle bundle) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        return wiring.getClassLoader();
    }

    //
    // Code below comes from Aries blueprint implementation.  Given this code is not available
    // from a public API it has been copied here.
    //
    private Object newInstance(Constructor constructor, Object... args) throws Exception {
        return constructor.newInstance(args);
    }

    private Map<Constructor, List<Object>> findMatchingConstructors(Class type, List<Object> args, List<ReifiedType> types) {
        Map<Constructor, List<Object>> matches = new HashMap<Constructor, List<Object>>();
        // Get constructors
        List<Constructor> constructors = new ArrayList<Constructor>(Arrays.asList(type.getConstructors()));
        // Discard any signature with wrong cardinality
        for (Iterator<Constructor> it = constructors.iterator(); it.hasNext();) {
            if (it.next().getParameterTypes().length != args.size()) {
                it.remove();
            }
        }
        // Find a direct match with assignment
        if (matches.size() != 1) {
            Map<Constructor, List<Object>> nmatches = new HashMap<Constructor, List<Object>>();
            for (Constructor cns : constructors) {
                boolean found = true;
                List<Object> match = new ArrayList<Object>();
                for (int i = 0; i < args.size(); i++) {
                    ReifiedType argType = new GenericType(cns.getGenericParameterTypes()[i]);
                    if (types.get(i) != null && !argType.getRawClass().equals(types.get(i).getRawClass())) {
                        found = false;
                        break;
                    }
                    if (!isAssignable(args.get(i), argType)) {
                        found = false;
                        break;
                    }
                    try {
                        match.add(convert(args.get(i), cns.getGenericParameterTypes()[i]));
                    } catch (Throwable t) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    nmatches.put(cns, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        // Find a direct match with conversion
        if (matches.size() != 1) {
            Map<Constructor, List<Object>> nmatches = new HashMap<Constructor, List<Object>>();
            for (Constructor cns : constructors) {
                boolean found = true;
                List<Object> match = new ArrayList<Object>();
                for (int i = 0; i < args.size(); i++) {
                    ReifiedType argType = new GenericType(cns.getGenericParameterTypes()[i]);
                    if (types.get(i) != null && !argType.getRawClass().equals(types.get(i).getRawClass())) {
                        found = false;
                        break;
                    }
                    try {
                        Object val = convert(args.get(i), argType);
                        match.add(val);
                    } catch (Throwable t) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    nmatches.put(cns, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        // Start reordering with assignment
        if (matches.size() != 1 && reorderArguments && args.size() > 1) {
            Map<Constructor, List<Object>> nmatches = new HashMap<Constructor, List<Object>>();
            for (Constructor cns : constructors) {
                ArgumentMatcher matcher = new ArgumentMatcher(cns.getGenericParameterTypes(), false);
                List<Object> match = matcher.match(args, types);
                if (match != null) {
                    nmatches.put(cns, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        // Start reordering with conversion
        if (matches.size() != 1 && reorderArguments && args.size() > 1) {
            Map<Constructor, List<Object>> nmatches = new HashMap<Constructor, List<Object>>();
            for (Constructor cns : constructors) {
                ArgumentMatcher matcher = new ArgumentMatcher(cns.getGenericParameterTypes(), true);
                List<Object> match = matcher.match(args, types);
                if (match != null) {
                    nmatches.put(cns, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        return matches;
    }

    protected Object convert(Object obj, Type type) throws Exception {
        return converter.convert(obj, new GenericType(type));
    }

    protected Object convert(Object obj, ReifiedType type) throws Exception {
        return converter.convert(obj, type);
    }

    @SuppressWarnings("unchecked")
    public static boolean isAssignable(Object source, ReifiedType target) {
        return source == null
                || (target.size() == 0
                    && unwrap(target.getRawClass()).isAssignableFrom(unwrap(source.getClass())));
    }

    private static Class unwrap(Class c) {
        Class u = primitives.get(c);
        return u != null ? u : c;
    }

    private static final Map<Class, Class> primitives;
    static {
        primitives = new HashMap<Class, Class>();
        primitives.put(byte.class, Byte.class);
        primitives.put(short.class, Short.class);
        primitives.put(char.class, Character.class);
        primitives.put(int.class, Integer.class);
        primitives.put(long.class, Long.class);
        primitives.put(float.class, Float.class);
        primitives.put(double.class, Double.class);
        primitives.put(boolean.class, Boolean.class);
    }


    private static Object UNMATCHED = new Object();

    private class ArgumentMatcher {

        private List<TypeEntry> entries;
        private boolean convert;

        public ArgumentMatcher(Type[] types, boolean convert) {
            entries = new ArrayList<TypeEntry>();
            for (Type type : types) {
                entries.add(new TypeEntry(new GenericType(type)));
            }
            this.convert = convert;
        }

        public List<Object> match(List<Object> arguments, List<ReifiedType> forcedTypes) {
            if (find(arguments, forcedTypes)) {
                return getArguments();
            }
            return null;
        }

        private List<Object> getArguments() {
            List<Object> list = new ArrayList<Object>();
            for (TypeEntry entry : entries) {
                if (entry.argument == UNMATCHED) {
                    throw new RuntimeException("There are unmatched types");
                } else {
                    list.add(entry.argument);
                }
            }
            return list;
        }

        private boolean find(List<Object> arguments, List<ReifiedType> forcedTypes) {
            if (entries.size() == arguments.size()) {
                boolean matched = true;
                for (int i = 0; i < arguments.size() && matched; i++) {
                    matched = find(arguments.get(i), forcedTypes.get(i));
                }
                return matched;
            }
            return false;
        }

        private boolean find(Object arg, ReifiedType forcedType) {
            for (TypeEntry entry : entries) {
                Object val = arg;
                if (entry.argument != UNMATCHED) {
                    continue;
                }
                if (forcedType != null) {
                    if (!forcedType.equals(entry.type)) {
                        continue;
                    }
                } else if (arg != null) {
                    if (convert) {
                        try {
                            // TODO: call canConvert instead of convert()
                            val = convert(arg, entry.type);
                        } catch (Throwable t) {
                            continue;
                        }
                    } else {
                        if (!isAssignable(arg, entry.type)) {
                            continue;
                        }
                    }
                }
                entry.argument = val;
                return true;
            }
            return false;
        }

    }

    private static class TypeEntry {

        private final ReifiedType type;
        private Object argument;

        public TypeEntry(ReifiedType type) {
            this.type = type;
            this.argument = UNMATCHED;
        }

    }

    public static Throwable getRealCause(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }
}
