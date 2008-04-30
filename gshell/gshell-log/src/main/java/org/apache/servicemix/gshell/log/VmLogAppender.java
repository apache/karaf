package org.apache.servicemix.gshell.log;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 28, 2008
 * Time: 5:13:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class VmLogAppender implements PaxAppender {

    protected LruList<PaxLoggingEvent> events;

    public LruList<PaxLoggingEvent> getEvents() {
        return events;
    }

    public void setEvents(LruList<PaxLoggingEvent> events) {
        this.events = events;
    }

    public void doAppend(PaxLoggingEvent event) {
        if (events != null) {
            events.add(event);
        }
    }

}
