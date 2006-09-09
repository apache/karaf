/*
 * This script demonstrates "getMBeanAttribute"
 * and "setMBeanAttribute" functions. Instead of using
 * MXBean proxy or script wrapper object returned by
 * 'mbean' function, this file uses direct get/set MBean
 * attribute functions.
 *
 * To use this particular script, load this script file in
 * script console prompt and call verboseGC or verboseClass
 * functions. These functions based on events such as 
 * heap threshold crossing a given limit. i.e., A timer thread
 * can keep checking for threshold event and then turn on
 * verbose:gc or verbose:class based on expected event.

 */

/**
 * Get or set verbose GC flag.
 *
 * @param flag verbose mode flag [optional]
 *
 * If flag is not passed verboseGC returns current
 * flag value.
 */
function verboseGC(flag) {
    if (flag == undefined) {
        // no argument passed. interpret this as 'get'
        return getMBeanAttribute("java.lang:type=Memory", "Verbose");    
    } else {
        return setMBeanAttribute("java.lang:type=Memory", "Verbose", flag);
    }
}

/**
 * Get or set verbose class flag.
 *
 * @param flag verbose mode flag [optional]
 *
 * If flag is not passed verboseClass returns current
 * flag value.
 */
function verboseClass(flag) {
    if (flag == undefined) {
        // no argument passed. interpret this as 'get'
        return getMBeanAttribute("java.lang:type=ClassLoading", "Verbose");    
    } else {
        return setMBeanAttribute("java.lang:type=ClassLoading", "Verbose", flag);
    }
}
