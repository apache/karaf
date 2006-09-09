/*
 * This script demonstrates "invokeMBean" function. Instead 
 * of using MXBean proxy or script wrapper object returned by
 * 'mbean' function, this file uses direct invoke on MBean.
 *
 * To use this particular script, load this script file in
 * script console prompt and call resetPeakThreadCount().

 */

/**
 * Resets the peak thread count to the current number of live threads.
 *
 */
function resetPeakThreadCount() {
    return invokeMBean("java.lang:type=Threading", "resetPeakThreadCount", [], "");    
}

