package org.wx.core.web3unit.evm;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * EVM 多链 Scan Demo（ETH / BSC / Polygon / Arb / Op）
 * 基于 Etherscan API V2
 */
public class EvmTrsRecord {

    // ================== 必填 ==================

    /** 你的 Etherscan API KEY */
    private static final String API_KEY = "V2DGC79YQ852VWZM1UECA7JPJ5QN67HEDD";

    /** Etherscan V2 统一入口 */
    private static final String API_BASE = "https://api.etherscan.io/v2/api";

    /** 示例地址 */
    private static final String ADDRESS = "0x3FCdba57215C4707B7B261e6fAF36fa596e5f7FE";

    /** 示例 Token（ETH-USDT） */
    private static final String USDT_ETH =
            "0x55d398326f99059fF775485246999027B3197955";

    // ========================================

    public static void main(String[] args) throws Exception {



        System.out.println("\n===== BSC ERC20 转账 =====");
        queryErc20Tx(56, ADDRESS, USDT_ETH); // BSC 上请换成 BSC USDT

        System.out.println("\n===== ETH 主币转账 =====");
        queryNativeTx(56, ADDRESS);
    }

    /**
     * 查询 ERC20 转账记录
     */
    public static void queryErc20Tx(
            int chainId,
            String address,
            String tokenAddress
    ) throws Exception {

        String url = API_BASE
                + "?chainid=" + chainId
                + "&module=account"
                + "&action=tokentx"
                + "&address=" + address
                + "&contractaddress=" + tokenAddress
                + "&startblock=0"
                + "&endblock=99999999"
                + "&page=1"
                + "&offset=10"
                + "&sort=desc"
                + "&apikey=" + API_KEY;

        JSONObject root = JSONObject.parseObject(httpGet(url));

        if (!"1".equals(root.getString("status"))) {
            System.out.println(root);
            System.out.println("no result: " + root.getString("message"));
            return;
        }

        JSONArray arr = root.getJSONArray("result");

        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);

            String hash = o.getString("hash");
            String from = o.getString("from");
            String to = o.getString("to");
            String value = o.getString("value");
            int decimals = o.getIntValue("tokenDecimal");
            String symbol = o.getString("tokenSymbol");

            BigDecimal amount = new BigDecimal(value)
                    .divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.DOWN);

            System.out.println(
                    "hash=" + hash
                            + ", from=" + from
                            + ", to=" + to
                            + ", amount=" + amount + " " + symbol
            );
        }
    }

    /**
     * 查询主币转账（ETH / BNB / MATIC）
     */
    public static void queryNativeTx(
            int chainId,
            String address
    ) throws Exception {

        String url = API_BASE
                + "?chainid=" + chainId
                + "&module=account"
                + "&action=txlist"
                + "&address=" + address
                + "&startblock=0"
                + "&endblock=99999999"
                + "&page=1"
                + "&offset=10"
                + "&sort=desc"
                + "&apikey=" + API_KEY;

        JSONObject root = JSONObject.parseObject(httpGet(url));

        if (!"1".equals(root.getString("status"))) {
            System.out.println("no result: " + root.getString("message"));
            return;
        }

        JSONArray arr = root.getJSONArray("result");

        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);

            String hash = o.getString("hash");
            String from = o.getString("from");
            String to = o.getString("to");
            String value = o.getString("value");

            BigDecimal amount = new BigDecimal(value)
                    .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.DOWN);

            System.out.println(
                    "hash=" + hash
                            + ", from=" + from
                            + ", to=" + to
                            + ", amount=" + amount
            );
        }
    }

    /**
     * HTTP GET
     */
    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
