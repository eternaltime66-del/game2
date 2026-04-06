package org.wx.core.web3unit;

import org.web3j.crypto.*;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 以太坊签名消息校验工具
 */
public class CryptoUtils {
    /**
     * 以太坊自定义的签名消息都以以下字符开头
     * 参考 eth_sign in https://github.com/ethereum/wiki/wiki/JSON-RPC
     */
    public static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    public static void main(String[] args) {
      /*  //签名后的数据
        String signature="0x53ea88d24f4ef8cdcc4bcc843912510b065cd6014c453ff61316c4cd75162f0a38f83a2103da028fb8e5181292ba194b0c8aa21a9ddacdf6783ebfa608889d121c";
        //签名原文
        String message="Hello Dapp";
        //签名的钱包地址
        String address="0x40141cf4756a72df8d8f81c1e0c2ad403c127b9e";

        */
           //签名后的数据
        String signature="0x0654555a884f7927db4dbec9b32177bbeb1fe7d26780c03518bfd873f17127e03277429f4548e42b8ad50d21f60bc6a7cd66afaecc4ea0bd28f7d18ab3a9b6b41c";
        //签名原文
        String message="aa";
        //签名的钱包地址
        String address="0x6dde1f58884b97f23f7769f6fb3f39e7560cb65b";

        Boolean result = validate(signature,message,address);
        //System.out.println(result);
    }
    /**
     * 对签名消息，原始消息，账号地址三项信息进行认证，判断签名是否有效
     * @param signature
     * @param message
     * @param address
     * @return
     */
    public static boolean validate(String signature, String message, String address) {
        address=address.toLowerCase();
        //参考 eth_sign in https://github.com/ethereum/wiki/wiki/JSON-RPC
        // eth_sign
        // The sign method calculates an Ethereum specific signature with:
        //    sign(keccak256("\x19Ethereum Signed Message:\n" + len(message) + message))).
        //
        // By adding a prefix to the message makes the calculated signature recognisable as an Ethereum specific signature.
        // This prevents misuse where a malicious DApp can sign arbitrary data (e.g. transaction) and use the signature to
        // impersonate the victim.
        String prefix = PERSONAL_MESSAGE_PREFIX + message.length();
        byte[] msgHash = Hash.sha3((prefix + message).getBytes());

        byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }

        SignatureData sd = new SignatureData(
                v,
                Arrays.copyOfRange(signatureBytes, 0, 32),
                Arrays.copyOfRange(signatureBytes, 32, 64));

        String addressRecovered = null;
        boolean match = false;

        // Iterate for each possible key to recover
        for (int i = 0; i < 4; i++) {
            BigInteger publicKey = Sign.recoverFromSignature(
                    (byte) i,
                    new ECDSASignature(new BigInteger(1, sd.getR()), new BigInteger(1, sd.getS())),
                    msgHash);

            if (publicKey != null) {
                addressRecovered = "0x" + Keys.getAddress(publicKey);

                if (addressRecovered.equals(address)) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }
}