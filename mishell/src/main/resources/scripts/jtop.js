/*
 * This code is "ported" from JTop demo. This file defines
 * 'jtop' function. jtop prints threads sorting by CPU time. 
 * jtop can be called once or periodically from a timer thread. 
 * To call this once, just call 'jtop()' in script console prompt. 
 * To call jtop in a timer thread, you can use
 *
 *     var t = setTimeout(function () { jtop(print); }, 2000); 
 *
 * The above call prints threads in sorted order for every 2 seconds.
 * The print output goes to OS console window from which jconsole was 
 * started. The timer can be cancelled later by clearTimeout() function
 * as shown below:
 * 
 *     clearTimeout(t);    
 */

/**
 * This function returns a List of Map.Entry objects
 * in which each entry maps cpu time to ThreadInfo.
 */
function getThreadList() {
    var tmbean = newPlatformMXBeanProxy(
        "java.lang:type=Threading",
        java.lang.management.ThreadMXBean);

    if (!tmbean.isThreadCpuTimeSupported()) {
        return;
    }

    tmbean.setThreadCpuTimeEnabled(true);

    var tids = tmbean.allThreadIds;
    var tinfos = tmbean["getThreadInfo(long[])"](tids);

    var map = new java.util.TreeMap();
    for (var i in tids) {
        var cpuTime = tmbean.getThreadCpuTime(tids[i]);
        if (cpuTime != -1 && tinfos[i] != null) {
            map.put(cpuTime, tinfos[i]);
        }
    }
    var list = new java.util.ArrayList(map.entrySet());
    java.util.Collections.reverse(list);
    return list;
}

/**
 * This function prints threads sorted by CPU time.
 *
 * @param printFunc function called back to print [optional]
 *
 * By default, it uses 'echo' function to print in screen.
 * Other choices could be 'print' (prints in console), 'alert'
 * to show message box. Or you can define a function that writes
 * the output to a text file.
 */ 
function jtop(printFunc) {
    if (printFunc == undefined) {
        printFunc = echo;
    }
    var list = getThreadList();
    var itr = list.iterator();
    printFunc("time - state - name");
    while (itr.hasNext()) {
        var entry = itr.next();
        // time is in nanoseconds - convert to seconds
        var time = entry.key / 1.0e9;
        var name = entry.value.threadName;
        var state = entry.value.threadState;
        printFunc(time + " - " + state + " - " + name); 
    }
}
