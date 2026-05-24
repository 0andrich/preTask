package org.backend.dto.request;

public record ManualRetryRequest(boolean retryCount) {
    public ManualRetryRequest(){
        this(true);
    }
}
