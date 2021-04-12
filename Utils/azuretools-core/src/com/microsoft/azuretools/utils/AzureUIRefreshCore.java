/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.HashMap;
import java.util.Map;

public class AzureUIRefreshCore {
  public static final boolean RUN_LISTENER_EVENT_OPS = false;
  public static Map<String, AzureUIRefreshListener> listeners;

  public static synchronized void addListener(String id, AzureUIRefreshListener listener) {
    if (listeners == null) {
      listeners = new HashMap<>();
    }
    listeners.put(id, listener);
    if (RUN_LISTENER_EVENT_OPS) execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.ADD, id));
  }

  public static synchronized void execute(AzureUIRefreshEvent event) {
    if (listeners != null && !listeners.isEmpty()) {
      Observable.from(listeners.values()).flatMap((listener) ->
          Observable.create( subscriber -> {
              listener.setEvent(event);
              listener.run();
              subscriber.onNext(listener);
              subscriber.onCompleted();
          })
      ).subscribeOn(Schedulers.io()).toBlocking().subscribe();
    }
  }

  public static synchronized void removeListener(String id) {
    if (listeners != null) {
      try {
        if (RUN_LISTENER_EVENT_OPS) execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REMOVE, id));
        listeners.remove(id);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public static synchronized void removeAll() {
    if (listeners != null) {
      for (String id : listeners.keySet()) {
        if (RUN_LISTENER_EVENT_OPS) execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REMOVE, id));
      }
    }
  }
}
