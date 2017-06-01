package com.microsoft.tooling.msservices.serviceexplorer.azure.base;

public interface MvpPresenter<V extends MvpView> {

    void onAttachView(V mvpView);

    void onDetachView();

}
