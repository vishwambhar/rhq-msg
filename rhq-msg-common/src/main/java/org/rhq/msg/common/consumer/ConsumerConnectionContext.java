package org.rhq.msg.common.consumer;

import javax.jms.MessageConsumer;

import org.rhq.msg.common.ConnectionContext;

public class ConsumerConnectionContext extends ConnectionContext {
    private MessageConsumer consumer;

    public MessageConsumer getMessageConsumer() {
        return consumer;
    }

    public void setMessageConsumer(MessageConsumer consumer) {
        this.consumer = consumer;
    }
}
