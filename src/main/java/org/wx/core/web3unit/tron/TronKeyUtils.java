package org.wx.core.web3unit.tron;

import org.bitcoinj.base.Base58;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

public class TronKeyUtils {

    /**
     * 私钥转 TRON 地址
     * @param privateKeyHex 64位私钥（hex字符串）
     * @return Base58Check 格式的 TRON 地址（T开头）
     */
    public static String privateKeyToTronAddress(String privateKeyHex) {
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        ECKeyPair keyPair = ECKeyPair.create(privateKey);

        // 获取未压缩公钥的 Keccak256 哈希（取后20字节）
        byte[] pubKey = Numeric.hexStringToByteArray(Keys.getAddress(keyPair));  // 20 bytes

        // 拼接 TRON 主网地址前缀 0x41
        byte[] tronAddress = new byte[21];
        tronAddress[0] = 0x41;
        System.arraycopy(pubKey, 0, tronAddress, 1, 20);

        // 计算 checksum：两次 SHA-256 取前4字节
        byte[] hash0 = sha256(tronAddress);
        byte[] hash1 = sha256(hash0);
        byte[] checksum = Arrays.copyOfRange(hash1, 0, 4);

        // 拼接 address + checksum
        byte[] addressWithChecksum = new byte[25];
        System.arraycopy(tronAddress, 0, addressWithChecksum, 0, 21);
        System.arraycopy(checksum, 0, addressWithChecksum, 21, 4);

        // Base58Check 编码
        return Base58.encode(addressWithChecksum);
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    public static void main(String[] args) {
        String privateKey = "17696d15284a8589d3e61a2d11cf152f819fa385d0669a6360f7c019706ef28f";
        String tronAddress = privateKeyToTronAddress(privateKey);
        System.out.println("TRON 地址: " + tronAddress);
    }
}
