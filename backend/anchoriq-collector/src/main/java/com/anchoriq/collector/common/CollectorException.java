package com.anchoriq.collector.common;

/**
 * 데이터 수집 과정에서 발생하는 예외.
 */
public class CollectorException extends RuntimeException {

    private final String source;

    public CollectorException(String source, String message) {
        super(String.format("[%s] %s", source, message));
        this.source = source;
    }

    public CollectorException(String source, String message, Throwable cause) {
        super(String.format("[%s] %s", source, message), cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
