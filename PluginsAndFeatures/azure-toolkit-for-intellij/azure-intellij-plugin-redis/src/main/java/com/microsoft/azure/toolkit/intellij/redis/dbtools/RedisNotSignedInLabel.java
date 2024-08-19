package com.microsoft.azure.toolkit.intellij.redis.dbtools;


import com.microsoft.azure.toolkit.intellij.dbtools.NotSignedInTipLabel;

public class RedisNotSignedInLabel extends NotSignedInTipLabel {
    private static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select an existing Redis cache in Azure.</html>";

    public RedisNotSignedInLabel() {
        super(NOT_SIGNIN_TIPS);
    }

    @Override
    protected void signIn() {
        super.signIn();
    }
}