package org.wx.core.web3unit.tron;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.wx.core.web3unit.HttpRequestUnit;
import org.wx.core.web3unit.Web3HashCheckResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;

public class TronUnit {

    private static final String TRONGRID = "https://api.trongrid.io";

    // ============================
    // secp256k1 初始化
    // ============================
    private static final ECDomainParameters CURVE;
    private static final ECPoint G;
    private static final BigInteger N;

    static {
        X9ECParameters p = SECNamedCurves.getByName("secp256k1");
        CURVE = new ECDomainParameters(p.getCurve(), p.getG(), p.getN(), p.getH());
        G = p.getG();
        N = p.getN();
    }


    // ============================
    // HTTP POST
    // ============================
    private static JSONObject post(String url, String body) throws Exception {

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        if (body != null) {
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.close();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null)
            sb.append(line);

        br.close();
        return JSON.parseObject(sb.toString());
    }


    // ============================
    // Base58 工具
    // ============================
    public static class Base58 {
        private static final char[] ALPHA =
                "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

        private static final int[] INDEX = new int[128];

        static {
            Arrays.fill(INDEX, -1);
            for (int i = 0; i < ALPHA.length; i++)
                INDEX[ALPHA[i]] = i;
        }

        public static byte[] decode(String s) {
            byte[] input = new byte[s.length()];

            for (int i = 0; i < s.length(); i++) {
                int digit = INDEX[s.charAt(i)];
                if (digit < 0) throw new RuntimeException("Invalid Base58 char");
                input[i] = (byte) digit;
            }

            int zeros = 0;
            while (zeros < input.length && input[zeros] == 0) zeros++;

            byte[] decoded = new byte[input.length];
            int j = decoded.length;

            for (int i = zeros; i < input.length; i++) {
                int carry = input[i] & 0xFF;
                int k = decoded.length;

                while (k > j) {
                    carry += (decoded[k - 1] & 0xFF) * 58;
                    decoded[k - 1] = (byte) (carry % 256);
                    carry /= 256;
                    k--;
                }

                while (carry > 0) {
                    decoded[--j] = (byte) (carry % 256);
                    carry /= 256;
                }
            }

            while (j < decoded.length && decoded[j] == 0) j++;

            byte[] out = new byte[decoded.length - j + zeros];
            System.arraycopy(decoded, j, out, zeros, decoded.length - j);
            return out;
        }
    }


    public static String hex(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : arr) sb.append(String.format("%02x", b));
        return sb.toString();
    }



    private static byte[] hexToBytes(String h) {
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    private static byte[] sha256(byte[] data) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }


    // ============================================================
    // 查询主币 TRX
    // ============================================================
    public static BigDecimal getMainBalance(String address) throws Exception {

        byte[] dec = Base58.decode(address);
        String hexAddr = hex(Arrays.copyOfRange(dec, 0, dec.length - 4));

        JSONObject res = post(TRONGRID + "/wallet/getaccount",
                "{\"address\":\"" + hexAddr + "\"}");

        long sun = res.getLongValue("balance");
        return new BigDecimal(sun).divide(BigDecimal.TEN.pow(6));
    }


    // ============================================================
    // 查询 TRC20 Token 余额
    // ============================================================
    public static BigDecimal getTokenBalance(String address, String token, int decimals) throws Exception {

        byte[] dec = Base58.decode(address);
        String raw20 = hex(Arrays.copyOfRange(dec, 0, dec.length - 4)).substring(2);

        String param = "000000000000000000000000" + raw20;

        JSONObject req = new JSONObject();
        req.put("owner_address", address);
        req.put("contract_address", token);
        req.put("function_selector", "balanceOf(address)");
        req.put("parameter", param);
        req.put("visible", true);

        JSONObject result = post(TRONGRID + "/wallet/triggersmartcontract", req.toJSONString());

        String hexVal = result.getJSONArray("constant_result").getString(0);

        return new BigDecimal(new BigInteger(hexVal, 16))
                .divide(BigDecimal.TEN.pow(decimals));
    }


    // ============================================================
    // 构建主币 TRX 交易
    // ============================================================
    public static JSONObject buildMainTx(String from, String to, BigDecimal amount) throws Exception {

        JSONObject req = new JSONObject();
        req.put("owner_address", from);
        req.put("to_address", to);
        req.put("amount", amount.multiply(BigDecimal.TEN.pow(6)).longValue());
        req.put("visible", true);

        return post(TRONGRID + "/wallet/createtransaction", req.toJSONString());
    }


    // ============================================================
    // 构建 TRC20 Token 转账
    // ============================================================
    public static JSONObject buildTokenTx(String from, String to, BigDecimal amount, String token, int decimals) throws Exception {

        String amtHex = amount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger().toString(16);
        amtHex = String.format("%064x", new BigInteger(amtHex, 16));

        String toRaw = hex(Base58.decode(to));
        String to20 = toRaw.substring(2, 42);

        String param = "000000000000000000000000" + to20 + amtHex;

        JSONObject req = new JSONObject();
        req.put("owner_address", from);
        req.put("contract_address", token);
        req.put("function_selector", "transfer(address,uint256)");
        req.put("parameter", param);
        req.put("call_value", 0);
        req.put("fee_limit", 100000000);
        req.put("visible", true);

        JSONObject result = post(TRONGRID + "/wallet/triggersmartcontract", req.toJSONString());
        return result.getJSONObject("transaction");
    }


    // ============================================================
    // Tron 签名（正确）
    // ============================================================
    public static String sign(String rawHex, String prv) throws Exception {

        if (prv.startsWith("0x"))
            prv = prv.substring(2);

        byte[] raw = hexToBytes(rawHex);
        byte[] hash = sha256(raw);

        BigInteger priv = new BigInteger(prv, 16);

        ECPrivateKeyParameters key = new ECPrivateKeyParameters(priv, CURVE);
        ECDSASigner signer = new ECDSASigner();
        signer.init(true, key);

        BigInteger[] sig = signer.generateSignature(hash);
        BigInteger r = sig[0];
        BigInteger s = sig[1];

        if (s.compareTo(N.shiftRight(1)) > 0)
            s = N.subtract(s);

        return to(r, 32) + to(s, 32) + "00"; // v = 0
    }

    private static String to(BigInteger v, int size) {
        String hex = v.toString(16);
        while (hex.length() < size * 2) hex = "0" + hex;
        return hex;
    }


    // ============================================================
    // 签名交易 + 广播
    // ============================================================
    public static JSONObject signTx(JSONObject tx, String privateKey) throws Exception {
        String raw = tx.getString("raw_data_hex");
        tx.put("signature", Arrays.asList(sign(raw, privateKey)));
        return tx;
    }

    public static JSONObject broadcast(JSONObject tx) throws Exception {
        return post(TRONGRID + "/wallet/broadcasttransaction", tx.toJSONString());
    }


    // ============================================================
    // 统一转账入口（主币 / Token）
    // ============================================================
    public static String transferMain(String from, String prv, String to, BigDecimal amount) throws Exception {
        JSONObject unsigned = buildMainTx(from, to, amount);
        JSONObject signed = signTx(unsigned, prv);
        JSONObject r = broadcast(signed);
        return r.getString("txid");
    }

    public static String transferToken(String from, String prv, String token, String to, BigDecimal amount, int decimals) throws Exception {
        JSONObject unsigned = buildTokenTx(from, to, amount, token, decimals);
        JSONObject signed = signTx(unsigned, prv);
        JSONObject r = broadcast(signed);
        return r.getString("txid");
    }

    public static Web3HashCheckResult checkTokenHash(
            String hash,
            String expectFrom,
            String expectTo,
            BigDecimal expectAmount,
            String tokenAddress,
            Integer decimals
    ) {

        Web3HashCheckResult ret = new Web3HashCheckResult();
        ret.setHash(hash);

        // =============== 1. 查询交易 ===============
        HttpRequestUnit http = HttpRequestUnit.init();
        http.setUrl("https://apilist.tronscanapi.com/api/transaction-info?hash=" + hash);
        http.setMethod(HttpRequestUnit.RequestMethod.GET);

        JSONObject json;
        try {
            json = JSONObject.parseObject(http.request());
        } catch (Exception e) {
            ret.fail("交易查询失败");
            return ret;
        }

        if (json == null || !json.containsKey("tokenTransferInfo")) {
            ret.fail("不是有效的 TRC20 转账");
            return ret;
        }

        JSONObject info = json.getJSONObject("tokenTransferInfo");

        // =============== 2. 提取字段 ===============
        String realFrom = info.getString("from_address");
        String realTo = info.getString("to_address");
        String contract = info.getString("contract_address");

        BigInteger amountRaw = info.getBigInteger("amount_str");
        BigDecimal realAmount = new BigDecimal(amountRaw)
                .divide(BigDecimal.TEN.pow(decimals));

        ret.setFrom(realFrom);
        ret.setTo(realTo);
        ret.setAmount(realAmount);

        // =============== 3. 校验地址与金额 ===============

        if (expectFrom != null && !expectFrom.isEmpty()
                && !expectFrom.equalsIgnoreCase(realFrom)) {
            ret.fail("fromAddress 不一致");
            return ret;
        }

        if (expectTo != null && !expectTo.isEmpty()
                && !expectTo.equalsIgnoreCase(realTo)) {
            ret.fail("toAddress 不一致");
            return ret;
        }

        if (expectAmount != null
                && realAmount.compareTo(expectAmount) != 0) {
            ret.fail("转账金额不一致");
            return ret;
        }

        if (!tokenAddress.equalsIgnoreCase(contract)) {
            ret.fail("Token 合约地址不一致");
            return ret;
        }

        // =============== OK ===============
        ret.ok();
        return ret;
    }



}
