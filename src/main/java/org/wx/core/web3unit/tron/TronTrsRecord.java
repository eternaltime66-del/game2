package org.wx.core.web3unit.tron;



import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class TronTrsRecord {

    // ======== 必改参数 ========
    private static final String API_KEY = "83b9e3b5-73de-4392-9cf2-c04f80c46e14"; // 你的 TronGrid Key
    private static final String ADDRESS =
            "TMWHMZJR2kuZsoZAVP3qRBAAVdXDdE7e1B"; // 你的 TRON 地址（T 开头）
    // USDT(TRC20) 官方合约地址
    private static final String USDT_CONTRACT =
            "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
    // =========================

    public static void main(String[] args) throws Exception {

        // 第一次不传 fingerprint，后续分页再用
        String fingerprint = null;

        String url = "https://api.trongrid.io/v1/accounts/"
                + ADDRESS
                + "/transactions/trc20"
                + "?only_to=true"
                + "&limit=20";

        if (fingerprint != null) {
            url += "&fingerprint=" + fingerprint;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        // ===== 固定请求头（别改）=====
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("TRON-PRO-API-KEY", API_KEY);

        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);

        int code = conn.getResponseCode();
        System.out.println("HTTP Status: " + code);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        code >= 200 && code < 300
                                ? conn.getInputStream()
                                : conn.getErrorStream(),
                        StandardCharsets.UTF_8
                )
        )) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject root = JSONObject.parseObject(sb.toString());
            JSONArray data = root.getJSONArray("data");

            System.out.println("=== TRC20 Deposits ===");

            for (int i = 0; i < data.size(); i++) {
                JSONObject tx = data.getJSONObject(i);

                // 过滤 USDT
                String contract = tx.getJSONObject("token_info").getString("address");
                if (!USDT_CONTRACT.equals(contract)) {
                    continue;
                }

                String txId = tx.getString("transaction_id");
                String from = tx.getString("from");
                String to = tx.getString("to");

                String value = tx.getString("value"); // 最小单位
                int decimals = tx.getJSONObject("token_info").getIntValue("decimals");

                BigDecimal amount = new BigDecimal(value)
                        .divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.DOWN);

                long timestamp = tx.getLongValue("block_timestamp");

                System.out.println(
                        "txId=" + txId
                                + ", from=" + from
                                + ", to=" + to
                                + ", amount=" + amount + " USDT"
                                + ", time=" + timestamp
                );
            }

            // 分页指纹（下次用）
            if (root.containsKey("meta")) {
                String next = root.getJSONObject("meta").getString("fingerprint");
                System.out.println("next fingerprint = " + next);
            }
        }
    }
}
