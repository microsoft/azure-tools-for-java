package com.microsoft.azuretools.core.mvp.model;

public class ResourceEx<T> {
    private T resource;
    private String subscriptionId;
    public ResourceEx(T resource, String subscriptionId) {
        this.resource = resource;
        this.subscriptionId = subscriptionId;
    }
    public T getResource() {
        return resource;
    }
    public void setResource(T resource) {
        this.resource = resource;
    }
    public String getSubscriptionId() {
        return subscriptionId;
    }
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
