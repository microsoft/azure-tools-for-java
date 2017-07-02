package com.microsoft.azuretools.core.mvp.ui.base;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AppSchedulerProvider implements SchedulerProvider {
    
    private AppSchedulerProvider() {}
    
    private static final class  AppSchedulerProviderHolder {
        private static final  AppSchedulerProvider INSTANCE = new AppSchedulerProvider();
    }
    
    public static  AppSchedulerProvider getInstance() {
        return AppSchedulerProviderHolder.INSTANCE;
    }
    
    @Override
    public Scheduler io() {
        return Schedulers.io();
    }
    
    @Override
    public Scheduler computation() {
        return Schedulers.computation();
    }

}