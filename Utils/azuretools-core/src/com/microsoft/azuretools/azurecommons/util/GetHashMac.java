/**
 * Copyright (c) Microsoft Corporation
 * <p>
 * All rights reserved.
 * <p>
 * MIT License
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.azuretools.azurecommons.util;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetHashMac {

    public static final String MAC_REGEX = "([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}";
    public static final Pattern MAC_PATTERN = Pattern.compile(MAC_REGEX);
    public static final String[] UNIX_COMMAND = {"/sbin/ifconfig -a || /sbin/ip link"};
    public static final String[] WINDOWS_COMMAND = {"getmac"};
    public static final String[] INVALIDATE_MAC_ADDRESS = {"00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "ac:de:48:00:11:22"};

    // Hashed mac address for iBridge device
    public static final String INVALIDATE_HASHED_MAC_ADDRESS = "6c9d2bc8f91b89624add29c0abeae7fb42bf539fa1cdb2e3e57cd668fa9bcead";

    public static boolean IsValidHashMacFormat(String hashMac) {
        if (hashMac == null || hashMac.isEmpty()) {
            return false;
        }

        Pattern hashmac_pattern = Pattern.compile("[0-9a-f]{64}");
        Matcher matcher = hashmac_pattern.matcher(hashMac);
        return matcher.matches() && !(StringUtils.containsIgnoreCase(System.getProperty("os.name"), "mac") &&
                StringUtils.equalsIgnoreCase(INVALIDATE_HASHED_MAC_ADDRESS, hashMac));
    }

    public static String GetHashMac() {
        String ret = null;
        String mac_raw = getRawMac();
        mac_raw = isValidMac(mac_raw) ? mac_raw : getRawMacWithoutCommand();

        if (isValidMac(mac_raw)) {
            String mac_regex_zero = "([0]{2}[:-]){5}[0]{2}";
            Pattern pattern_zero = Pattern.compile(mac_regex_zero);
            Matcher matcher = MAC_PATTERN.matcher(mac_raw);
            String mac = "";
            while (matcher.find()) {
                mac = matcher.group(0);
                if (!pattern_zero.matcher(mac).matches()) {
                    break;
                }
            }
            ret = hash(mac);
        }

        return ret;
    }

    private static boolean isValidMac(String mac) {
        final boolean isMacAddress = StringUtils.isNotEmpty(mac) && MAC_PATTERN.matcher(mac).find();
        final boolean isValidateMacAddress = !Arrays.stream(INVALIDATE_MAC_ADDRESS)
                .anyMatch(invalidateMacAddress -> StringUtils.equalsIgnoreCase(mac, invalidateMacAddress));
        return isMacAddress && isValidateMacAddress;
    }

    private static String getRawMac() {
        String ret = null;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] command = StringUtils.startsWithIgnoreCase(os, "win") ?
                    WINDOWS_COMMAND : UNIX_COMMAND;
            ProcessBuilder probuilder = new ProcessBuilder(command);
            Process process = probuilder.start();
            try (final InputStream inputStream = process.getInputStream();
                 final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 final BufferedReader br = new BufferedReader(inputStreamReader)) {
                String tmp;
                while ((tmp = br.readLine()) != null) {
                    ret += tmp;
                }
            }
        } catch (IOException ex) {
            return null;
        }

        return ret;
    }

    private static String getRawMacWithoutCommand() {
        List<String> macSet = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.getHardwareAddress() != null) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    // Refers https://www.mkyong.com/java/how-to-get-mac-address-in-java/
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    macSet.add(sb.toString());
                }
            }
        } catch (SocketException e) {
            return StringUtils.EMPTY;
        }
        Collections.sort(macSet);
        return String.join(" ", macSet);
    }

    public static String hash(String mac) {
        if (mac == null || mac.isEmpty()) {
            return null;
        }

        String ret = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = mac.getBytes("UTF-8");
            md.update(bytes);
            byte[] bytesAfterDigest = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < bytesAfterDigest.length; i++) {
                sb.append(Integer.toString((bytesAfterDigest[i] & 0xff) + 0x100, 16).substring(1));
            }

            ret = sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }

        return ret;
    }
}
