package com.azure.core.implementation;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.lang.invoke.MethodHandles;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

final class ReflectionUtils implements ReflectionUtilsApi {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Module CORE_MODULE = ReflectionUtils.class.getModule();

    public MethodHandles.Lookup getLookupToUse(Class<?> targetClass) throws Exception {
        Module responseModule = targetClass.getModule();
        if (!responseModule.isNamed()) {
            CORE_MODULE.addReads(responseModule);
            return performSafePrivateLookupIn(targetClass);
        } else if (responseModule == CORE_MODULE) {
            return LOOKUP;
        } else if (!responseModule.isOpen(targetClass.getPackageName()) && !responseModule.isOpen(targetClass.getPackageName(), CORE_MODULE)) {
            return MethodHandles.publicLookup();
        } else {
            CORE_MODULE.addReads(responseModule);
            return performSafePrivateLookupIn(targetClass);
        }
    }

    public int getJavaImplementationMajorVersion() {
        return 9;
    }

    private static MethodHandles.Lookup performSafePrivateLookupIn(Class<?> targetClass) throws Exception {
        return System.getSecurityManager() == null ? MethodHandles.privateLookupIn(targetClass, LOOKUP) : (MethodHandles.Lookup) AccessController.doPrivileged((PrivilegedExceptionAction<MethodHandles.Lookup>) () -> MethodHandles.privateLookupIn(targetClass, LOOKUP));
    }

    ReflectionUtils() {
    }
}
