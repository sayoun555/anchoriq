package com.anchoriq.collector.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 수집 실패 시 재시도 로직.
 * 지정된 횟수만큼 재시도하며, 실패 시 로그를 남긴다.
 */
public class RetryHandler {

    private static final Logger log = LoggerFactory.getLogger(RetryHandler.class);

    private final int maxRetries;
    private final long delayMillis;

    public RetryHandler(int maxRetries, long delayMillis) {
        this.maxRetries = maxRetries;
        this.delayMillis = delayMillis;
    }

    public static RetryHandler withDefaults() {
        return new RetryHandler(3, 1000L);
    }

    public <T> T executeWithRetry(String operationName, Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                return operation.get();
            } catch (Exception e) {
                if (attempt >= maxRetries) {
                    log.error("[{}] Failed after {} attempts: {}", operationName, maxRetries, e.getMessage());
                    throw new CollectorException(operationName,
                            "Failed after " + maxRetries + " attempts", e);
                }
                log.warn("[{}] Attempt {}/{} failed: {}. Retrying in {}ms...",
                        operationName, attempt, maxRetries, e.getMessage(), delayMillis);
                sleep();
            }
        }
    }

    public void executeWithRetry(String operationName, Runnable operation) {
        executeWithRetry(operationName, () -> {
            operation.run();
            return null;
        });
    }

    private void sleep() {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CollectorException("RetryHandler", "Interrupted during retry delay", e);
        }
    }
}
