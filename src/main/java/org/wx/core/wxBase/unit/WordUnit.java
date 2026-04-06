package org.wx.core.wxBase.unit;

import cn.hutool.core.date.DateUtil;

import lombok.SneakyThrows;
import org.springframework.util.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 无心
 * @date 2021/5/20
 * @msg 备注:创建id工具类
 * @demo CreateId.java
 */
public class WordUnit {

    public static String ID() {
        String s = Long.toString(System.currentTimeMillis(), 36);
        return s;
    }

    public static String creatWord() {
        return Long.toString((long) (Math.random() * 36), 36);
    }

    /**
     * key :1 纯数字
     * :2 纯字母
     * :3 数字加字母
     */
    public static String randomKey(int len, int key) {
        String id = "";
        for (int x = 0; x < len; x++) {
            if (key == 1) {
                double v = Math.random() * 10;
                while (v < 1 && x == 0) {
                    v = Math.random() * 10;
                }
                id += String.valueOf((int) (v));
            }
            if (key == 2) {
                id += toLowerCase(Long.toString(((long) (Math.random() * 26) + (long) 10), 36));
            }
            if (key == 3) {
                id += toLowerCase(Long.toString((long) (Math.random() * 36), 36));
            }
        }
        return id;
    }

    public static String token() {
        return randomKey(8, 3);
    }

    public static String uid() {
        return randomKey(8, 1);
    }

    public String UserInviteCoed() {
        String uid = randomKey(8, 1);
        return uid;
    }

    public static String toLowerCase(String str) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if ('a' <= chars[i] && chars[i] <= 'z' && Math.random() * 2 > 1) {
                chars[i] -= 32;
            }
        }
        return String.valueOf(chars);
    }

    @SneakyThrows
    public static String HMACSHA256(String data, String key) {

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");

        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");

        sha256_HMAC.init(secret_key);

        byte[] array = sha256_HMAC.doFinal(data.getBytes("UTF-8"));

        StringBuilder sb = new StringBuilder();

        for (byte item : array) {

            sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));

        }

        return sb.toString();

    }


    public static String base64Set(String str) {
        byte[] bytes = str.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String base64Get(String str) {
        byte[] decoded = Base64.getDecoder().decode(str);
        return new String(decoded);
    }

    public static String mapJoin(Map<String, String> map) {
        return mapJoin(map, "=", "&");
    }

    public static <T> String mapJoin(Map<String, String> map, String and, String join) {
        AtomicReference<String> str = new AtomicReference<>("");
        map.forEach((key, val) -> {
            String baseStr = str.get();
            String thisStr = key + and + val;
            if (baseStr.length() != 0) {
                thisStr = join + thisStr;
            }
            baseStr += thisStr;
            str.set(baseStr);
        });
        return str.get();
    }


    public static String upperFirstCase(String str) {
        char[] chars = str.toCharArray();
        //首字母小写方法，大写会变成小写，如果小写首字母会消失
        chars[0] -= 32;
        return String.valueOf(chars);
    }

    public static String md5(String str) {
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }

    public static Integer toInt(Double val) {
        return BigDecimal.valueOf(val).setScale(0, RoundingMode.FLOOR).intValue();
    }

    public static String nowId(Integer len, Integer key) {
        return nowId()+randomKey(len, key);
    }

    public static String nowId() {
        return DateUtil.format(new Date(), "yyyyMMddHHMMss");
    }

    public static BigDecimal random(Number min, Number max) {
        BigDecimal minNum = new BigDecimal(min.toString());
        BigDecimal maxNum = new BigDecimal(max.toString());
        return random(minNum, maxNum);
    }

    public static BigDecimal random(BigDecimal minNum, BigDecimal maxNum) {
        return BigDecimal.valueOf(Math.random()).multiply(maxNum.subtract(minNum)).add(minNum);
    }

    public static String richText(String str) {
        if (str.indexOf("-*1*-") < 0) return str;
        return str.replace("-*1*-", "<")
                .replace("-*2*-", ">")
                .replace("-*3*-", "\"")
                .replace("-*4*-", "\'")
                .replace("&nbsp;", " ")
                .replace("&amp;nbsp;", "  ");
    }

    /**
     * 脱敏方法
     * @param input 原始字符串
     * @return 脱敏后的字符串
     */
    public static String mark(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        int len = input.length();

        if (len >= 10) {
            // 保留首4位和尾4位，中间6个*
            return input.substring(0, 4) + "******" + input.substring(len - 4);
        } else if (len >= 5) {
            // 保留首2位和尾2位，中间4个*
            return input.substring(0, 2) + "****" + input.substring(len - 2);
        } else {
            // 保留首1位和尾1位，中间3个*
            return input.substring(0, 1) + "***" + input.substring(len - 1);
        }
    }

    public static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
