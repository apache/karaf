package org.apache.felix.dm.annotation.plugin.bnd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patterns
{
    // Pattern used to check if a method is void and does not take any params
    public final static Pattern VOID = Pattern.compile("\\(\\)V");

    // Pattern used to check if a method returns an array of Objects
    public final static Pattern COMPOSITION = Pattern.compile("\\(\\)\\[Ljava/lang/Object;");

   // Pattern used to parse the class parameter from the bind methods ("bind(Type)" or "bind(Map, Type)" or "bind(BundleContext, Type)"
    public final static Pattern BIND_CLASS = Pattern.compile("\\((L[^;]+;)?L([^;]+);\\)V");

    // Pattern used to parse classes from class descriptors;
    public final static Pattern CLASS = Pattern.compile("L([^;]+);");

    /**
     * Parses a class.
     * @param clazz the class to be parsed (the package is "/" separated).
     * @param pattern the pattern used to match the class.
     * @param group the pattern group index where the class can be retrieved.
     * @return the parsed class.
     */
    public static String parseClass(String clazz, Pattern pattern, int group)
    {
        Matcher matcher = pattern.matcher(clazz);
        if (matcher.matches())
        {
            return matcher.group(group).replace("/", ".");
        }
        else
        {
            throw new IllegalArgumentException("Invalid class descriptor: " + clazz);
        }
    }
    
    /**
     * Checks if a method descriptor matches a given pattern. 
     * @param pattern the pattern used to check the method signature descriptor
     * @throws IllegalArgumentException if the method signature descriptor does not match the given pattern.
     */
    public static void parseMethod(String method, String descriptor, Pattern pattern)
    {
        Matcher matcher = pattern.matcher(descriptor);
        if (!matcher.matches())
        {
            throw new IllegalArgumentException("Invalid method " + method + ", wrong signature: "
                + descriptor);
        }
    }
}
