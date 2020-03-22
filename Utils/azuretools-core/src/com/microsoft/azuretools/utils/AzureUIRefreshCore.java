/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
