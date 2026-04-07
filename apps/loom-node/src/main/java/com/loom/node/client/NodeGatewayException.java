package com.loom.node.client;

public class NodeGatewayException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public NodeGatewayException(String message, Throwable cause) {
        this(-1, null, message, cause);
    }

    public NodeGatewayException(int statusCode, String responseBody, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
