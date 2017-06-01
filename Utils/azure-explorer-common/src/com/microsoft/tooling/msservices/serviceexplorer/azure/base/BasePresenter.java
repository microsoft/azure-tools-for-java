package com.microsoft.tooling.msservices.serviceexplorer.azure.base;

public class BasePresenter<V extends MvpView> implements MvpPresenter<V> {

    private V mMvpView;

    public V getMvpView() {
        return mMvpView;
    }

    @Override
    public void onAttachView(V mvpView) {
        mMvpView = mvpView;
    }

    @Override
    public void onDetachView() {
        mMvpView = null;
    }

}
