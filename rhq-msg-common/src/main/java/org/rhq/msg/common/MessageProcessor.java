package org.rhq.msg.common;

import java.util.concurrent.Future;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.rhq.msg.common.consumer.AbstractBasicMessageListener;
import org.rhq.msg.common.consumer.BasicMessageListener;
import org.rhq.msg.common.consumer.ConsumerConnectionContext;
import org.rhq.msg.common.consumer.FutureBasicMessageListener;
import org.rhq.msg.common.consumer.RPCConnectionContext;
import org.rhq.msg.common.producer.ProducerConnectionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides some functionality to process messages, both as a producer or consumer.
 * 
 * Use {@link ConnectionContextFactory} to create contexts (which create destinations, sessions, and connections for you)
 * that you then use to pass to the listen and send methods in this class.
 */
public class MessageProcessor {

    private final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    /**
     * Listens for messages.
     * 
     * @param context
     *            information that determines where to listen
     * @param listener
     *            the listener that processes the incoming messages
     * @throws JMSException
     * 
     * @see {@link #createConsumerConnectionContext(Endpoint)}
     */
    public <T extends BasicMessage> void listen(ConsumerConnectionContext context, AbstractBasicMessageListener<T> listener) throws JMSException {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        if (listener == null) {
            throw new NullPointerException("listener must not be null");
        }

        MessageConsumer consumer = context.getMessageConsumer();
        if (consumer == null) {
            throw new NullPointerException("context had a null consumer");
        }

        listener.setConsumerConnectionContext(context);
        consumer.setMessageListener(listener);
    }

    /**
     * Send the given message to its destinations across the message bus. Once sent, the message will get assigned a
     * generated message ID. That message ID will also be returned by this method.
     * 
     * Since this is fire-and-forget - no response is expected of the remote endpoint.
     * 
     * @param context
     *            information that determines where the message is sent
     * @param basicMessage
     *            the message to send
     * @return the message ID
     * @throws JMSException
     * 
     * @see {@link #createProducerConnectionContext(Endpoint)}
     */
    public MessageId send(ProducerConnectionContext context, BasicMessage basicMessage) throws JMSException {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        if (basicMessage == null) {
            throw new NullPointerException("message must not be null");
        }

        // create the JMS message to be sent
        Message msg = createMessage(context, basicMessage);

        // if the message is correlated with another, put the correlation ID in the Message to be sent
        if (basicMessage.getCorrelationId() != null) {
            msg.setJMSCorrelationID(basicMessage.getCorrelationId().toString());
        }

        if (basicMessage.getMessageId() != null) {
            log.debug("Non-null message ID [{}] will be ignored and a new one generated", basicMessage.getMessageId());
            basicMessage.setMessageId(null);
        }

        // now send the message to the broker
        MessageProducer producer = context.getMessageProducer();
        if (producer == null) {
            throw new NullPointerException("context had a null producer");
        }

        producer.send(msg);

        // put message ID into the message in case the caller wants to correlate it with another record
        MessageId messageId = new MessageId(msg.getJMSMessageID());
        basicMessage.setMessageId(messageId);

        return messageId;
    }

    /**
     * Send the given message to its destinations across the message bus and any response sent back will be passed to the
     * given listener. Use this for request-response messages where you expect to get a non-void response back.
     * 
     * The response listener should close its associated consumer since typically there is only a single response that is
     * expected. This is left to the listener to do in case there are special circumstances where the listener does
     * expect multiple response messages.
     * 
     * If the caller merely wants to wait for a single response and obtain the response message to process it further,
     * consider using instead the method {@link #sendRPC(ProducerConnectionContext, BasicMessage)} and use its returned
     * Future to wait for the response, rather than having to supply your own response listener.
     * 
     * @param context
     *            information that determines where the message is sent
     * @param basicMessage
     *            the request message to send
     * @param responseListener
     *            The listener that will process the response of the request. This listener should close its associated
     *            consumer when appropriate.
     * 
     * @param T
     *            the expected basic message type that will be received as the response to the request
     * 
     * @return the RPC context which includes information about the handling of the expected response
     * @throws JMSException
     * 
     * @see {@link #createProducerConnectionContext(Endpoint)}
     */
    public <T extends BasicMessage> RPCConnectionContext sendAndListen(ProducerConnectionContext context, BasicMessage basicMessage,
            BasicMessageListener<T> responseListener) throws JMSException {

        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        if (basicMessage == null) {
            throw new NullPointerException("message must not be null");
        }
        if (responseListener == null) {
            throw new NullPointerException("response listener must not be null");
        }

        // create the JMS message to be sent
        Message msg = createMessage(context, basicMessage);

        // if the message is correlated with another, put the correlation ID in the Message to be sent
        if (basicMessage.getCorrelationId() != null) {
            msg.setJMSCorrelationID(basicMessage.getCorrelationId().toString());
        }

        if (basicMessage.getMessageId() != null) {
            log.debug("Non-null message ID [{}] will be ignored and a new one generated", basicMessage.getMessageId());
            basicMessage.setMessageId(null);
        }

        MessageProducer producer = context.getMessageProducer();
        if (producer == null) {
            throw new NullPointerException("Cannot send request-response message - the producer is null");
        }

        // prepare for the response prior to sending the request
        Session session = context.getSession();
        if (session == null) {
            throw new NullPointerException("Cannot send request-response message - the session is null");
        }
        TemporaryQueue responseQueue = session.createTemporaryQueue();
        MessageConsumer responseConsumer = session.createConsumer(responseQueue);

        RPCConnectionContext rpcContext = new RPCConnectionContext();
        rpcContext.copy(context);
        rpcContext.setDestination(responseQueue);
        rpcContext.setMessageConsumer(responseConsumer);
        rpcContext.setRequestMessage(msg);
        rpcContext.setResponseListener(responseListener);

        responseListener.setConsumerConnectionContext(rpcContext);
        responseConsumer.setMessageListener(responseListener);

        msg.setJMSReplyTo(responseQueue);

        // now send the message to the broker
        producer.send(msg);

        // put message ID into the message in case the caller wants to correlate it with another record
        MessageId messageId = new MessageId(msg.getJMSMessageID());
        basicMessage.setMessageId(messageId);

        return rpcContext;
    }

    /**
     * Send the given message to its destinations across the message bus and returns a Future to allow the caller to
     * retrieve the response.
     * 
     * This is intended to mimic an RPC-like request-response workflow. It is assumed the request will trigger a single
     * response message to be sent back. This method returns a Future that will provide you with the response message
     * that is received back.
     * 
     * @param context
     *            information that determines where the message is sent
     * @param basicMessage
     *            the request message to send
     * @param expectedResponseMessageClass
     *            this is the message class of the expected response object.
     * 
     * @param R
     *            the expected basic message type that will be received as the response to the request
     * 
     * @return a future that allows you to wait for and get the response of the given response type
     * @throws JMSException
     * 
     * @see {@link #createProducerConnectionContext(Endpoint)}
     */
    public <R extends BasicMessage> Future<R> sendRPC(ProducerConnectionContext context, BasicMessage basicMessage, Class<R> expectedResponseMessageClass)
            throws JMSException {

        FutureBasicMessageListener<R> futureListener = new FutureBasicMessageListener<R>(expectedResponseMessageClass);
        sendAndListen(context, basicMessage, futureListener);
        return futureListener;
    }

    /**
     * Creates a text message that can be send via a producer that contains the given BasicMessage's JSON encoded data.
     * 
     * @param context
     *            the context whose session is used to create the message
     * @param basicMessage
     *            contains the data that will be JSON-encoded and encapsulated in the created message
     * @return the message that can be produced
     * @throws JMSException
     * @throws NullPointerException
     *             if the context is null or the context's session is null
     */
    protected Message createMessage(ConnectionContext context, BasicMessage basicMessage) throws JMSException {
        if (context == null) {
            throw new NullPointerException("The context is null");
        }
        Session session = context.getSession();
        if (session == null) {
            throw new NullPointerException("The context had a null session");
        }
        Message msg = session.createTextMessage(basicMessage.toJSON());
        return msg;
    }
}
