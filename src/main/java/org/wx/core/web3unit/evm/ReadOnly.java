package org.wx.core.web3unit.evm;

import lombok.Data;
import lombok.SneakyThrows;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ReadOnly {

    // 补充必要的成员变量（原代码中使用但未定义）
    private final Web3j web3;
    private final Credentials credentials;
    private final BigInteger gasPrice;
    private final BigInteger gasLimit;

    // 构造函数初始化成员变量
    public ReadOnly(Web3j web3, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        this.web3 = web3;
        this.credentials = credentials;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
    }

    // 内部Contract调用类
    class Call extends Contract {
        protected Call(String contract) {
            super(contract, web3, credentials, gasPrice, gasLimit);
        }

        public RemoteFunctionCall<List<Type>> call(Function function) {
            return executeRemoteCallMultipleValueReturn(function);
        }
    }

    /**
     * 核心读取方法 - 调用合约只读方法并返回原始Type列表
     */
    @SneakyThrows
    public List<Type> read(Function fn, String contract) {
        // 参数校验
        if (fn == null || contract == null || contract.trim().isEmpty()) {
            throw new IllegalArgumentException("Function and contract address cannot be null or empty");
        }

        String data = FunctionEncoder.encode(fn);

        EthCall call = web3.ethCall(
                Transaction.createEthCallTransaction(
                        null,          // from = null（非常关键）
                        contract,
                        data
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        // 处理调用结果
        if (call.isReverted() || call.getValue() == null || call.getValue().equals("0x")) {
            return Collections.emptyList();
        }

        return FunctionReturnDecoder.decode(
                call.getValue(),
                fn.getOutputParameters()
        );
    }

    /**
     * 简化的读取方法 - 确保返回非空（无结果返回null）
     */
    @SneakyThrows
    public List<Type> readSingle(Function fn, String contract) {
        List<Type> result = read(fn, contract);
        return (result == null || result.isEmpty()) ? null : result;
    }

    /**
     * 结果封装类 - 提供常用类型的便捷获取方法
     */
    @Data
    public static class OnlyReadResult {
        private List<Type> list;
        private String contract;
        private String param;
        private Integer index = 0; // 默认取第一个元素

        /**
         * 获取指定索引的地址类型值
         */
        public String toAddress() {
            Type type = getValidType();
            if (type instanceof Address) {
                return ((Address) type).getValue();
            }
            throw new ClassCastException("Type at index " + index + " is not Address type");
        }

        /**
         * 获取指定索引的布尔类型值
         */
        public Boolean toBoolean() {
            Type type = getValidType();
            if (type instanceof Bool) {
                return ((Bool) type).getValue();
            }
            throw new ClassCastException("Type at index " + index + " is not Boolean type");
        }

        /**
         * 获取指定索引的整型值（适配Uint256/Int256等）
         */
        public BigInteger toBigInteger() {
            Type type = getValidType();
            if (type instanceof Uint256) {
                return ((Uint256) type).getValue();
            } else if (type instanceof Int256) {
                return ((Int256) type).getValue();
            } else if (type instanceof Uint) {
                return ((Uint) type).getValue();
            } else if (type instanceof Int) {
                return ((Int) type).getValue();
            }
            throw new ClassCastException("Type at index " + index + " is not Numeric type");
        }

        /**
         * 获取指定索引的长整型值
         */
        public Long toLong() {
            return toBigInteger().longValue();
        }

        /**
         * 获取指定索引的整型值
         */
        public Integer toInt() {
            return toBigInteger().intValue();
        }

        /**
         * 获取指定索引的字符串类型值
         */
        public String toStringValue() {
            Type type = getValidType();
            if (type instanceof Utf8String) {
                return ((Utf8String) type).getValue();
            }
            throw new ClassCastException("Type at index " + index + " is not String type");
        }

        /**
         * 获取指定索引的原始Type对象
         */
        public Type getTypeValue() {
            return getValidType();
        }

        /**
         * 安全获取指定索引的Type对象（包含边界检查）
         */
        private Type getValidType() {
            if (list == null || list.isEmpty()) {
                throw new IllegalStateException("Result list is null or empty");
            }
            if (index == null || index < 0 || index >= list.size()) {
                throw new IndexOutOfBoundsException(
                        String.format("Index %d out of bounds for result list of size %d; contract: %s; param: %s",
                                index, list.size(), contract, param)
                );
            }
            return list.get(index);
        }

        /**
         * 设置要读取的索引位置
         */
        public OnlyReadResult atIndex(Integer index) {
            this.index = index;
            return this; // 支持链式调用
        }
    }

    /**
     * 通用调用方法 - 返回封装后的结果对象
     */
    private OnlyReadResult single(
            String method,
            String contract,
            TypeReference<?> output,
            Object... inputs
    ) {
        // 构建Function对象（适配常用输入类型）
        Function fun = buildFunction(method, output, inputs);
        
        List<Type> types = readSingle(fun, contract);
        
        OnlyReadResult result = new OnlyReadResult();
        result.setList(types);
        result.setContract(contract);
        result.setParam(method + "(" + String.join(",", Objects.toString(inputs, "")) + ")");
        result.setIndex(0);
        
        return result;
    }

    /**
     * 构建Function对象（处理常用输入类型转换）
     */
    private Function buildFunction(String method, TypeReference<?> output, Object... inputs) {
        List<Type> inputParameters = new java.util.ArrayList<>();
        
        // 转换输入参数为web3j的Type类型
        for (Object input : inputs) {
            if (input instanceof String) {
                // 地址类型
                if (((String) input).startsWith("0x") && ((String) input).length() == 42) {
                    inputParameters.add(new Address((String) input));
                } else {
                    // 字符串类型
                    inputParameters.add(new Utf8String((String) input));
                }
            } else if (input instanceof Boolean) {
                inputParameters.add(new Bool((Boolean) input));
            } else if (input instanceof BigInteger) {
                inputParameters.add(new Uint256((BigInteger) input));
            } else if (input instanceof Long) {
                inputParameters.add(new Uint256(BigInteger.valueOf((Long) input)));
            } else if (input instanceof Integer) {
                inputParameters.add(new Uint256(BigInteger.valueOf((Integer) input)));
            } else if (input instanceof Type) {
                // 如果已经是Type类型，直接添加
                inputParameters.add((Type) input);
            } else {
                throw new IllegalArgumentException("Unsupported input type: " + input.getClass().getName());
            }
        }
        
        // 构建Function
        return new Function(
                method,
                inputParameters,
                Collections.singletonList(output)
        );
    }

    // ==================== 便捷的类型化读取方法 ====================

    /**
     * 读取布尔类型结果
     */
    public Boolean readBoolean(String method, String contract, Object... inputs) {
        return single(method, contract, TypeReference.create(Bool.class), inputs)
                .toBoolean();
    }

    /**
     * 读取BigInteger类型结果
     */
    public BigInteger readBigInteger(String method, String contract, Object... inputs) {
        return single(method, contract, TypeReference.create(Uint256.class), inputs)
                .toBigInteger();
    }

    /**
     * 读取Long类型结果
     */
    public Long readLong(String method, String contract, Object... inputs) {
        return single(method, contract, TypeReference.create(Uint256.class), inputs)
                .toLong();
    }

    /**
     * 读取Integer类型结果
     */
    public Integer readInt(String method, String contract, Object... inputs) {
        return single(method, contract, TypeReference.create(Uint256.class), inputs)
                .toInt();
    }

    /**
     * 读取地址类型结果
     */
    public String readAddress(String method, String contract, Object... inputs) {
        return single(method, contract, TypeReference.create(Address.class), inputs)
                .toAddress();
    }

    /**
     * 读取字符串类型结果
     */
    public String readString(String method, String contract, Object... inputs) {
        return single(method, contract, TypeReference.create(Utf8String.class), inputs)
                .toStringValue();
    }

    /**
     * 读取原始Type类型结果（适用于自定义类型）
     */
    public Type readType(String method, String contract, TypeReference<?> outputType, Object... inputs) {
        return single(method, contract, outputType, inputs)
                .getTypeValue();
    }

    /**
     * 读取多返回值结果（返回封装对象，手动指定索引读取）
     */
    public OnlyReadResult readMulti(String method, String contract, TypeReference<?> outputType, Object... inputs) {
        return single(method, contract, outputType, inputs);
    }
}