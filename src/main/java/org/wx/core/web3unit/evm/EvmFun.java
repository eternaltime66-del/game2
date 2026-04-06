package org.wx.core.web3unit.evm;


import lombok.Getter;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.wx.core.wxBase.factory.ErrorFactory;

import java.math.BigInteger;
import java.util.*;

/**
 * ABI 构建器（修复版）
 *
 * 修复内容：
 * 1. String 不再默认识别为 Address
 * 2. 地址必须以 0x 开头 + 长度 42 位
 * 3. 金额必须为 BigInteger（禁止字符串）
 * 4. 自动推断 BigInteger → Uint256
 */
@Getter
public class EvmFun {

    private String functionName;
    private final List<Type> inputs = new ArrayList<>();
    private final List<TypeReference<?>> outputs = new ArrayList<>();

    public static final TypeReference<Address> ADDRESS = new TypeReference<Address>() {};
    public static final TypeReference<Uint256> UINT256 = new TypeReference<Uint256>() {};
    public static final TypeReference<Uint8> UINT8 = new TypeReference<>() {};
    public static final TypeReference<Utf8String> STRING = new TypeReference<>() {};
    public static final TypeReference<Bool> BOOL = new TypeReference<>() {};
    /** 创建函数 */
    public static EvmFun of(String name) {
        EvmFun f = new EvmFun();
        f.functionName = name;
        return f;
    }

    /** 输入参数 */
    public EvmFun withInputs(Object... params) {
        inputs.addAll(convertParams(params));
        return this;
    }
    public EvmFun withInputs(Type<?>... outs) {
        inputs.addAll(Arrays.asList(outs));
        return this;
    }

    /** 输出参数 */
    public EvmFun withOutputs(TypeReference<?>... outs) {
        outputs.addAll(Arrays.asList(outs));
        return this;
    }

    /** 生成 Function */
    public Function build() {
        return new Function(functionName, inputs, outputs);
    }

    /** 兼容旧版：无返回值 */
    public static Function create(String name, Object... params) {
        return new Function(name, convertParams(params), Collections.emptyList());
    }

    /** ---------------- 参数类型推断核心逻辑（修复版） ---------------- */
    private static List<Type> convertParams(Object... params) {
        List<Type> types = new ArrayList<>();

        for (Object param : params) {

            if (param == null) {
                ErrorFactory.throwError("ABI 参数不能为 null");
            }

            // List 处理（数组）
            if (param instanceof List<?>) {
                List<?> list = (List<?>) param;

                ErrorFactory.throwError(list.isEmpty(), "ABI 数组不能为空");

                Object first = list.get(0);

                // 地址数组
                if (first instanceof String && isAddress((String) first)) {
                    List<Address> arr = new ArrayList<>();
                    for (Object o : list) arr.add(new Address((String) o));
                    types.add(new DynamicArray<>(Address.class, arr));
                    continue;
                }

                // Uint256 数组
                if (first instanceof BigInteger) {
                    List<Uint256> arr = new ArrayList<>();
                    for (Object o : list) arr.add(new Uint256((BigInteger) o));
                    types.add(new DynamicArray<>(Uint256.class, arr));
                    continue;
                }

                ErrorFactory.throwError("List 类型不支持：" + first.getClass().getSimpleName());
            }

            // 单个地址
            if (param instanceof String && isAddress((String) param)) {
                types.add(new Address((String) param));
                continue;
            }

            // 单个金额
            if (param instanceof BigInteger) {
                types.add(new Uint256((BigInteger) param));
                continue;
            }

            // 普通字符串（不当地址）
            if (param instanceof String) {
                types.add(new Utf8String((String) param));
                continue;
            }

            ErrorFactory.throwError("参数类型不支持：" + param.getClass().getSimpleName());
        }

        return types;
    }

    public static <T extends Type> TypeReference<T> createTypeReference(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("类型字符串不能为空");
        }

        String type = typeStr.trim();
        boolean isArray = type.endsWith("[]");
        String baseType = isArray ? type.substring(0, type.length() - 2) : type;

        // 创建基础类型的 TypeReference
        TypeReference<?> baseRef = switch (baseType.toLowerCase()) {
            case "string" -> STRING;
            case "address" -> ADDRESS;
            case "uint256" -> UINT256;
            case "uint8" -> UINT8;
            case "bool" -> BOOL;
            default -> throw new UnsupportedOperationException("不支持的类型: " + baseType);
        };

        // 如果是数组类型，创建数组的 TypeReference
        if (isArray) {
            return (TypeReference<T>) new TypeReference<DynamicArray<?>>() {};
        }

        return (TypeReference<T>) baseRef;
    }

    /** 判断是否为 EVM 地址 */
    private static boolean isAddress(String str) {
        return str != null && str.startsWith("0x") && str.length() == 42;
    }
}
