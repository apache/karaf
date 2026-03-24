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
package org.apache.karaf.jms;

import javax.jms.*;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Describe a JMS message is more human readable way.
 */
public class JmsMessage {

    private Map<String, Object> properties = new HashMap<>();

    private String content;
    private String charset = "UTF-8";
    private String correlationID;
    private String deliveryMode;
    private String destination;
    private String expiration;
    private String messageId;
    private int priority;
    private boolean redelivered;
    private String replyTo;
    private String timestamp;
    private String type;

    public JmsMessage(Message message) {
        try {
            initFromMessage(message);
        } catch (JMSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void initFromMessage(Message message) throws JMSException {
        @SuppressWarnings("unchecked")
        Enumeration<String> names = message.getPropertyNames();
        while (names.hasMoreElements()) {
            String key = names.nextElement();
            Object value = message.getObjectProperty(key);
            properties.put(key, value);
        }

        correlationID = message.getJMSCorrelationID();
        if (message.getJMSDeliveryMode() == DeliveryMode.NON_PERSISTENT) {
            deliveryMode = "Non Persistent";
        } else {
            deliveryMode = "Persistent";
        }
        Destination destinationDest = message.getJMSDestination();
        if (destinationDest != null) {
            destination = destinationDest.toString();
        }
        if (message.getJMSExpiration() > 0) {
            expiration = new Date(message.getJMSExpiration()).toString();
        } else {
            expiration = "Never";
        }
        messageId = message.getJMSMessageID();
        priority = message.getJMSPriority();
        redelivered = message.getJMSRedelivered();
        Destination replyToDest = message.getJMSReplyTo();
        if (replyToDest != null) {
            replyTo = replyToDest.toString();
        }
        if (message.getJMSTimestamp() > 0) {
            timestamp = new Date(message.getJMSTimestamp()).toString();
        } else {
            timestamp = "";
        }
        type = message.getJMSType();
        content = getMessageContent(message);
    }


    private String getMessageContent(Message message) throws JMSException {
        if (message instanceof TextMessage) {
            return ((TextMessage) message).getText();
        } else if (message instanceof BytesMessage) {
            BytesMessage bMessage = (BytesMessage) message;
            long length = bMessage.getBodyLength();
            byte[] content = new byte[(int) length];
            bMessage.readBytes(content);
            try {
                return new String(content, charset);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return "";
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getContent() {
        return content;
    }

    public String getCharset() {
        return charset;
    }

    public String getCorrelationID() {
        return correlationID;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public String getDestination() {
        return destination;
    }

    public String getExpiration() {
        return expiration;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isRedelivered() {
        return redelivered;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

}