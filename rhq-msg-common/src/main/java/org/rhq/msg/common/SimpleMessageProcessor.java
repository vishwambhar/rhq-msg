package org.rhq.msg.common;

import java.util.Map;
import java.util.concurrent.Future;

import javax.jms.JMSException;
import javax.jms.Message;

import org.rhq.msg.common.consumer.AbstractBasicMessageListener;
import org.rhq.msg.common.consumer.BasicMessageListener;
import org.rhq.msg.common.consumer.ConsumerConnectionContext;
import org.rhq.msg.common.consumer.RPCConnectionContext;
import org.rhq.msg.common.producer.ProducerConnectionContext;

/**
 * A version of the MessageProcessor that keeps the passed interfaces internally available
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class SimpleMessageProcessor extends MessageProcessor {

    private ConsumerConnectionContext consumerCtx;
    private ProducerConnectionContext producerCtx;

    public SimpleMessageProcessor(ConsumerConnectionContext consumerCtx, ProducerConnectionContext producerCtx) {
        this.consumerCtx = consumerCtx;
        this.producerCtx = producerCtx;
    }

    public <T extends BasicMessage> void listen(
                                                AbstractBasicMessageListener<T> listener) throws JMSException {
        super.listen(consumerCtx, listener);
    }

    public MessageId send(BasicMessage basicMessage) throws JMSException {
        return super.send(producerCtx, basicMessage, null);
    }

    public MessageId send(BasicMessage basicMessage, Map<String,String> headers) throws JMSException {
        return super.send(producerCtx, basicMessage,headers);
    }


    public <T extends BasicMessage> RPCConnectionContext sendAndListen(
                                                                       BasicMessage basicMessage,
                                                                       BasicMessageListener<T> responseListener) throws JMSException {
        return super.sendAndListen(producerCtx, basicMessage, responseListener);
    }

    public <T extends BasicMessage> RPCConnectionContext sendAndListen(
                                                                       BasicMessage basicMessage,
                                                                       BasicMessageListener<T> responseListener,
                                                                       Map<String,String> headers) throws JMSException {
        return super.sendAndListen(producerCtx, basicMessage, responseListener,headers);
    }

    public <R extends BasicMessage> Future<R> sendRPC(BasicMessage basicMessage,
                                                      Class<R> expectedResponseMessageClass) throws JMSException {
        return super.sendRPC(producerCtx, basicMessage,
            expectedResponseMessageClass);
    }

    public <R extends BasicMessage> Future<R> sendRPC(BasicMessage basicMessage,
                                                      Class<R> expectedResponseMessageClass,
                                                      Map<String,String> headers) throws JMSException {
        return super.sendRPC(producerCtx, basicMessage,
            expectedResponseMessageClass, headers);
    }

    protected Message createMessage(ConnectionContext context, BasicMessage basicMessage) throws JMSException {
        return super.createMessage(context, basicMessage);
    }
}
