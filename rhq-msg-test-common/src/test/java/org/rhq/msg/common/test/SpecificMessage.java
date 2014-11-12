package org.rhq.msg.common.test;

import java.util.Map;

import org.rhq.msg.common.BasicMessage;

import com.google.gson.annotations.Expose;

/**
 * Test subclass of BasicMessage.
 */
public class SpecificMessage extends BasicMessage {
    @Expose
    private final String specific;

    public SpecificMessage(String message, Map<String, String> details, String specific) {
        super(message, details);
        if (specific == null) {
            throw new NullPointerException("specific string cannot be null");
        }
        this.specific = specific;
    }

    public String getSpecific() {
        return specific;
    }
}
