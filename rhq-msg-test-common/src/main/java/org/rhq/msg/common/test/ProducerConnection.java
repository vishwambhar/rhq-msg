package org.rhq.msg.common.test;

import javax.jms.JMSException;
import javax.jms.Message;

import org.rhq.msg.common.ConnectionContextFactory;
import org.rhq.msg.common.Endpoint;
import org.rhq.msg.common.producer.ProducerConnectionContext;

/**
 * Convenience class tests can use to create a producer of either topic or queue
 * messages.
 * 
 * The constructor creates the connection after which you just call sendMessage
 * to produce a message.
 */
public class ProducerConnection extends ConnectionContextFactory {

    private ProducerConnectionContext pcc;

    public ProducerConnection(String brokerURL, Endpoint endpoint) throws JMSException {
        super(brokerURL);
        prepareProducer(brokerURL, endpoint);
    }

    protected void prepareProducer(String brokerURL, Endpoint endpoint) throws JMSException {
        pcc = new ProducerConnectionContext();
        createConnection(pcc);
        setConnection(pcc.getConnection());
        getConnection().start();
        createSession(pcc);
        createDestination(pcc, endpoint);
        pcc.setMessageProducer(pcc.getSession().createProducer(pcc.getDestination()));
    }

    public ProducerConnectionContext getConsumerConnectionContext() {
        return pcc;
    }

    public void sendMessage(String msg) throws JMSException {
        Message producerMessage = pcc.getSession().createTextMessage(msg);
        pcc.getMessageProducer().send(producerMessage);

    }
}
