package com.microsoft.azuretools.core.mvp.ui.base;

import rx.Scheduler;

public interface SchedulerProvider {
    
    Scheduler io();
    
    Scheduler computation();
}
