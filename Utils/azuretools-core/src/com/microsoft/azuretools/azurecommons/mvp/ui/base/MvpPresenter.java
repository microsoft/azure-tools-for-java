package com.microsoft.azuretools.azurecommons.mvp.ui.base;

public interface MvpPresenter<V extends MvpView> {

    void onAttachView(V mvpView);

    void onDetachView();

}
