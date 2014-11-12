package org.rhq.msg.common;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class BasicMessageTest {

    // tests a minimal basic record with no details
    public void simpleConversion() {
        BasicMessage arec = new BasicMessage("my msg");
        String json = arec.toJSON();
        System.out.println(json);
        Assert.assertNotNull(json, "missing JSON");

        BasicMessage arec2 = BasicMessage.fromJSON(json, BasicMessage.class);
        Assert.assertNotNull(arec2, "JSON conversion failed");
        Assert.assertNotSame(arec, arec2);
        Assert.assertEquals(arec.getMessage(), arec2.getMessage());
        Assert.assertEquals(arec.getDetails(), arec2.getDetails());
    }

    // test a full basic record with several details
    public void fullConversion() {
        Map<String,String> details = new HashMap<String,String>();
        details.put("key1", "val1");
        details.put("secondkey", "secondval");

        BasicMessage arec = new BasicMessage("my msg", details);
        arec.setMessageId(new MessageId("12345"));
        arec.setCorrelationId(new MessageId("67890"));
        String json = arec.toJSON();
        System.out.println(json);
        Assert.assertNotNull(json, "missing JSON");

        BasicMessage arec2 = BasicMessage.fromJSON(json, BasicMessage.class);
        Assert.assertNotNull(arec2, "JSON conversion failed");
        Assert.assertNotSame(arec, arec2);
        Assert.assertNull(arec2.getMessageId(), "Message ID should not be encoded in JSON");
        Assert.assertNull(arec2.getCorrelationId(), "Correlation ID should not be encoded in JSON");
        Assert.assertEquals(arec2.getMessage(), "my msg");
        Assert.assertEquals(arec2.getDetails().size(), 2);
        Assert.assertEquals(arec2.getDetails().get("key1"), "val1");
        Assert.assertEquals(arec2.getDetails().get("secondkey"), "secondval");
        Assert.assertEquals(arec.getMessage(), arec2.getMessage());
        Assert.assertEquals(arec.getDetails(), arec2.getDetails());
    }

    public void testUnmodifiableDetails() {
        Map<String, String> details = new HashMap<String, String>();
        details.put("key1", "val1");

        BasicMessage msg = new BasicMessage("a", details);

        try {
            msg.getDetails().put("key1", "CHANGE!");
            assert false : "Should not have been able to change the details map";
        } catch (UnsupportedOperationException expected) {
            // to be expected
        }

        // make sure it didn't change and its still the same
        Assert.assertEquals(msg.getDetails().get("key1"), "val1");
    }
}
