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
package org.apache.felix.scrplugin.tags.annotation.defaulttag;

import java.util.List;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.tags.*;

import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.annotation.AnnotationFieldRef;
import com.thoughtworks.qdox.model.annotation.EvaluatingVisitor;

/**
 * Helper class for getting values from annotations.
 */
public abstract class Util {

    /**
     * Get a boolean value from an annotation.
     * @param annotation The annotation.
     * @param name The name of the attribute.
     * @param clazz The annotation class.
     * @return The boolean value.
     */
    public static boolean getBooleanValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            return Boolean.valueOf(obj.toString());
        }
        try {
            return (Boolean) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return true;
        }
    }

    public static int getIntValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            if ( obj instanceof String ) {
                return Integer.valueOf((String)obj);
            } else if ( obj instanceof Number ) {
                return ((Number)obj).intValue();
            }
            return Integer.valueOf(obj.toString());
        }
        try {
            return (Integer) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return 0;
        }
    }

    /**
     * Helper method to get the values of an annotation as string values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of string values or null.
     */
    public static String[] getStringValues(Annotation annotation, JavaClassDescription desc, String name)
    {
        final String[] sValues = getAnnotationValues(annotation, name, desc);
        return sValues;
    }

    /**
     * Helper method to get the values of an annotation as long values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of long values or null.
     */
    public static long[] getLongValues(Annotation annotation, JavaClassDescription desc, String name)
    {
        final String[] sValues = getAnnotationValues(annotation, name, desc);
        if ( sValues != null && sValues.length > 0 )
        {
            final long[] values = new long[sValues.length];
            for (int i=0; i<values.length; i++)
            {
                values[i] = Long.valueOf(sValues[i]);
            }
            return values;

        }
        return null;
    }

    /**
     * Helper method to get the values of an annotation as long values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of long values or null.
     */
    public static int[] getIntValues(Annotation annotation, JavaClassDescription desc, String name)
    {
        final String[] sValues = getAnnotationValues(annotation, name, desc);
        if ( sValues != null && sValues.length > 0 )
        {
            final int[] values = new int[sValues.length];
            for (int i=0; i<values.length; i++)
            {
                values[i] = Integer.valueOf(sValues[i]);
            }
            return values;

        }
        return null;
    }

    /**
     * Helper method to get the values of an annotation as float values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of float values or null.
     */
    public static float[] getFloatValues(Annotation annotation, JavaClassDescription desc, String name)
    {
        final String[] sValues = getAnnotationValues(annotation, name, desc);
        if ( sValues != null && sValues.length > 0 )
        {
            final float[] values = new float[sValues.length];
            for (int i=0; i<values.length; i++)
            {
                values[i] = Float.valueOf(sValues[i]);
            }
            return values;

        }
        return null;
    }

    /**
     * Helper method to get the values of an annotation as double values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of double values or null.
     */
    public static double[] getDoubleValues(Annotation annotation, JavaClassDescription desc, String name)
    {
        final String[] sValues = getAnnotationValues(annotation, name, desc);
        if ( sValues != null && sValues.length > 0 )
        {
            final double[] values = new double[sValues.length];
            for (int i=0; i<values.length; i++)
            {
                values[i] = Double.valueOf(sValues[i]);
            }
            return values;

        }
        return null;
    }

    /**
     * Helper method to get the values of an annotation as char values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of char values or null.
     */
    public static char[] getCharValues(Annotation annotation, JavaClassDescription desc, String name) {
        final String[] sValues = getAnnotationValues(annotation, name, desc);
        if ( sValues != null && sValues.length > 0 )
        {
            final char[] values = new char[sValues.length];
            for (int i=0; i<values.length; i++)
            {
                values[i] = sValues[i].charAt(0);
            }
            return values;

        }
        return null;
    }

    /**
     * Helper method to get the values of an annotation as short values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of short values or null.
     */
   public static short[] getShortValues(Annotation annotation, JavaClassDescription desc, String name)
   {
       final String[] sValues = getAnnotationValues(annotation, name, desc);
       if ( sValues != null && sValues.length > 0 )
       {
           final short[] values = new short[sValues.length];
           for (int i=0; i<values.length; i++)
           {
               values[i] = Short.valueOf(sValues[i]);
           }
           return values;

       }
       return null;
    }

    /**
     * Helper method to get the values of an annotation as byte values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of byte values or null.
     */
   public static byte[] getByteValues(Annotation annotation, JavaClassDescription desc, String name)
   {
       final String[] sValues = getAnnotationValues(annotation, name, desc);
       if ( sValues != null && sValues.length > 0 )
       {
           final byte[] values = new byte[sValues.length];
           for (int i=0; i<values.length; i++)
           {
               values[i] = Byte.valueOf(sValues[i]);
           }
           return values;

       }
       return null;
    }

    /**
     * Helper method to get the values of an annotation as boolean values.
     * @param annotation The annotation.
     * @param desc The java class description.
     * @param name The attribute name from the annotation.
     * @return The array of boolean values or null.
     */
    public static boolean[] getBooleanValues(Annotation annotation, JavaClassDescription desc, String name)
    {
        final String[] sValues = getAnnotationValues(annotation, name, desc);
        if ( sValues != null && sValues.length > 0 )
        {
            final boolean[] values = new boolean[sValues.length];
            for (int i=0; i<values.length; i++)
            {
                values[i] = Boolean.valueOf(sValues[i]);
            }
            return values;

        }
        return null;
    }

    /**
     * Get a single annotation value
     * @param annotation The annotation
     * @param desc The class description
     * @param name The name of the annotation
     * @param clazz The class of the annotation
     * @return The value
     */
    public static String getStringValue(Annotation annotation, JavaClassDescription desc, String name, final Class<?> clazz) {
        final String values[] = getAnnotationValues(annotation, name, desc);
        if ( values != null && values.length > 0 ) {
            return values[0];
        }
        // try to get the default value
        try {
            return (String) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return "";
        }
    }

    public static Class<?> getClassValue(Annotation annotation, String name, final Class<?> clazz) {
        final Object obj = annotation.getNamedParameter(name);
        if ( obj != null ) {
            if ( obj instanceof Class<?> ) {
                return (Class<?>)obj;
            }
            return ClassUtil.getClass(obj.toString());
        }
        try {
            return (Class<?>) clazz.getMethod(name).getDefaultValue();
        } catch( NoSuchMethodException mnfe) {
            // we ignore this
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum> T getEnumValue(Annotation annotation,
                                                  String name,
                                                  final Class<T> enumClass,
                                                  final Class<?> clazz,
                                                  final boolean returnDefault) {
        Object obj = annotation.getNamedParameter(name);
        if (obj == null) {
            if ( returnDefault ) {
                try {
                    obj = clazz.getMethod(name).getDefaultValue();
                } catch( NoSuchMethodException mnfe) {
                    // we ignore this
                }
            }
        }
        if ( obj != null ) {
            if (enumClass.isAssignableFrom(obj.getClass())) {
                return (T)obj;
            }
            else if ( obj instanceof String ) {
                String enumName = (String)obj;
                int dotPos = enumName.lastIndexOf('.');
                if (dotPos >= 0) {
                    enumName = enumName.substring(dotPos+1);
                }
                Object o = Enum.valueOf(enumClass, enumName);
                return (T)o;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum> T getEnumValue(Annotation annotation,
            String name,
            final Class<T> enumClass,
            final Class<?> clazz) {
        return getEnumValue(annotation, name, enumClass, clazz, true);
    }

    public static String[] getAnnotationValues(final Annotation annotation, final String name, final JavaClassDescription desc)
    throws IllegalArgumentException
    {

        EvaluatingVisitor evaluatingVisitor = new EvaluatingVisitor() {

            public Object visitAnnotationFieldRef( AnnotationFieldRef fieldRef ) {
                // during prescan of AnnotationTagProviderManager#hasScrPluginAnnotation this method is called without desc attribute
                // avoid NPE in this case and just skip value resolving
                // FELIX-1629
                if ( desc == null)
                {
                    return "";
                }

                // getField throws AIOOBE
                // return ((AnnotationFieldRef)av).getField().getInitializationExpression();
                final String s = fieldRef.getParameterValue().toString().trim();
                try
                {
                    int classSep = s.lastIndexOf('.');
                    JavaField field = null;
                    if ( classSep == -1 ) {
                        // local variable
                        field = desc.getFieldByName(s);
                    }
                    if ( field == null ) {
                        field = desc.getExternalFieldByName(s);
                    }
                    if ( field == null ) {
                        throw new IllegalArgumentException("Property references unknown field " + s + " in class " + desc.getName());
                    }
                    String[] values = field.getInitializationExpression();
                    if ( values != null && values.length == 1 ) {
                        return values[0];
                    }
                    throw new IllegalArgumentException("Something is wrong: " + s);
                }
                catch (NoClassDefFoundError ncdfe)
                {
                    throw new IllegalArgumentException("A class could not be found while parsing class " + desc.getName() +
                            ". Please check this stracktrace and add a dependency with the missing class to your project.", ncdfe);
                }
                catch (SCRDescriptorException mee)
                {
                    throw new IllegalArgumentException(mee);
                }
            }

            @Override
            protected Object getFieldReferenceValue(com.thoughtworks.qdox.model.JavaField javaField) {
                // is never called because visitAnnotationFieldRef is overridden as well
                return null;
            }

        };
        @SuppressWarnings("unchecked")
        final List<Object> valueList = evaluatingVisitor.getListValue(annotation, name);
        if (valueList==null) {
            return null;
        }
        String[] values = new String[valueList.size()];
        for (int i=0; i<values.length; i++) {
            Object value = valueList.get(i);
            if (value!=null) {
                values[i] = value.toString();
            }
        }
        return values;
    }
}
