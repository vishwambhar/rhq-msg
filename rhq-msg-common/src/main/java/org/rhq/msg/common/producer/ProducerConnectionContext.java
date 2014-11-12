package org.rhq.msg.common.producer;

import javax.jms.MessageProducer;

import org.rhq.msg.common.ConnectionContext;

public class ProducerConnectionContext extends ConnectionContext {
    private MessageProducer producer;

    public MessageProducer getMessageProducer() {
        return producer;
    }

    public void setMessageProducer(MessageProducer producer) {
        this.producer = producer;
    }
}
