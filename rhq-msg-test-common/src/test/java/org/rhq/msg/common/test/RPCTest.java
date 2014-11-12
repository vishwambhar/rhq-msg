package org.rhq.msg.common.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.rhq.msg.common.ConnectionContextFactory;
import org.rhq.msg.common.Endpoint;
import org.rhq.msg.common.Endpoint.Type;
import org.rhq.msg.common.MessageProcessor;
import org.rhq.msg.common.consumer.ConsumerConnectionContext;
import org.rhq.msg.common.consumer.RPCBasicMessageListener;
import org.rhq.msg.common.producer.ProducerConnectionContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests request-response messaging.
 */
@Test
public class RPCTest {
    public void testSendRPC() throws Exception {
        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            // mimic the server-side - this will receive the initial request message (and will send the response back)
            ConnectionContextFactory consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            TestRPCListener requestListener = new TestRPCListener();
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, requestListener);

            // mimic the client side - this will send the initial request message and receive the response from the server
            ConnectionContextFactory producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();
            Future<SpecificMessage> future = clientSideProcessor.sendRPC(producerContext, specificMessage, SpecificMessage.class);

            // wait for the message to flow
            SpecificMessage receivedSpecificMessage = null;
            try {
                receivedSpecificMessage = future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                assert false : "Future failed to obtain response message";
            }

            // close everything
            producerFactory.close();
            consumerFactory.close();

            // make sure the message flowed properly
            Assert.assertFalse(future.isCancelled());
            Assert.assertTrue(future.isDone());
            Assert.assertNotNull(receivedSpecificMessage, "Didn't receive response");
            Assert.assertEquals(receivedSpecificMessage.getMessage(), "RESPONSE:" + specificMessage.getMessage());
            Assert.assertEquals(receivedSpecificMessage.getDetails(), specificMessage.getDetails());
            Assert.assertEquals(receivedSpecificMessage.getSpecific(), "RESPONSE:" + specificMessage.getSpecific());
        } finally {
            broker.stop();
        }
    }

    public void testSendAndListen() throws Exception {
        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            // mimic the server-side - this will receive the initial request message (and will send the response back)
            ConnectionContextFactory consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            TestRPCListener requestListener = new TestRPCListener();
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, requestListener);

            // mimic the client side - this will send the initial request message and receive the response from the
            // server
            ConnectionContextFactory producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            CountDownLatch latch = new CountDownLatch(1);
            ArrayList<SpecificMessage> receivedMessages = new ArrayList<SpecificMessage>();
            ArrayList<String> errors = new ArrayList<String>();
            SpecificMessageStoreAndLatchListener responseListener = new SpecificMessageStoreAndLatchListener(latch, receivedMessages, errors);
            MessageProcessor clientSideProcessor = new MessageProcessor();
            clientSideProcessor.sendAndListen(producerContext, specificMessage, responseListener);

            // wait for the message to flow
            boolean gotMessage = latch.await(5, TimeUnit.SECONDS);
            if (!gotMessage) {
                errors.add("Timed out waiting for response message - it never showed up");
            }

            // close everything
            producerFactory.close();
            consumerFactory.close();

            // make sure the message flowed properly
            Assert.assertTrue(errors.isEmpty(), "Failed to send message properly: " + errors);
            Assert.assertEquals(receivedMessages.size(), 1, "Didn't receive response: " + receivedMessages);
            SpecificMessage receivedSpecificMessage = receivedMessages.get(0);
            Assert.assertEquals(receivedSpecificMessage.getMessage(), "RESPONSE:" + specificMessage.getMessage());
            Assert.assertEquals(receivedSpecificMessage.getDetails(), specificMessage.getDetails());
            Assert.assertEquals(receivedSpecificMessage.getSpecific(), "RESPONSE:" + specificMessage.getSpecific());
        } finally {
            broker.stop();
        }
    }

    private class TestRPCListener extends RPCBasicMessageListener<SpecificMessage, SpecificMessage> {
        @Override
        protected SpecificMessage onBasicMessage(SpecificMessage requestMessage) {
            SpecificMessage responseMessage = new SpecificMessage("RESPONSE:" + requestMessage.getMessage(), requestMessage.getDetails(), "RESPONSE:"
                    + requestMessage.getSpecific());
            return responseMessage;
        }

    }
}
