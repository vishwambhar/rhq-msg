package org.rhq.msg.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * Basic information that is sent over the message bus.
 *
 * The {@link #getMessageId() message ID} is assigned by the messaging framework and so typically is not explicitly set.
 *
 * The {@link #getCorrelationId() correlation ID} is a message ID of another message that was sent previously. This is
 * usually left unset unless this message needs to be correlated with another. As an example, when a process is stopped,
 * you can correlate the "Stopped" event with the "Stopping" event so you can later determine how long it took for the
 * process to stop.
 */
public class BasicMessage {
    // these are passed out-of-band of the message body - these attributes will therefore not be JSON encoded
    private MessageId messageId;
    private MessageId correlationId;

    // the basic message body - it will be exposed to the JSON output
    @Expose
    private String message;

    // some optional additional details about the basic message
    @Expose
    private Map<String, String> details;

    /**
     * Convenience static method that converts a JSON string to a particular message object.
     *
     * @param json
     *            the JSON string
     * @param clazz
     *            the class whose instance is represented by the JSON string
     *
     * @return the message object that was represented by the JSON string
     */
    public static <T extends BasicMessage> T fromJSON(String json, Class<T> clazz) {
        final Gson gson = createGsonBuilder();
        return gson.fromJson(json, clazz);
    }

    /**
     * Converts this message to its JSON string representation.
     *
     * @return JSON encoded data that represents this message.
     */
    public String toJSON() {
        final Gson gson = createGsonBuilder();
        return gson.toJson(this);
    }

    protected BasicMessage() {
        ; // Intentionally left blank
    }

    public BasicMessage(String message) {
        this(message, null);
    }

    public BasicMessage(String message, Map<String, String> details) {
        this.message = message;

        // make our own copy of the details data
        if (details != null && !details.isEmpty()) {
            this.details = new HashMap<String, String>(details);
        } else {
            this.details = null;
        }
    }

    /**
     * Returns the message ID that was assigned to this message by the messaging infrastructure. This could be null if
     * the message has not been sent yet.
     *
     * @return message ID assigned to this message by the messaging framework
     */
    public MessageId getMessageId() {
        return messageId;
    }

    public void setMessageId(MessageId messageId) {
        this.messageId = messageId;
    }

    /**
     * If this message is correlated with another message, this will be that other message's ID. This could be null if
     * the message is not correlated with another message.
     *
     * @return the message ID of the correlated message
     */
    public MessageId getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(MessageId correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * The basic message string of this message.
     *
     * @return message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Allow subclasses to set the message
     */
    protected void setMessage(String msg) {
        this.message = msg;
    }

    /**
     * Optional additional details about this message. This could be null if there are no additional details associated
     * with this message.
     *
     * @return the details of this message or null. This is an unmodifiable, read-only map of details.
     */
    public Map<String, String> getDetails() {
        if (details == null) {
            return null;
        }
        return Collections.unmodifiableMap(details);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass().getSimpleName() + ": [");
        str.append("message-id=");
        str.append(getMessageId());
        str.append(", correlation-id=");
        str.append(getCorrelationId());
        str.append(", json-body=[");
        str.append(toJSON());
        str.append("]]");
        return str.toString();
    }

    protected static Gson createGsonBuilder() {
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }
}
