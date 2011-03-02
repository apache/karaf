package org.apache.karaf.shell.console.table;

public class StringUtil {

    /**
     * Returns length of the string.
     * 
     * @param string String.
     * @return Length.
     */
    public static int length(String string) {
        return string == null ? 0 : string.length();
    }

    /**
     * Utility method to repeat string.
     * 
     * @param string String to repeat.
     * @param times Number of times.
     * @return Repeat string.
     */
    public static String repeat(String string, int times) {
        if (times <= 0) {
            return "";
        }
        else if (times % 2 == 0) {
            return repeat(string+string, times/2);
        }
        else {
           return string + repeat(string+string, times/2);
        }
    }
}
