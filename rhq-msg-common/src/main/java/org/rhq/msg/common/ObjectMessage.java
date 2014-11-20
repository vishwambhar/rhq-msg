package org.rhq.msg.common;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A message that contains a complex object, which gets serialized
 * into JSON
 * @author Heiko W. Rupp
 */
public class ObjectMessage extends BasicMessage {

    private ObjectMessage(String message) {
        super(message);
    }

    private ObjectMessage(String message, Map<String, String> details) {
        super(message, details);
    }

    public ObjectMessage(Object object) {
        super();
        Gson gson = new GsonBuilder().create();
        String msg = gson.toJson(object);
        setMessage(msg);
    }
}
