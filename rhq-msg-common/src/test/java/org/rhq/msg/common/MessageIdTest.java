package org.rhq.msg.common;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class MessageIdTest {
    public void messageIdEquality() {
        MessageId one = new MessageId("msg1");
        MessageId oneDup = new MessageId("msg1");
        MessageId two = new MessageId("msg2");
        Assert.assertEquals(one, oneDup);
        Assert.assertFalse(one.equals(two));
    }
}
