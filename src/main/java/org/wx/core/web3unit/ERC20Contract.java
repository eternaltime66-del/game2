package org.wx.core.web3unit;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.wx.core.web3unit.BigIntegerDecUtils;
import org.wx.core.web3unit.Web3Config;
import org.wx.core.web3unit.evm.EvmFun;
import org.wx.core.web3unit.evm.EvmUnit;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

/**
 * ERC20代币合约调用类
 * 支持流畅的链式调用API，与WorkTradePoolContract风格统一
 * 使用方式：
 * 1. 读操作: ERC20Contract.at("代币地址").read().symbol().val()
 * 2. 写操作: ERC20Contract.at("代币地址").run("私钥").approve("授权地址", 100)
 */
@Slf4j
public class ERC20Contract {
    // ========== 合约常量 ==========
    // 默认Gas配置（BSC链适配）
    public static final BigInteger DEFAULT_GAS_PRICE = BigInteger.valueOf(50_000_000L); // 3 Gwei
    public static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(200_000);      // ERC20操作Gas限制
    public static final int DEFAULT_DECIMALS = 18; // 默认小数位数

    // ========== 静态工厂方法 ==========
    /**
     * 核心入口：指定代币地址创建ERC20合约实例
     *
     * @param tokenAddress 代币合约地址
     * @return ERC20合约实例
     */
    public static ContractInstance at(String tokenAddress) {
        return new ContractInstance(tokenAddress);
    }

    // ========== 合约实例封装 ==========
    @Data
    public static class ContractInstance {
        private final String tokenAddress;
        private Web3j web3j;
        private Web3Config web3Config;

        /**
         * 初始化默认Web3配置（BSC主网）
         */
        private void initDefaultConfig() {
            if (web3Config == null) {
                web3Config = new Web3Config();
                String url = "https://bsc-dataseed1.binance.org/";
                web3j = Web3j.build(new HttpService(url));
                web3Config.setChainId(56);
                web3Config.setNodeUrl(url);
                web3Config.setWeb3j(web3j);
                // 占位私钥（只读操作使用）
                web3Config.setPrivateKey("0000000000000000000000000000000000000000000000000000000000000001");
            }
        }

        /**
         * 只读操作入口
         *
         * @return 只读操作器
         */
        public ReadOperations read() {
            initDefaultConfig();
            EvmUnit evmUnit = createEvmUnit(web3Config);
            return new ReadOperations(evmUnit, tokenAddress);
        }

        /**
         * 写操作入口（指定执行人私钥）
         *
         * @param privateKey 执行人私钥
         * @return 写操作器
         */
        public WriteOperations run(String privateKey) {
            initDefaultConfig();
            web3Config.setPrivateKey(privateKey);
            EvmUnit evmUnit = createEvmUnit(web3Config);

            return new WriteOperations(evmUnit, tokenAddress);
        }

        /**
         * 自定义Web3配置
         *
         * @param web3Config 自定义配置
         * @return 当前实例（链式调用）
         */
        public ContractInstance withConfig(Web3Config web3Config) {
            this.web3Config = web3Config;
            this.web3j = web3Config.getWeb3j();
            return this;
        }

        /**
         * 创建EvmUnit实例并设置默认Gas配置
         */
        private EvmUnit createEvmUnit(Web3Config config) {
            EvmUnit evmUnit = new EvmUnit(config);
            evmUnit.setGasPrice(DEFAULT_GAS_PRICE);
            evmUnit.setGasLimit(DEFAULT_GAS_LIMIT);
            return evmUnit;
        }
    }

    // ========== 只读操作器 ==========
    @RequiredArgsConstructor
    public static class ReadOperations {
        private final EvmUnit evmUnit;
        private final String tokenAddress;
        private final HashMap<String, TokenParam<?>> paramMap = new HashMap<>();

        // ========== ERC20核心参数定义 ==========
        public final TokenParam<String> symbol = new TokenParam<>("symbol", String.class, "代币符号（如USDT、ETH）");
        public final TokenParam<String> name = new TokenParam<>("name", String.class, "代币名称（如Tether USD）");
        public final TokenParam<Integer> decimals = new TokenParam<>("decimals", Integer.class, "代币小数位数");
        public final TokenParam<BigInteger> totalSupply = new TokenParam<>("totalSupply", BigInteger.class, "代币总供应量");

        // ========== 参数读取内部类 ==========
        @Data
        public class TokenParam<T> {
            private final String method;       // 合约方法名
            private final Class<T> clazz;      // 返回值类型
            private final String message;      // 参数备注

            public TokenParam(String method, Class<T> clazz, String message) {
                this.method = method;
                this.clazz = clazz;
                this.message = message;
                paramMap.put(method, this);
            }

            /**
             * 读取参数值
             *
             * @return 参数值
             */
            public T val() {
                try {
                    EvmFun evmFun = buildEvmFun(method, clazz);
                    List<Type> result = evmUnit.readOnly.read(evmFun.build(), tokenAddress);

                    if (result.isEmpty()) {
                        throw new RuntimeException("读取" + method + "失败，返回值为空");
                    }

                    return convertResult(result.get(0), clazz);
                } catch (Exception e) {
                    log.error("读取ERC20参数失败，代币地址={}，参数={}，备注={}", tokenAddress, method, message, e);
                    throw new RuntimeException("读取ERC20参数[" + method + "]失败，备注：" + message, e);
                }
            }

            /**
             * 读取数值型参数并转换为带精度的小数（仅适用于BigInteger类型）
             *
             * @return 带精度的小数
             */
            public BigDecimal decimals() {
                if (clazz != BigInteger.class) {
                    throw new UnsupportedOperationException("仅支持BigInteger类型参数的精度转换");
                }

                try {
                    BigInteger rawValue = (BigInteger) val();
                    int decimals = readOps().decimals.val();
                    return BigIntegerDecUtils.decimals(rawValue, decimals);
                } catch (Exception e) {
                    log.error("转换ERC20参数精度失败，代币地址={}，参数={}", tokenAddress, method, e);
                    throw new RuntimeException("转换ERC20参数[" + method + "]精度失败", e);
                }
            }
        }

        // ========== 私有工具方法 ==========
        private EvmFun buildEvmFun(String method, Class<?> clazz) {
            EvmFun evmFun = EvmFun.of(method);

            if (clazz == String.class) {
                evmFun.withOutputs(new TypeReference<Utf8String>() {});
            } else if (clazz == Integer.class) {
                evmFun.withOutputs(new TypeReference<Uint8>() {});
            } else if (clazz == BigInteger.class) {
                evmFun.withOutputs(new TypeReference<Uint256>() {});
            } else if (clazz == Boolean.class) {
                evmFun.withOutputs(new TypeReference<Bool>() {});
            }

            return evmFun;
        }

        @SuppressWarnings("unchecked")
        private <T> T convertResult(Type result, Class<T> clazz) {
            Object value = result.getValue();

            if (clazz == Integer.class && value instanceof BigInteger) {
                return (T) Integer.valueOf(((BigInteger) value).intValue());
            } else {
                return (T) value;
            }
        }

        private ReadOperations readOps() {
            return this;
        }

        // ========== 通用参数获取方法 ==========
        public TokenParam<?> param(String key) {
            return paramMap.get(key);
        }

        // ========== ERC20核心只读方法 ==========
        /**
         * 查询用户代币余额
         *
         * @param userAddress 用户地址
         * @return 代币余额（原始值，未转换精度）
         */
        public BigInteger balanceOf(String userAddress) {
            try {
                EvmFun evmFun = EvmFun.of("balanceOf")
                        .withInputs(new Address(userAddress))
                        .withOutputs(new TypeReference<Uint256>() {});

                List<Type> result = evmUnit.readOnly.read(evmFun.build(), tokenAddress);

                if (result.isEmpty()) {
                    throw new RuntimeException("查询余额失败，返回值为空");
                }

                return (BigInteger) result.get(0).getValue();
            } catch (Exception e) {
                log.error("查询代币余额失败，代币地址={}，用户地址={}", tokenAddress, userAddress, e);
                throw new RuntimeException("查询代币余额失败", e);
            }
        }

        /**
         * 查询用户代币余额（带精度转换）
         *
         * @param userAddress 用户地址
         * @return 转换后的余额
         */
        public BigDecimal balanceOfDecimals(String userAddress) {
            BigInteger rawBalance = balanceOf(userAddress);
            int decimals = readOps().decimals.val();
            return BigIntegerDecUtils.decimals(rawBalance, decimals);
        }

        /**
         * 查询授权额度
         *
         * @param owner   授权方地址
         * @param spender 被授权方地址
         * @return 授权额度（原始值）
         */
        public BigInteger allowance(String owner, String spender) {
            try {
                EvmFun evmFun = EvmFun.of("allowance")
                        .withInputs(new Address(owner), new Address(spender))
                        .withOutputs(new TypeReference<Uint256>() {});

                List<Type> result = evmUnit.readOnly.read(evmFun.build(), tokenAddress);

                if (result.isEmpty()) {
                    throw new RuntimeException("查询授权额度失败，返回值为空");
                }

                return (BigInteger) result.get(0).getValue();
            } catch (Exception e) {
                log.error("查询授权额度失败，代币地址={}，授权方={}，被授权方={}", tokenAddress, owner, spender, e);
                throw new RuntimeException("查询授权额度失败", e);
            }
        }

        /**
         * 查询授权额度（带精度转换）
         *
         * @param owner   授权方地址
         * @param spender 被授权方地址
         * @return 转换后的授权额度
         */
        public BigDecimal allowanceDecimals(String owner, String spender) {
            BigInteger rawAllowance = allowance(owner, spender);
            int decimals = readOps().decimals.val();
            return BigIntegerDecUtils.decimals(rawAllowance, decimals);
        }

        /**
         * 批量获取代币基础信息
         *
         * @return 包含所有基础信息的HashMap
         */
        public HashMap<String, Object> getTokenInfo() {
            HashMap<String, Object> info = new HashMap<>();
            info.put("address", tokenAddress);
            info.put("symbol", symbol.val());
            info.put("name", name.val());
            info.put("decimals", decimals.val());
            info.put("totalSupply", totalSupply.val());
            info.put("totalSupplyDecimals", totalSupply.decimals());
            return info;
        }
    }

    // ========== 写操作器 ==========
    @RequiredArgsConstructor
    public static class WriteOperations {
        private final EvmUnit evmUnit;
        private final String tokenAddress;

        // ========== ERC20核心写方法 ==========
        /**
         * 授权代币额度
         *
         * @param spender 被授权方地址
         * @param amount  授权额度（原始值，已处理精度）
         * @return 交易哈希
         */
        public String approve(String spender, BigInteger amount) {
            try {
                EvmFun evmFun = EvmFun.of("approve")
                        .withInputs(new Address(spender), new Uint256(amount));

                return sendTransaction(evmFun);
            } catch (Exception e) {
                log.error("授权代币失败，代币地址={}，被授权方={}，额度={}", tokenAddress, spender, amount, e);
                throw new RuntimeException("授权代币失败", e);
            }
        }

        /**
         * 授权代币额度（自动处理精度）
         *
         * @param spender 被授权方地址
         * @param amount  授权额度（小数形式）
         * @return 交易哈希
         */
        public String approve(String spender, BigDecimal amount) {
            try {
                // 自动获取小数位数并转换
                int decimals = ERC20Contract.at(tokenAddress).read().decimals.val();
                BigInteger rawAmount = BigIntegerDecUtils.toWei(amount, decimals);
                return approve(spender, rawAmount);
            } catch (Exception e) {
                log.error("授权代币（带精度）失败，代币地址={}，被授权方={}，额度={}", tokenAddress, spender, amount, e);
                throw new RuntimeException("授权代币（带精度）失败", e);
            }
        }

        /**
         * 转账代币
         *
         * @param to     接收方地址
         * @param amount 转账金额（原始值）
         * @return 交易哈希
         */
        public String transfer(String to, BigInteger amount) {
            try {
                EvmFun evmFun = EvmFun.of("transfer")
                        .withInputs(new Address(to), new Uint256(amount));

                return sendTransaction(evmFun);
            } catch (Exception e) {
                log.error("转账代币失败，代币地址={}，接收方={}，金额={}", tokenAddress, to, amount, e);
                throw new RuntimeException("转账代币失败", e);
            }
        }

        /**
         * 转账代币（自动处理精度）
         *
         * @param to     接收方地址
         * @param amount 转账金额（小数形式）
         * @return 交易哈希
         */
        public String transfer(String to, BigDecimal amount) {
            try {
                int decimals = ERC20Contract.at(tokenAddress).read().decimals.val();
                BigInteger rawAmount = BigIntegerDecUtils.toWei(amount, decimals);
                return transfer(to, rawAmount);
            } catch (Exception e) {
                log.error("转账代币（带精度）失败，代币地址={}，接收方={}，金额={}", tokenAddress, to, amount, e);
                throw new RuntimeException("转账代币（带精度）失败", e);
            }
        }

        /**
         * 授权转账
         *
         * @param from   转出方地址
         * @param to     接收方地址
         * @param amount 转账金额（原始值）
         * @return 交易哈希
         */
        public String transferFrom(String from, String to, BigInteger amount) {
            try {
                EvmFun evmFun = EvmFun.of("transferFrom")
                        .withInputs(new Address(from), new Address(to), new Uint256(amount));

                return sendTransaction(evmFun);
            } catch (Exception e) {
                log.error("授权转账失败，代币地址={}，转出方={}，接收方={}，金额={}", tokenAddress, from, to, amount, e);
                throw new RuntimeException("授权转账失败", e);
            }
        }

        // ========== 工具方法 ==========
        /**
         * 发送交易并返回哈希
         */
        private String sendTransaction(EvmFun evmFun) {
            return evmUnit.send(evmFun.build(), tokenAddress).sync();
        }

        /**
         * 查询交易是否成功
         */
        public boolean isTransactionSuccess(String txHash) {
            try {
                EthGetTransactionReceipt receipt = evmUnit.getWeb3()
                        .ethGetTransactionReceipt(txHash)
                        .send();

                if (receipt.getTransactionReceipt().isEmpty()) {
                    return false;
                }

                TransactionReceipt txReceipt = receipt.getTransactionReceipt().get();
                return txReceipt.getStatus() != null && txReceipt.getStatus().equals("0x1");
            } catch (Exception e) {
                log.error("查询交易状态失败，交易哈希={}", txHash, e);
                return false;
            }
        }

        /**
         * 兼容USDT的授权方式（先清零再授权）
         *
         * @param spender 被授权方地址
         * @param amount  最终授权额度
         * @return 最终授权交易哈希
         */
        @SneakyThrows
        public String approveUsdtCompatible(String spender, BigDecimal amount) {
            String owner = evmUnit.getCredentials().getAddress();
            ReadOperations readOps = ERC20Contract.at(tokenAddress).read();

            // 查询当前授权额度
            BigDecimal currentAllowance = readOps.allowanceDecimals(owner, spender);
            log.info("当前授权额度：{} {}", currentAllowance, readOps.symbol.val());

            // 授权足够直接返回
            if (currentAllowance.compareTo(amount) >= 0) {
                log.info("授权额度充足，无需操作");
                return null;
            }

            // 先清零
            if (currentAllowance.compareTo(BigDecimal.ZERO) > 0) {
                log.info("先清零授权额度");
                String zeroTx = approve(spender, BigDecimal.ZERO);
                if (!isTransactionSuccess(zeroTx)) {
                    throw new RuntimeException("清零授权额度失败，交易哈希：" + zeroTx);
                }
                Thread.sleep(1000L); // 等待上一笔交易确认
            }

            // 重新授权
            log.info("授权最终额度：{}", amount);
            String approveTx = approve(spender, amount);
            if (!isTransactionSuccess(approveTx)) {
                throw new RuntimeException("授权额度失败，交易哈希：" + approveTx);
            }

            return approveTx;
        }
    }

    // ========== 兼容原有直接实例化的构造方法（保证旧代码可用） ==========
    private final EvmUnit evmUnit;
    private final String token;

    public ERC20Contract(EvmUnit evmUnit, String token) {
        this.evmUnit = evmUnit;
        this.token = token;
    }

    // ========== 兼容原有方法（适配新的链式调用） ==========
    public String symbol() {
        return ERC20Contract.at(token).read().symbol.val();
    }

    public String name() {
        return ERC20Contract.at(token).read().name.val();
    }

    public int decimals() {
        return ERC20Contract.at(token).read().decimals.val();
    }

    /** allowance(owner, spender) */
    public BigInteger allowance(String owner, String spender) {
        EvmFun fn = EvmFun.of("allowance")
                .withInputs(new Address(owner), new Address(spender))
                .withOutputs(new TypeReference<Uint256>() {});
        List<Type> r = evmUnit.readOnly.read(fn.build(), token);
        return r.isEmpty() ? BigInteger.ZERO : (BigInteger) r.get(0).getValue();
    }

    /** approve(spender, amount) */
    public String approve(String spender, BigInteger amount) {
        EvmFun fn = EvmFun.of("approve")
                .withInputs(new Address(spender), new Uint256(amount));
        return evmUnit.send(fn.build(), token).sync();
    }

    // ========== 使用示例 ==========
    public static void main(String[] args) {
        // USDT BSC地址示例
        String usdtAddress = "0x55d398326f99059ff775485246999027b3197955";
        String privateKey = "你的私钥";
        String userAddress = "你的钱包地址";
        String spenderAddress = "授权的合约地址";

        // 1. 只读操作示例
        ReadOperations readOps = ERC20Contract.at(usdtAddress).read();
        
        // 读取基础信息
        String symbol = readOps.symbol.val();
        String name = readOps.name.val();
        int decimals = readOps.decimals.val();
        System.out.println("代币符号：" + symbol); // 输出 USDT
        System.out.println("代币名称：" + name);   // 输出 Tether USD
        System.out.println("小数位数：" + decimals); // 输出 18

        // 读取余额
        BigDecimal balance = readOps.balanceOfDecimals(userAddress);
        System.out.println("用户余额：" + balance + " " + symbol);

        // 读取授权额度
        BigDecimal allowance = readOps.allowanceDecimals(userAddress, spenderAddress);
        System.out.println("授权额度：" + allowance + " " + symbol);

        // 批量获取代币信息
        HashMap<String, Object> tokenInfo = readOps.getTokenInfo();
        System.out.println("代币完整信息：" + tokenInfo);

        // 2. 写操作示例
        /*
        WriteOperations writeOps = ERC20Contract.at(usdtAddress).run(privateKey);
        
        // 授权100 USDT
        String approveTx = writeOps.approve(spenderAddress, BigDecimal.valueOf(100));
        System.out.println("授权交易哈希：" + approveTx);
        System.out.println("交易是否成功：" + writeOps.isTransactionSuccess(approveTx));
        
        // USDT兼容模式授权
        String usdtApproveTx = writeOps.approveUsdtCompatible(spenderAddress, BigDecimal.valueOf(1000));
        System.out.println("USDT授权交易哈希：" + usdtApproveTx);
        
        // 转账10 USDT
        String transferTx = writeOps.transfer("接收方地址", BigDecimal.valueOf(10));
        System.out.println("转账交易哈希：" + transferTx);
        */
    }
}