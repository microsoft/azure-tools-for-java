package com.microsoft.azuretools.core.mvp.model.container.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Tag {

    @SerializedName("name")
    private String name;

    @SerializedName("tags")
    private ArrayList<String> tags;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }
}
