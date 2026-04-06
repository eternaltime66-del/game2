package org.wx.core.web3unit;


import lombok.Data;
import lombok.SneakyThrows;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;
import org.wx.core.web3unit.tron.TronKeyUtils;
import org.wx.core.wxBase.factory.ErrorFactory;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
import java.util.*;

/**
 * ============================================================
 *  Web3WalletGenerator（优雅版）
 *  生成：
 *      ✔ 助记词
 *      ✔ TRON 钱包（地址 + 私钥）
 *      ✔ EVM 钱包（地址 + 私钥）
 *
 *  所有结果存入统一 Map，方便存储、读取、扩展
 * ============================================================
 */
public class Web3WalletGenerator {

    private static final int HARDENED = 0x80000000;


    public static boolean validateMnemonic(String mnemonic) {
        try {
            if (mnemonic == null) return false;
            List<String> words = Arrays.asList(mnemonic.trim().split(" "));
            // 必须满足 BIP39 单词数量规则：12/15/18/21/24
            if (!(words.size() == 12 || words.size() == 15 || words.size() == 18 || words.size() == 21 || words.size() == 24)) {
                return false;
            }

            // 尝试解析（包含词库校验 + 校验和验证）
            MnemonicCode mnemonicCode = new MnemonicCode();
            mnemonicCode.check(words);
            return true;

        } catch (Exception e) {
            return false;
        }
    }


    /**
     * ============================================================
     *  统一生成钱包（静态方法）
     * ============================================================
     */
    @SneakyThrows
    public static WalletResult generateWallet(String mnemonic) {

        WalletResult result = new WalletResult();

        // --------------------------------
        // 1) 生成助记词
        // --------------------------------
        SecureRandom random = new SecureRandom();
        byte[] entropy = new byte[16];
        random.nextBytes(entropy);

        MnemonicCode mnemonicCode = new MnemonicCode();
        List<String> words = mnemonicCode.toMnemonic(entropy);
        if ((mnemonic==null || mnemonic.isEmpty())){
            mnemonic = String.join(" ", words);
        }else {
            ErrorFactory.throwError(!validateMnemonic(mnemonic),"助记词不合法");
        }
        result.mnemonic = mnemonic;

        // 创建大 Map 容器
        Map<LinkGroup, WalletInfo> walletMap = new HashMap<>();


        // --------------------------------
        // 2) TRON 钱包（m/44'/195'/0'/0/0）
        // --------------------------------
        DeterministicSeed seed = new DeterministicSeed(words, null, "", 0);
        DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(seed).build();
        DeterministicKey tronKey =
                keyChain.getKeyByPath(parsePath("M/44H/195H/0H/0/0"), true);

        String tronPrivate = bytesToHex(tronKey.getPrivKeyBytes());
        String tronAddress = TronKeyUtils.privateKeyToTronAddress(tronPrivate);

        walletMap.put(LinkGroup.TRON, new WalletInfo(tronAddress,tronPrivate));


        // --------------------------------
        // 3) EVM 钱包（ETH/BSC/VAM） m/44'/60'/0'/0/0
        // --------------------------------
        Credentials evm = createEvmWalletFromMnemonic(mnemonic);

        String evmPrivate = evm.getEcKeyPair().getPrivateKey().toString(16);
        String evmAddress = evm.getAddress();

        walletMap.put(LinkGroup.EVM, new WalletInfo(evmAddress,evmPrivate));

        // 赋值
        result.wallet = walletMap;

        return result;
    }


    @Data
    public static class WalletInfo{
        public String address;
        public String privateKey;

        public WalletInfo(String address, String privateKey) {
            this.address = address;
            this.privateKey = privateKey;
        }
    }

    /**
     * ============================================================
     *  生成 EVM 钱包
     * ============================================================
     */
    public static Credentials createEvmWalletFromMnemonic(String mnemonic) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");
        Bip32ECKeyPair master = Bip32ECKeyPair.generateKeyPair(seed);

        int[] path = {44 | HARDENED, 60 | HARDENED, 0 | HARDENED, 0, 0};

        Bip32ECKeyPair derived = Bip32ECKeyPair.deriveKeyPair(master, path);
        return Credentials.create(derived);
    }

    /**
     * bytes → hex
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }


    /**
     * ============================================================
     *  最终结果类（全部装进 Map）
     * ============================================================
     */
    @Data
    public static class WalletResult {

        /** 助记词 */
        public String mnemonic;

        /** 所有链的钱包 Map */
        public Map<LinkGroup, WalletInfo> wallet;
        
    }

    public static List<ChildNumber> parsePath(@Nonnull String path) {
        String[] parsedNodes = path.replace("M", "").split("/");
        List<ChildNumber> nodes = new ArrayList();
        String[] var3 = parsedNodes;
        int var4 = parsedNodes.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String n = var3[var5];
            n = n.replaceAll(" ", "");
            if (n.length() != 0) {
                boolean isHard = n.endsWith("H");
                if (isHard) {
                    n = n.substring(0, n.length() - 1);
                }

                int nodeNumber = Integer.parseInt(n);
                nodes.add(new ChildNumber(nodeNumber, isHard));
            }
        }

        return nodes;
    }



}
