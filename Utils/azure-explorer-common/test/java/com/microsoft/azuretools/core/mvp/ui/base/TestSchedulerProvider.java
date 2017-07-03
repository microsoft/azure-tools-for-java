package com.microsoft.azuretools.core.mvp.ui.base;

import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProvider;

import rx.Scheduler;
import rx.schedulers.TestScheduler;

public class TestSchedulerProvider implements SchedulerProvider {
    
    private TestSchedulerProvider() {}
    
    private TestScheduler testScheduler = new TestScheduler();
    
    private static final class TestSchedulerProviderHolder {
        private static final  TestSchedulerProvider INSTANCE = new TestSchedulerProvider();
    }
    
    public static  TestSchedulerProvider getInstance() {
        return TestSchedulerProviderHolder.INSTANCE;
    }
    
    @Override
    public Scheduler io() {
        return testScheduler;
    }
    
    @Override
    public Scheduler computation() {
        return testScheduler;
    }
    
    public void triggerActions() {
        testScheduler.triggerActions();
    }

}
