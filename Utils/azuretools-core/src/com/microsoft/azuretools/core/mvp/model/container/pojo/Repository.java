package com.microsoft.azuretools.core.mvp.model.container.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Repository {

    @SerializedName("repositories")
    private ArrayList<String> repositories;

    public ArrayList<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(ArrayList<String> repositories) {
        this.repositories = repositories;
    }
}
