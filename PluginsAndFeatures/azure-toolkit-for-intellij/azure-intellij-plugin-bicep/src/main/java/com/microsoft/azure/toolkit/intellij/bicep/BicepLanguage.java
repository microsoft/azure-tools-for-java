// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.microsoft.azure.toolkit.intellij.bicep;

import com.intellij.lang.Language;

public class BicepLanguage extends Language {

  public static final BicepLanguage INSTANCE = new BicepLanguage();

  private BicepLanguage() {
    
    super("Bicep");
  }

}
