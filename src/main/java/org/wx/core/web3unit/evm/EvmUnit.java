package org.wx.core.web3unit.evm;


import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;
import org.wx.core.web3unit.Web3Config;
import org.wx.core.web3unit.Web3HashCheckResult;
import org.wx.core.web3unit.Web3Tool;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.context.ReqContextHolder;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.entity.Web3RunWatch;
import org.wx.core.wxBusiness.account.service.Web3RunWatchService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
@Slf4j
public class EvmUnit {

    // ===============================
    // 基础成员
    // ===============================
    private final Web3j web3;
    private final Credentials credentials; // read-only 时为 null
    private final long chainId;

    private final Map<String, BigInteger> nonceCache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public BigInteger gasPrice = BigInteger.valueOf(50_000_000L); // 3 Gwei
    public BigInteger gasLimit = BigInteger.valueOf(5_000_000);        // 普通合约操作
    public BigInteger gasLimitComplex = BigInteger.valueOf(2_000_000L); // 复杂操作（createWork/finalizeSuccess）
    public static final BigInteger GAS_LIMIT_NATIVE = BigInteger.valueOf(21_000L); // 主币转账固定值

    public static class AmountUtil {

        /**
         * 1 Gwei
         */
        public static final BigInteger ONE_GWEI = BigInteger.valueOf(1_000_000_000L);

        /**
         * 主币转账 gas limit 固定是 21000
         */
        public static final BigInteger GAS_LIMIT_21000 = BigInteger.valueOf(21_000L);

        public static BigDecimal fromWei(BigInteger val, int accuracy) {
            return new BigDecimal(val)
                    .divide(BigDecimal.TEN.pow(accuracy), accuracy, RoundingMode.DOWN);
        }

        public static BigInteger toWei(BigDecimal val, int accuracy) {
            return val.multiply(BigDecimal.TEN.pow(accuracy))
                    .setScale(0, RoundingMode.DOWN)
                    .toBigInteger();
        }

    }

    // ===============================
    // 构造：支持 read-only
    // ===============================
    public EvmUnit(Web3Config config) {
        this.web3 = config.getWeb3j();
        this.chainId = config.getChainId();
        this.credentials = (config.getPrivateKey() == null || config.getPrivateKey().isEmpty())
                ? null
                : Credentials.create(config.getPrivateKey());
        if (this.credentials != null && !config.getPrivateKey().equals("0000000000000000000000000000000000000000000000000000000000000001")) {
            String userAddress = this.credentials.getAddress();
            BigDecimal balance = Web3Tool.getBalance(Web3Coin.BSC_BNB, userAddress);
            System.out.println("执行人: " + userAddress + " BNB 余额: " + balance.stripTrailingZeros().toPlainString());
            ErrorFactory.throwError(balance.compareTo(new BigDecimal("0.0005")) < 0, "601","执行人 BNB 数量不得小于 0.0005");
        }
    }

    // 禁止交易逻辑
    private void requireWritable() {
        if (credentials == null) {
            throw new RuntimeException("当前 Web3Unit 为 read-only 模式，无法执行写操作！");
        }
    }

    // ============================================================
    //                        Client - RPC
    // ============================================================
    public class Client {

        @SneakyThrows
        public BigInteger getNonce(String address) {
            requireWritable();

            EthGetTransactionCount r =
                    web3.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();

            BigInteger nonce = r.getTransactionCount();

            BigInteger last = nonceCache.get(address);
            if (last != null && nonce.compareTo(last) <= 0) {
                nonce = last.add(BigInteger.ONE);
            }

            nonceCache.put(address, nonce);
            return nonce;
        }

        @SneakyThrows
        public BigDecimal getMainBalance(String address, int decimals) {
            EthGetBalance r = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            return amountFromWei(r.getBalance(), decimals);
        }

        @SneakyThrows
        public BigDecimal getTokenBalance(String address, String token, int decimals) {
            Function fn = EvmFun.create("balanceOf", address);

            EthCall call = web3.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            address, token, FunctionEncoder.encode(fn)
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (call.getValue() == null || call.getValue().length() <= 2) {
                return BigDecimal.ZERO;
            }

            BigInteger val = Numeric.toBigInt(call.getValue());
            return amountFromWei(val, decimals);
        }

        @SneakyThrows
        public BigInteger estimateGas(String from, String to, String data) {
            requireWritable();
            try {
                BigInteger estimated = web3.ethEstimateGas(
                        org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(from, to, data)
                ).send().getAmountUsed();
                // 乘以1.2倍留余量，避免Gas不足
                BigInteger gasLimit = estimated.multiply(BigInteger.valueOf(12)).divide(BigInteger.TEN);
                log.info("估算Gas Limit：{}（原始：{}）", gasLimit, estimated);
                return gasLimit;
            } catch (Exception e) {
                log.warn("估算Gas失败，使用默认值：{}", gasLimit);
                return gasLimit;
            }
        }
    }

    public final Client client = new Client();

    // ============================================================
    // Signer
    // ============================================================
    public class Signer {

        // ========== 修复4：合约调用支持自定义Gas Limit ==========
        public RawTransaction buildTx(BigInteger nonce, String to, String data, BigInteger customGasLimit) {
            requireWritable();
            BigInteger finalGasLimit = (customGasLimit != null && customGasLimit.compareTo(BigInteger.ZERO) > 0)
                    ? customGasLimit
                    : gasLimit;
            return RawTransaction.createTransaction(
                    nonce, gasPrice, finalGasLimit, to, data
            );
        }

        public RawTransaction buildTx(BigInteger nonce, String to, String data) {
            requireWritable();
//            System.out.println("gasPrice: "+gasPrice);
//            System.out.println("gasLimit: "+gasLimit);
            return RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, to, data
            );
        }

        public RawTransaction buildNativeTx(BigInteger nonce, String to, BigInteger valueWei) {
            requireWritable();
            return RawTransaction.createEtherTransaction(
                    nonce, gasPrice, BigInteger.valueOf(21_000),
                    to, valueWei
            );
        }

        public String sign(RawTransaction tx) {
            requireWritable();
            byte[] signed = TransactionEncoder.signMessage(tx, chainId, credentials);
            return Numeric.toHexString(signed);
        }
    }

    public final Signer signer = new Signer();

    // ============================================================
    // Sender
    // ============================================================
    public class Sender {

        @SneakyThrows
        public String send(String hex) {

            requireWritable();

            EthSendTransaction r = web3.ethSendRawTransaction(hex).send();

            if (r.hasError()) {

                ErrorFactory.throwError("Evm链 发送失败: " + r.getError().getMessage());
            }

            return r.getTransactionHash();
        }

    }

    public final Sender sender = new Sender();

    // ============================================================
    // SendTx
    // ============================================================
    public SendTx send(Function fn, String contract) {

        if (credentials == null) {
            throw new RuntimeException("当前 Web3Unit 为 read-only 模式，无法执行写操作！");
        }
        return new SendTx(fn, contract, credentials.getAddress());

    }

    public class SendTx {
        private final Function fn;
        private final String contract;
        private final String from;

        public SendTx(Function fn, String contract, String from) {
            this.fn = fn;
            this.contract = contract;
            this.from = from;
        }

        public String sync() {
            try {
                BigInteger nonce = client.getNonce(from);
                String data = FunctionEncoder.encode(fn);
                RawTransaction tx = signer.buildTx(nonce, contract, data);
                Web3RunWatchService service = Wx.Web3RunWatchService;
                String hash = sender.send(signer.sign(tx));
                Web3RunWatch web3RunWatch = new Web3RunWatch();
                web3RunWatch.setHash(hash);
                web3RunWatch.setHashState("Loading");
                web3RunWatch.setActionAddress(from);
                web3RunWatch.setContractAddress(contract);
                web3RunWatch.setFunctionName(fn.getName());

                String uid = ReqContextHolder.quickGet("uid");
                String contractTypeStr = "";
                String workId = ReqContextHolder.quickGet("workId");
                web3RunWatch.setActionUid(uid);
                web3RunWatch.setContractWorkId(workId);

                List<Type> inputParameters = fn.getInputParameters();
                ArrayList<String> objects = new ArrayList<>();
                inputParameters.forEach(item -> {
                    Object value = item.getValue();
                    objects.add(value + "");
                });
                web3RunWatch.setFunctionParam(String.join(",", objects));
                log.info("合约地址: {} ; 执行方法: {} ;", contract, fn.getName());
                log.info("参数: {} ;", web3RunWatch.getFunctionParam());
                if (service != null) {
                    service.save(web3RunWatch);
                }
                log.info("hash: {} ;", hash);
                return hash;
            } finally {

            }
        }

        @SneakyThrows
        public String syncAndWait(long timeoutSeconds) {
            String hash = sync();
            long end = System.currentTimeMillis() + timeoutSeconds * 1000;

            while (System.currentTimeMillis() < end) {
                var receiptOpt = web3.ethGetTransactionReceipt(hash).send().getTransactionReceipt();
                if (receiptOpt.isPresent()) {
                    TransactionReceipt r = receiptOpt.get();
                    if (!"0x1".equals(r.getStatus())) {
                        throw new RuntimeException("交易失败: " + hash);
                    }
                    return hash;
                }
                Thread.sleep(1500);
            }
            throw new RuntimeException("等待交易确认超时: " + hash);
        }

    }

    // ============================================================
    // ReadOnly
    // ============================================================
    public class ReadOnly {


        @SneakyThrows
        public List<Type> read(Function fn, String contract) {

            String data = FunctionEncoder.encode(fn);

            EthCall call = web3.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            null,          // from = null（非常关键）
                            contract,
                            data
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();
            if (call.isReverted() || call.getValue() == null || call.getValue().equals("0x")) {
                return Collections.emptyList();
            }

            return org.web3j.abi.FunctionReturnDecoder.decode(
                    call.getValue(),
                    fn.getOutputParameters()
            );
        }

        /**
         * 读取合约静态参数-单个
         */
        public <T> T readStaticParam(String paramName, String contractAddress, Class<T> clazz) {
            TypeReference<?> evmValueType = null;
            if (clazz.equals(String.class)) {
                evmValueType = EvmFun.ADDRESS;
            }
            if (clazz.equals(BigInteger.class)) {
                evmValueType = EvmFun.UINT256;
            }
            if (clazz.equals(Boolean.class)) {
                evmValueType = EvmFun.BOOL;
            }
            ErrorFactory.throwError(evmValueType == null, "未配置的类型");
            var function = EvmFun.of(paramName)
                    .withOutputs(evmValueType)
                    .build();
            List<Type> result = read(function, contractAddress);
            if (result == null || result.isEmpty()) {
                return null;
            }
            return clazz.cast(result.get(0).getValue());
        }

        /**
         * 根据用户地址读取参数-单个
         */
        public <T> T readUserParam(String paramName, String contractAddress, Class<T> clazz, String userAddress) {
            TypeReference<?> evmValueType = null;
            if (clazz.equals(String.class)) {
                evmValueType = EvmFun.ADDRESS;
            }
            if (clazz.equals(BigInteger.class)) {
                evmValueType = EvmFun.UINT256;
            }
            if (clazz.equals(Boolean.class)) {
                evmValueType = EvmFun.BOOL;
            }
            ErrorFactory.throwError(evmValueType == null, "未配置的类型");
            var function = EvmFun.of(paramName)
                    .withInputs(new Address(userAddress))
                    .withOutputs(evmValueType)
                    .build();
            List<Type> result = read(function, contractAddress);
            if (result == null || result.isEmpty()) {
                return null;
            }
            return clazz.cast(result.get(0).getValue());
        }


    }


    public final ReadOnly readOnly = new ReadOnly();

    // ============================================================
    // Checker - 事件校验 & 哈希校验
    // ============================================================
    public class Checker {

        @SneakyThrows
        public EvmHashResult checkHash(String hash, String eventName, TypeReference<?>... params) {

            TransactionReceipt r1 =
                    web3.ethGetTransactionReceipt(hash).send().getResult();

            Transaction r2 =
                    web3.ethGetTransactionByHash(hash).send().getResult();

            if (r1 == null || r2 == null) return null;
            if (!"0x1".equals(r1.getStatus())) return null;

            List<org.web3j.protocol.core.methods.response.Log> logs = r1.getLogs();

            EvmHashResult res =
                    new EvmHashResult(r1.getFrom(), r1.getTo(), r2.getValue(), logs);

            if (eventName == null) return res;

            String eventCode = EventEncoder.encode(new Event(eventName, Arrays.asList(params)));

            for (org.web3j.protocol.core.methods.response.Log logEntry : logs) {
                if (!logEntry.getTopics().isEmpty() &&
                        logEntry.getTopics().get(0).equals(eventCode)) {
                    res.setEventData(logEntry.getData());
                    res.setEventTopics(logEntry.getTopics());

                    return res;
                }
            }
            return null;
        }

        @SneakyThrows
        public boolean isTxSuccess(String txHash) {

            if (txHash == null || txHash.isEmpty()) {
                return false;
            }

            EthGetTransactionReceipt resp =
                    web3.ethGetTransactionReceipt(txHash).send();

            Optional<TransactionReceipt> receiptOpt = resp.getTransactionReceipt();

            // 1️⃣ 交易还没被打包
            if (!receiptOpt.isPresent()) {
                return false;
            }

            TransactionReceipt receipt = receiptOpt.get();

            // 2️⃣ status == 0x1 才是真正成功
            return "0x1".equalsIgnoreCase(receipt.getStatus());
        }
    }

    public final Checker checker = new Checker();

    // ============================================================
    //                哈希校验（Token）
    // ============================================================
    public Web3HashCheckResult checkTokenHash(
            Web3Coin coin,
            String hash,
            String expectFrom,
            String expectTo,
            BigDecimal expectAmount
    ) {
        Web3HashCheckResult ret = new Web3HashCheckResult();
        ret.setHash(hash);


        EvmHashResult tx = checker.checkHash(
                hash,
                "Transfer",
                EvmFun.ADDRESS,
                EvmFun.ADDRESS,
                EvmFun.UINT256
        );

        if (tx == null) {
            ret.fail("交易不存在或失败");
            return ret;
        }

        String realFrom = EvmHashResult.topicToAddress(tx.getEventTopics().get(1));
        String realTo = EvmHashResult.topicToAddress(tx.getEventTopics().get(2));
        BigDecimal realAmount = EvmHashResult.accuracyReversal(
                tx.getDataBigInteger(0),
                coin.getDecimals()
        );

        ret.setFrom(realFrom);
        ret.setTo(realTo);
        ret.setAmount(realAmount);
        if (expectFrom != null && !expectFrom.isEmpty() &&
                !expectFrom.equalsIgnoreCase(realFrom)) {
            ret.fail("fromAddress 不一致");
            return ret;
        }

        if (expectTo != null && !expectTo.isEmpty() &&
                !expectTo.equalsIgnoreCase(realTo)) {
            ret.fail("toAddress 不一致");
            return ret;
        }

        if (expectAmount != null &&
                realAmount.compareTo(expectAmount) != 0) {
            ret.fail("转账金额不一致");
            return ret;
        }

        if (!coin.getTokenAddress().equalsIgnoreCase(tx.getTo())) {
            ret.fail("Token 合约地址不一致");
            return ret;
        }

        ret.ok();
        return ret;
    }

    // ============================================================
    //                哈希校验（主币）
    // ============================================================
    public Web3HashCheckResult checkMainHash(
            Web3Coin coin,
            String hash,
            String expectFrom,
            String expectTo,
            BigDecimal expectAmount
    ) {
        Web3HashCheckResult ret = new Web3HashCheckResult();
        ret.setHash(hash);

        if (!coin.isMainCoin()) {
            ret.fail("该币不是主币");
            return ret;
        }

        EvmHashResult tx = checker.checkHash(hash, null);

        if (tx == null) {
            ret.fail("交易不存在或失败");
            return ret;
        }

        String realFrom = tx.getFrom();
        String realTo = tx.getTo();
        BigDecimal realAmount = EvmHashResult.accuracyReversal(tx.getVal(), coin.getDecimals());

        ret.setFrom(realFrom);
        ret.setTo(realTo);
        ret.setAmount(realAmount);

        if (expectFrom != null && !expectFrom.isEmpty() &&
                !expectFrom.equalsIgnoreCase(realFrom)) {
            ret.fail("fromAddress 不一致");
            return ret;
        }

        if (expectTo != null && !expectTo.isEmpty() &&
                !expectTo.equalsIgnoreCase(realTo)) {
            ret.fail("toAddress 不一致");
            return ret;
        }

        if (expectAmount != null &&
                realAmount.compareTo(expectAmount) != 0) {
            ret.fail("金额不一致");
            return ret;
        }

        ret.ok();
        return ret;
    }

    // ============================================================
    // Wei 工具
    // ============================================================
    public static BigDecimal amountFromWei(BigInteger wei, int decimals) {
        return new BigDecimal(wei)
                .divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.DOWN);
    }
}
