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
package org.apache.felix.karaf.shell.commands;

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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.karaf.shell.console.commands.GenericType;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * Execute a closure on a list of arguments.
 */
@Command(scope = "shell", name = "new", description = "Creates a new java object.")
public class NewAction extends OsgiCommandSupport {

    @Argument(name = "class", index = 0, multiValued = false, required = true, description = "The object class")
    Class clazz;

    @Argument(name = "args", index = 1, multiValued = true, required = false, description = "Constructor arguments")
    List<Object> args;

    boolean reorderArguments;

    protected Converter blueprintConverter;

    public void setBlueprintConverter(Converter blueprintConverter) {
        this.blueprintConverter = blueprintConverter;
    }

    @Override
    protected Object doExecute() throws Exception {
        // Map of matching constructors
        Map<Constructor, List<Object>> matches = findMatchingConstructors(clazz, args, Arrays.asList(new ReifiedType[args.size()]));
        if (matches.size() == 1) {
            try {
                Map.Entry<Constructor, List<Object>> match = matches.entrySet().iterator().next();
                return newInstance(match.getKey(), match.getValue().toArray());
            } catch (Throwable e) {
                throw new ComponentDefinitionException("Error when instanciating object of class " + clazz.getName(), getRealCause(e));
            }
        } else if (matches.size() == 0) {
            throw new ComponentDefinitionException("Unable to find a matching constructor on class " + clazz.getName() + " for arguments " + args + " when instanciating object.");
        } else {
            throw new ComponentDefinitionException("Multiple matching constructors found on class " + clazz.getName() + " for arguments " + args + " when instanciating object: " + matches.keySet());
        }
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
        return blueprintConverter.convert(obj,  new GenericType(type));
    }

    protected Object convert(Object obj, ReifiedType type) throws Exception {
        return blueprintConverter.convert(obj,  type);
    }

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