package org.rhq.msg.broker;

import org.testng.annotations.Test;

@Test
public class EmbeddedBrokerConfigurationTest {
    public void testPropertiesConfig() throws Exception {
        new EmbeddedBroker(new String[] { "--config=test-broker.properties" }).stopBroker();
    }

    public void testXMLConfig() throws Exception {
        new EmbeddedBroker(new String[] { "--config=test-broker.xml", "-Dtest.bind.port=61616" }).stopBroker();
    }
}
