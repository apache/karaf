package org.apache.karaf.event.command;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.event.command.EventSendCommand;
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
        send.properties = Arrays.asList("a=b");
        send.execute();
        verify(send.eventAdmin);
        
        Event event = eventCapture.getValue();
        assertThat(event.getTopic(), equalTo("myTopic"));
        assertThat(event.getProperty("a"), equalTo("b"));
    }
    
    @Test
    public void testParse() {
        List<String> propList = Arrays.asList("a=b","b=c");
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("a", "b");
        expectedMap.put("b", "c");
        Map<String, String> props = EventSendCommand.parse(propList);
        assertThat(props.size(), equalTo(2));
        assertThat(props.get("a"), equalTo("b"));
        assertThat(props.get("b"), equalTo("c"));
    }
    
    @Test
    public void testParseNull() {
        Map<String, String> props = EventSendCommand.parse(null);
        assertNotNull(props);
        assertThat(props.size(), equalTo(0));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testParseNoKeyValue() {
        EventSendCommand.parse(Arrays.asList("="));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testParseNoKey() {
        EventSendCommand.parse(Arrays.asList("=b"));
    }
    
    @Test
    public void testParseStrange() {
        Map<String, String> props = EventSendCommand.parse(Arrays.asList("a=b","c=d=3", "e="));
        assertThat(props.size(), equalTo(3));
        assertThat(props.get("a"), equalTo("b"));
        assertThat(props.get("c"), equalTo("d=3"));
        assertThat(props.get("e"), equalTo(""));
    }
}
