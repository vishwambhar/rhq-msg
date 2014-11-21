package org.rhq.msg.sample.client;

import org.rhq.msg.common.BasicMessage;
import org.rhq.msg.common.ConnectionContextFactory;
import org.rhq.msg.common.Endpoint;
import org.rhq.msg.common.MessageProcessor;
import org.rhq.msg.common.consumer.BasicMessageListener;
import org.rhq.msg.common.consumer.ConsumerConnectionContext;

/**
 * A simple sample client
 * @author Heiko W. Rupp
 */
public class Main {


    public static void main(String[] args) throws Exception {

        Main main = new Main();
        main.run();
    }

    private void run() throws Exception {
        Endpoint endpoint = new Endpoint(Endpoint.Type.QUEUE, "metrics");
        ConnectionContextFactory factory = new ConnectionContextFactory("tcp://localhost:17173");
        ConsumerConnectionContext context = factory.createConsumerConnectionContext(endpoint);
        MessageProcessor processor = new MessageProcessor();
        BasicMessageListener listener = new MyCustomListener();
        processor.listen(context, listener);
    }


    public class MyCustomListener extends BasicMessageListener<BasicMessage> {
        @Override
        protected void onBasicMessage(BasicMessage receivedMessage) {

            System.out.println(receivedMessage.getMessage());
        }
    }
}
