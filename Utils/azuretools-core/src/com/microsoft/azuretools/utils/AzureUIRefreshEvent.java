/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

public class AzureUIRefreshEvent {
  public enum EventType {
    ADD,
    REMOVE,
    UPDATE,
    REFRESH,
    SIGNIN,
    SIGNOUT
  }

  public Object object;
  public EventType opsType;

  public AzureUIRefreshEvent(EventType opsType, Object object) {
    this.opsType = opsType;
    this.object = object;
  }
}
