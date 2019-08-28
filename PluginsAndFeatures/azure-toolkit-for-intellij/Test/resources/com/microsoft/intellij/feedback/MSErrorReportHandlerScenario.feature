Feature: MSErrorReportHandler tests

  Scenario: Can filter call stacks Microsoft related package functions
    Given the full call stacks to filter MSCallStacks
      """
    IdeaLoggingEvent[message=null, throwable=java.lang.IllegalStateException: Exception thrown on Scheduler.Worker thread. Add `onError` handling.
		at rx.internal.schedulers.ScheduledAction.run(ScheduledAction.java:57)
		at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
		at java.util.concurrent.FutureTask.run(FutureTask.java:266)
		at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:180)
		at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:293)
		at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
		at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
		at java.lang.Thread.run(Thread.java:748)
	Caused by: rx.exceptions.OnErrorNotImplementedException: Coordinate out of bounds!
		at rx.internal.util.InternalObservableUtils$ErrorNotImplementedAction.call(InternalObservableUtils.java:386)
		at rx.internal.util.InternalObservableUtils$ErrorNotImplementedAction.call(InternalObservableUtils.java:383)
		at rx.internal.util.ActionSubscriber.onError(ActionSubscriber.java:44)
		at rx.observers.SafeSubscriber._onError(SafeSubscriber.java:153)
		at rx.observers.SafeSubscriber.onError(SafeSubscriber.java:115)
		at rx.exceptions.Exceptions.throwOrReport(Exceptions.java:212)
		at rx.observers.SafeSubscriber.onNext(SafeSubscriber.java:139)
		at rx.internal.operators.OperatorTake$1.onNext(OperatorTake.java:79)
		at rx.internal.operators.OperatorSubscribeOn$SubscribeOnSubscriber.onNext(OperatorSubscribeOn.java:74)
		at rx.internal.operators.OnSubscribeTimerOnce$1.call(OnSubscribeTimerOnce.java:49)
		at rx.internal.schedulers.EventLoopsScheduler$EventLoopWorker$2.call(EventLoopsScheduler.java:189)
		at rx.internal.schedulers.ScheduledAction.run(ScheduledAction.java:55)
		... 7 more
	Caused by: java.lang.ArrayIndexOutOfBoundsException: Coordinate out of bounds!
		at sun.awt.image.IntegerInterleavedRaster.getDataElements(IntegerInterleavedRaster.java:264)
		at sun.awt.image.OffScreenImageSource.sendPixels(OffScreenImageSource.java:136)
		at sun.awt.image.OffScreenImageSource.produce(OffScreenImageSource.java:187)
		at sun.awt.image.OffScreenImageSource.addConsumer(OffScreenImageSource.java:66)
		at sun.awt.image.OffScreenImageSource.startProduction(OffScreenImageSource.java:80)
		at java.awt.image.FilteredImageSource.startProduction(FilteredImageSource.java:183)
		at sun.awt.image.ImageRepresentation.startProduction(ImageRepresentation.java:732)
		at sun.awt.image.ToolkitImage.addWatcher(ToolkitImage.java:221)
		at sun.awt.image.ToolkitImage.getProperty(ToolkitImage.java:169)
		at javax.swing.ImageIcon.<init>(ImageIcon.java:240)
		at com.microsoft.intellij.util.PluginUtil.getIcon(PluginUtil.java:183)
		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.createUIComponents(SurveyPopUpDialog.java:223)
		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.$$$setupUI$$$(SurveyPopUpDialog.java)
		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.<init>(SurveyPopUpDialog.java:55)
		at com.microsoft.intellij.helpers.CustomerSurveyHelper.lambda$showFeedbackNotification$0(CustomerSurveyHelper.java:68)
		at rx.internal.util.ActionSubscriber.onNext(ActionSubscriber.java:39)
		at rx.observers.SafeSubscriber.onNext(SafeSubscriber.java:134)
		... 12 more
      """
    Then check the filtered MSCallStacks result
      """
    IdeaLoggingEvent[message=null, throwable=java.lang.IllegalStateException: Exception thrown on Scheduler.Worker thread. Add `onError` handling.
		at rx.internal.schedulers.ScheduledAction.run(ScheduledAction.java:57)
	Caused by: rx.exceptions.OnErrorNotImplementedException: Coordinate out of bounds!
	Caused by: java.lang.ArrayIndexOutOfBoundsException: Coordinate out of bounds!
		at com.microsoft.intellij.util.PluginUtil.getIcon(PluginUtil.java:183)
		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.createUIComponents(SurveyPopUpDialog.java:223)
		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.$$$setupUI$$$(SurveyPopUpDialog.java)
		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.<init>(SurveyPopUpDialog.java:55)
		at com.microsoft.intellij.helpers.CustomerSurveyHelper.lambda$showFeedbackNotification$0(CustomerSurveyHelper.java:68)
      """
