package org.apache.karaf.event;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class EventSendCommandTest {
    @Test
    public void testExecute() throws Exception {
        EventSendCommand send = new EventSendCommand();
        send.eventAdmin = mock(EventAdmin.class);
        Capture<Event> eventCapture = newCapture();
        send.eventAdmin.sendEvent(capture(eventCapture));
        expectLastCall();

        replay(send.eventAdmin);
        send.topic = "myTopic";
        send.propertiesSt = "a=b";
        send.execute();
        verify(send.eventAdmin);
        
        Event event = eventCapture.getValue();
        assertThat(event.getTopic(), equalTo("myTopic"));
        assertThat(event.getProperty("a"), equalTo("b"));
    }
    
    @Test
    public void testParse() {
        String propSt = "a=b,b=c";
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("a", "b");
        expectedMap.put("b", "c");
        Map<String, String> props = new EventSendCommand().parse(propSt);
        assertThat(props.entrySet(), equalTo(expectedMap.entrySet())); 
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testParseError() {
        String propSt = "a=b,c=";
        new EventSendCommand().parse(propSt);
    }
}
