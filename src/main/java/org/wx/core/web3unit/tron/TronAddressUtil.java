package org.wx.core.web3unit.tron;

import org.bitcoinj.base.Base58;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.Arrays;

public class TronAddressUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * TRON Hex 地址 → Base58
     * 41xxxxxx -> Txxxxxx
     */
    public static String tronHexToBase58(String hexAddress) {
        if (hexAddress == null || !hexAddress.startsWith("41")) {
            return null;
        }

        byte[] addressBytes = hexStringToBytes(hexAddress);
        byte[] checkSum = Arrays.copyOfRange(
                sha256(sha256(addressBytes)), 0, 4
        );

        byte[] addressWithCheck = new byte[addressBytes.length + 4];
        System.arraycopy(addressBytes, 0, addressWithCheck, 0, addressBytes.length);
        System.arraycopy(checkSum, 0, addressWithCheck, addressBytes.length, 4);

        return Base58.encode(addressWithCheck);
    }

    // ================== utils ==================

    private static byte[] sha256(byte[] input) {
        try {
            return java.security.MessageDigest
                    .getInstance("SHA-256")
                    .digest(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] hexStringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte) ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
