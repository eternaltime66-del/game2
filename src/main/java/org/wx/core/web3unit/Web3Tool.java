package org.wx.core.web3unit;


import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.RawTransaction;
import org.wx.core.web3unit.evm.EvmFun;
import org.wx.core.web3unit.evm.EvmUnit;
import org.wx.core.web3unit.tron.TronUnit;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Web3Coin;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Web3Tool {

    // ============================================================
    //   构建 EVM
    // ============================================================
    private static EvmUnit buildEvm(Web3Coin coin, String prvKey) {
        Web3Config cfg;
        switch (coin.getChain()) {
            case BSC:
                cfg = Web3Config.createBsc(prvKey);
                break;
            case ETH:
                cfg = Web3Config.createEth(prvKey);
                break;
            default:
                throw new RuntimeException("不是 EVM 链：" + coin.getChain());
        }

        return new EvmUnit(cfg);
    }

    // ============================================================
    //   查询余额（统一入口）
    // ============================================================
    public static BigDecimal getBalance(Web3Coin coin, String address) {

        switch (coin.getChain()) {

            case BSC:
            case ETH: {
                EvmUnit u = buildEvm(coin, null);
                return coin.isMainCoin()
                        ? u.client.getMainBalance(address, coin.getDecimals())
                        : u.client.getTokenBalance(address, coin.getTokenAddress(), coin.getDecimals());
            }

            case TRON:
                try {
                    return coin.isMainCoin()
                            ? TronUnit.getMainBalance(address)
                            : TronUnit.getTokenBalance(address, coin.getTokenAddress(), coin.getDecimals());
                } catch (Exception e) {
                    throw new RuntimeException("TRON 查询失败: " + e.getMessage());
                }

            default:
                throw new RuntimeException("不支持链：" + coin.getChain());
        }
    }

    // ============================================================
    //   转账（统一入口：自动识别主币/Token + EVM/TRON）
    // ============================================================
    public static String transfer(
            Web3Coin coin,
            String from,
            String prvKey,
            String to,
            BigDecimal amount) {

        BigDecimal balance = getBalance(coin, from);
        ErrorFactory.throwError(amount.compareTo(balance)>0,"钱包余额不足");

        return coin.isMainCoin()
                ? doTransferMain(coin, from, prvKey, to, amount)
                : doTransferToken(coin, from, prvKey, to, amount);
    }

    // ---------------- 内部分发：主币 ----------------
    private static String doTransferMain(Web3Coin coin, String from, String prvKey, String to, BigDecimal amount) {

        switch (coin.getChain()) {
            case BSC:
            case ETH: {
                EvmUnit u = buildEvm(coin, prvKey);
                BigInteger wei = EvmUnit.AmountUtil.toWei(amount, coin.getDecimals());
                BigInteger nonce = u.client.getNonce(from);
                RawTransaction tx = RawTransaction.createEtherTransaction(
                        nonce,
                        EvmUnit.AmountUtil.ONE_GWEI,
                        EvmUnit.AmountUtil.GAS_LIMIT_21000,
                        to,
                        wei
                );
                return u.sender.send(u.signer.sign(tx));
            }

            case TRON:
                try {
                    return TronUnit.transferMain(from, prvKey, to, amount);
                } catch (Exception e) {
                    throw new RuntimeException("TRON 主币转账失败: " + e.getMessage());
                }

            default:
                throw new RuntimeException("不支持链：" + coin.getChain());
        }
    }

    // ---------------- 内部分发：Token ----------------
    private static String doTransferToken(Web3Coin coin, String from, String prvKey, String to, BigDecimal amount) {

        switch (coin.getChain()) {

            case BSC:
            case ETH: {
                EvmUnit u = buildEvm(coin, prvKey);
                BigInteger wei = EvmUnit.AmountUtil.toWei(amount, coin.getDecimals());
                Function fn = EvmFun.create("transfer", to, wei);
                return u.send(fn, coin.getTokenAddress()).sync();
            }

            case TRON:
                try {
                    return TronUnit.transferToken(
                            from, prvKey, coin.getTokenAddress(), to, amount, coin.getDecimals()
                    );
                } catch (Exception e) {
                    throw new RuntimeException("TRON Token 转账失败: " + e.getMessage());
                }

            default:
                throw new RuntimeException("不支持链：" + coin.getChain());
        }
    }

    // ============================================================
    //   统一哈希校验（主币 + Token）
    // ============================================================
    public static Web3HashCheckResult checkHash(
            Web3Coin coin,
            String hash,
            String expectFrom,
            String expectTo,
            BigDecimal expectAmount) {

        return coin.isMainCoin()
                ? doCheckMainHash(coin, hash, expectFrom, expectTo, expectAmount)
                : doCheckTokenHash(coin, hash, expectFrom, expectTo, expectAmount);
    }

    // ---------------- 主币 hash 校验 ----------------
    private static Web3HashCheckResult doCheckMainHash(
            Web3Coin coin,
            String hash,
            String expectFrom,
            String expectTo,
            BigDecimal expectAmount) {

        switch (coin.getChain()) {

            case BSC:
            case ETH:
                return buildEvm(coin, null)
                        .checkMainHash(coin, hash, expectFrom, expectTo, expectAmount);

            case TRON:
//                return TronUnit.checkMainHash(
//                        hash,
//                        expectFrom,
//                        expectTo,
//                        expectAmount
//                );

            default:
                throw new RuntimeException("不支持链：" + coin.getChain());
        }
    }

    // ---------------- Token hash 校验 ----------------
    private static Web3HashCheckResult doCheckTokenHash(
            Web3Coin coin,
            String hash,
            String expectFrom,
            String expectTo,
            BigDecimal expectAmount) {

        switch (coin.getChain()) {

            case BSC:
            case ETH:
                return buildEvm(coin, null)
                        .checkTokenHash(coin, hash, expectFrom, expectTo, expectAmount);

            case TRON:
                return TronUnit.checkTokenHash(
                        hash,
                        expectFrom,
                        expectTo,
                        expectAmount,
                        coin.getTokenAddress(),
                        coin.getDecimals()
                );

            default:
                throw new RuntimeException("不支持链：" + coin.getChain());
        }
    }

    public static void main(String[] args) {
//        BigDecimal balance = getBalance(Web3Coin.TRON_USDT, "TMWHMZJR2kuZsoZAVP3qRBAAVdXDdE7e1B");
//        System.out.println(balance);
//        Web3HashCheckResult web3HashCheckResult = doCheckTokenHash(Web3Coin.TRON_USDT, "4f70bef5633b8ce8b1776e549257b8bf26b6b2b8137180e45387696ecabc398a", null, null, null);
//        web3HashCheckResult.logJson();
//        String transfer = transfer(
//                Web3Coin.TRON_USDT,
//                "TMWHMZJR2kuZsoZAVP3qRBAAVdXDdE7e1B",
//                "0x5d3cc6b290570a38f95d9ddc15bf745c47b32c8f22be67dd4e934af008f40d7b",
//                Web3Coin.TRON_USDT.getChain().getRechargeAddress(),
//                new BigDecimal("0.5")
//        );
//        System.out.println(transfer);
    }
}
