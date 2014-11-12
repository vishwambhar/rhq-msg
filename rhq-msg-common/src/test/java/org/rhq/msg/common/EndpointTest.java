package org.rhq.msg.common;

import org.rhq.msg.common.Endpoint.Type;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class EndpointTest {
    public void endpointEquality() {
        Endpoint q1 = new Endpoint(Type.QUEUE, "foo");
        Endpoint q1Dup = new Endpoint(Type.QUEUE, "foo");
        Endpoint t1 = new Endpoint(Type.TOPIC, "foo");
        Endpoint t1Dup = new Endpoint(Type.TOPIC, "foo");
        Endpoint q2 = new Endpoint(Type.QUEUE, "bar");
        Endpoint t2 = new Endpoint(Type.TOPIC, "bar");
        Assert.assertTrue(q1.equals(q1Dup));
        Assert.assertTrue(t1.equals(t1Dup));

        Assert.assertFalse(q1.equals(q2));
        Assert.assertFalse(q1.equals(t1));
        Assert.assertFalse(q1.equals(t2));

        Assert.assertFalse(t1.equals(t2));
        Assert.assertFalse(t1.equals(q1));
        Assert.assertFalse(t1.equals(q2));
    }
}
