package com.microsoft.azuretools.core.model;

public class NodeContent {
    
    public NodeContent(String id, String name, String provisionState) {
        this.id = id;
        this.name = name;
        this.provisionState = provisionState;
    }

    private String id;
    
    private String name;
    
    private String provisionState;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvisionState() {
        return provisionState;
    }

    public void setProvisionState(String provisionState) {
        this.provisionState = provisionState;
    }
}
