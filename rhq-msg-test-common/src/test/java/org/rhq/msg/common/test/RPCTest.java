package org.rhq.msg.common.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            // mimic server-side - this will receive the initial request message (and will send the response back)
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            TestRPCListener requestListener = new TestRPCListener();
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, requestListener);

            // mimic client side - this will send the initial request message and receive the response from the server
            producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();
            Future<SpecificMessage> future = clientSideProcessor.sendRPC(producerContext, specificMessage, SpecificMessage.class);

            // wait for the message to flow
            SpecificMessage receivedSpecificMessage = null;
            try {
                receivedSpecificMessage = future.get();
            } catch (Exception e) {
                assert false : "Future failed to obtain response message: " + e;
            }

            // make sure the message flowed properly
            Assert.assertFalse(future.isCancelled());
            Assert.assertTrue(future.isDone());
            Assert.assertNotNull(receivedSpecificMessage, "Didn't receive response");
            Assert.assertEquals(receivedSpecificMessage.getMessage(), "RESPONSE:" + specificMessage.getMessage());
            Assert.assertEquals(receivedSpecificMessage.getDetails(), specificMessage.getDetails());
            Assert.assertEquals(receivedSpecificMessage.getSpecific(), "RESPONSE:" + specificMessage.getSpecific());

            // use the future.get(timeout) method and make sure it returns the same
            try {
                receivedSpecificMessage = future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                assert false : "Future failed to obtain response message: " + e;
            }

            Assert.assertNotNull(receivedSpecificMessage, "Didn't receive response");
            Assert.assertEquals(receivedSpecificMessage.getMessage(), "RESPONSE:" + specificMessage.getMessage());
            Assert.assertEquals(receivedSpecificMessage.getDetails(), specificMessage.getDetails());
            Assert.assertEquals(receivedSpecificMessage.getSpecific(), "RESPONSE:" + specificMessage.getSpecific());

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }

    public void testSendAndListen() throws Exception {
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            // mimic server-side - this will receive the initial request message (and will send the response back)
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            TestRPCListener requestListener = new TestRPCListener();
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, requestListener);

            // mimic client side - this will send the initial request message and receive the response from the server
            producerFactory = new ConnectionContextFactory(brokerURL);
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

            // make sure the message flowed properly
            Assert.assertTrue(errors.isEmpty(), "Failed to send message properly: " + errors);
            Assert.assertEquals(receivedMessages.size(), 1, "Didn't receive response: " + receivedMessages);
            SpecificMessage receivedSpecificMessage = receivedMessages.get(0);
            Assert.assertEquals(receivedSpecificMessage.getMessage(), "RESPONSE:" + specificMessage.getMessage());
            Assert.assertEquals(receivedSpecificMessage.getDetails(), specificMessage.getDetails());
            Assert.assertEquals(receivedSpecificMessage.getSpecific(), "RESPONSE:" + specificMessage.getSpecific());

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }

    public void testRPCTimeout() throws Exception {
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            // mimic server-side - this will receive the initial request message (and will send the response back)
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            TestRPCListener requestListener = new TestRPCListener(4000L); // wait so we have a chance to timeout
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, requestListener);

            // mimic client side - this will send the initial request message and receive the response from the server
            producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();
            Future<SpecificMessage> future = clientSideProcessor.sendRPC(producerContext, specificMessage, SpecificMessage.class);

            // wait for the message to flow - notice we don't wait long enough - this should timeout
            SpecificMessage receivedSpecificMessage = null;
            try {
                receivedSpecificMessage = future.get(1, TimeUnit.SECONDS);
                assert false : "Future failed to timeout; should have not got a response: " + receivedSpecificMessage;
            } catch (TimeoutException expected) {
                // expected
            } catch (Exception e) {
                assert false : "Future threw unexpected exception: " + e;
            }

            Assert.assertFalse(future.isCancelled());
            Assert.assertFalse(future.isDone());

            // ok, now wait for the message to flow
            try {
                receivedSpecificMessage = future.get();
            } catch (Exception e) {
                assert false : "Future failed to obtain response message: " + e;
            }

            // make sure the message flowed properly
            Assert.assertFalse(future.isCancelled());
            Assert.assertTrue(future.isDone());
            Assert.assertNotNull(receivedSpecificMessage, "Didn't receive response");
            Assert.assertEquals(receivedSpecificMessage.getMessage(), "RESPONSE:" + specificMessage.getMessage());
            Assert.assertEquals(receivedSpecificMessage.getDetails(), specificMessage.getDetails());
            Assert.assertEquals(receivedSpecificMessage.getSpecific(), "RESPONSE:" + specificMessage.getSpecific());

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }

    public void testRPCCancel() throws Exception {
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            // mimic server-side - this will receive the initial request message (and will send the response back)
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            TestRPCListener requestListener = new TestRPCListener(3000L); // wait so we have a chance to cancel it
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, requestListener);

            // mimic client side - this will send the initial request message and receive the response from the server
            producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();
            Future<SpecificMessage> future = clientSideProcessor.sendRPC(producerContext, specificMessage, SpecificMessage.class);

            Assert.assertTrue(future.cancel(true), "Failed to cancel the future");
            Assert.assertTrue(future.isCancelled());
            Assert.assertTrue(future.isDone());

            // try to get the message using get(timeout) method
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (CancellationException expected) {
                // expected
            } catch (Exception e) {
                assert false : "Got unexpected exception: " + e;
            }

            // try to get the message using get() method
            try {
                future.get();
            } catch (CancellationException expected) {
                // expected
            } catch (Exception e) {
                assert false : "Got unexpected exception: " + e;
            }

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }

    private class TestRPCListener extends RPCBasicMessageListener<SpecificMessage, SpecificMessage> {
        private long sleep; // amount of seconds the onBasicMessage will sleep before returning the response

        public TestRPCListener() {
            sleep = 0L;
        }

        public TestRPCListener(long sleep) {
            this.sleep = sleep;
        }

        @Override
        protected SpecificMessage onBasicMessage(SpecificMessage requestMessage) {
            if (this.sleep > 0L) {
                try {
                    Thread.sleep(this.sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            SpecificMessage responseMessage = new SpecificMessage("RESPONSE:" + requestMessage.getMessage(), requestMessage.getDetails(), "RESPONSE:"
                    + requestMessage.getSpecific());
            return responseMessage;
        }
    }
}
