package org.wx.core.web3unit;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.SneakyThrows;
import org.wx.core.web3unit.tron.TronAddressUtil;
import org.wx.core.wxBusiness.account.entity.Web3Coin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Web3 转账统一扫描服务
 * 支持：ETH / BSC / TRON
 */
public class Web3TransferScanService {

    // ================== API KEY ==================
    private static final String ETHERSCAN_KEY = "V2DGC79YQ852VWZM1UECA7JPJ5QN67HEDD";
    private static final String TRONGRID_KEY = "83b9e3b5-73de-4392-9cf2-c04f80c46e14";

    private static final String ETHERSCAN_API = "https://api.etherscan.io/v2/api";
    private static final String TRONGRID_API = "https://api.trongrid.io/v1/accounts/";
    // ============================================

    /**
     * ✅ 统一入口
     *
     * @param coin     币种（Web3Coin）
     * @param address  地址
     * @param pageNo   页码（从 1 开始）
     * @param pageSize 每页条数
     */
    @SneakyThrows
    public static List<Web3TransferRecord> scan(
            Web3Coin coin,
            String address,
            int pageNo,
            int pageSize
    ) {

        if (coin.getChain() == Link.TRON) {
            return scanTron(coin, address, pageNo, pageSize);
        } else {
            return scanEvm(coin, address, pageNo, pageSize);
        }
    }

    // =====================================================
    // TRON 扫描（TRX / TRC20）
    // =====================================================
    private static List<Web3TransferRecord> scanTron(
            Web3Coin coin,
            String address,
            int pageNo,
            int pageSize
    ) throws Exception {

        String fingerprint = null;

        for (int i = 1; i < pageNo; i++) {
            fingerprint = fetchNextFingerprint(coin, address, pageSize, fingerprint);
            if (fingerprint == null) {
                return new ArrayList<>();
            }
        }

        String url = TRONGRID_API
                + address
                + (coin.isMainCoin() ? "/transactions" : "/transactions/trc20")
                + "?limit=" + pageSize;

        if (fingerprint != null) {
            url += "&fingerprint=" + fingerprint;
        }

        JSONObject root = JSONObject.parseObject(httpGet(url, TRONGRID_KEY));
        JSONArray data = root.getJSONArray("data");

        List<Web3TransferRecord> list = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            JSONObject tx = data.getJSONObject(i);

            Web3TransferRecord r = new Web3TransferRecord();
            r.setHash(tx.getString("txID"));
            r.setCoin(coin.getCoinName());

            long txTime = tx.getLongValue("block_timestamp");
            r.setTxTime(txTime);

            // ===============================
            // TRC20
            // ===============================
            if (!coin.isMainCoin() && tx.containsKey("token_info")) {

                String contract = tx.getJSONObject("token_info").getString("address");
                if (!coin.getTokenAddress().equalsIgnoreCase(contract)) {
                    continue;
                }

                String from = tx.getString("from");
                String to = tx.getString("to");

                String valueStr = tx.getString("value");
                if (valueStr == null) continue;

                BigDecimal amount = new BigDecimal(valueStr)
                        .divide(
                                BigDecimal.TEN.pow(coin.getDecimals()),
                                coin.getDecimals(),
                                RoundingMode.DOWN
                        );

                r.setFrom(from);
                r.setTo(to);
                r.setAmount(amount);
                r.setDirection(address.equalsIgnoreCase(to) ? "IN" : "OUT");

                list.add(r);
                continue;
            }

            // ===============================
            // TRX 主币
            // ===============================
            if (coin.isMainCoin()) {

                JSONArray contracts = tx
                        .getJSONObject("raw_data")
                        .getJSONArray("contract");

                if (contracts == null || contracts.isEmpty()) continue;

                JSONObject contract = contracts.getJSONObject(0);
                if (!"TransferContract".equals(contract.getString("type"))) {
                    continue;
                }

                JSONObject val = contract
                        .getJSONObject("parameter")
                        .getJSONObject("value");

                if (val == null) continue;

                String from = TronAddressUtil.tronHexToBase58(val.getString("owner_address"));
                String to =  TronAddressUtil.tronHexToBase58(val.getString("to_address"));

                long amountSun = val.getLongValue("amount");

                BigDecimal amount = new BigDecimal(amountSun)
                        .divide(BigDecimal.TEN.pow(coin.getDecimals()),
                                coin.getDecimals(),
                                RoundingMode.DOWN);

                r.setFrom(from);
                r.setTo(to);
                r.setAmount(amount);
                r.setDirection(address.equalsIgnoreCase(to) ? "IN" : "OUT");

                list.add(r);
            }
        }

        return list;
    }

    /**
     * 获取下一页 fingerprint（内部用）
     */
    private static String fetchNextFingerprint(
            Web3Coin coin,
            String address,
            int pageSize,
            String fingerprint
    ) throws Exception {

        String url = TRONGRID_API
                + address
                + (coin.isMainCoin() ? "/transactions" : "/transactions/trc20")
                + "?limit=" + pageSize;

        if (fingerprint != null) {
            url += "&fingerprint=" + fingerprint;
        }

        JSONObject root = JSONObject.parseObject(httpGet(url, TRONGRID_KEY));

        if (root.containsKey("meta")) {
            return root.getJSONObject("meta").getString("fingerprint");
        }
        return null;
    }

    // =====================================================
    // EVM 扫描（ETH / BSC）
    // =====================================================
    private static List<Web3TransferRecord> scanEvm(
            Web3Coin coin,
            String address,
            int pageNo,
            int pageSize
    ) throws Exception {
        String action = coin.isMainCoin() ? "txlist" : "tokentx";
        String url = ETHERSCAN_API
                + "?chainid=" + coin.getChain().getChainId()
                + "&module=account"
                + "&action=" + action
                + "&address=" + address
                + "&page=" + pageNo
                + "&offset=" + pageSize
                + "&sort=desc"
                + "&apikey=" + ETHERSCAN_KEY;

        if (!coin.isMainCoin()) {
            url += "&contractaddress=" + coin.getTokenAddress();
        }

        JSONObject root = JSONObject.parseObject(httpGet(url, null));
        JSONArray arr = root.getJSONArray("result");

        List<Web3TransferRecord> list = new ArrayList<>();

        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Web3TransferRecord r = new Web3TransferRecord();
            r.setHash(o.getString("hash"));
            r.setFrom(o.getString("from"));
            r.setTo(o.getString("to"));
            r.setCoin(coin.getCoinName());
            BigDecimal amount = new BigDecimal(o.getString("value"))
                    .divide(
                            BigDecimal.TEN.pow(coin.getDecimals()),
                            coin.getDecimals(),
                            RoundingMode.DOWN
                    );
            r.setAmount(amount);

            r.setDirection(
                    address.equalsIgnoreCase(r.getTo()) ? "IN" : "OUT"
            );

            long timeSeconds = o.getLongValue("timeStamp");
            r.setTxTime(timeSeconds * 1000);
            list.add(r);
        }

        return list;
    }

    // =====================================================
    // HTTP GET
    // =====================================================
    private static String httpGet(String url, String tronKey) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        if (tronKey != null) {
            conn.setRequestProperty("TRON-PRO-API-KEY", tronKey);
        }

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


    /**
     * EVM：按 hash 查 ERC20 / BEP20 Transfer
     * ⚠️ tokentx 对 txhash 并非强约束，必须二次过滤
     */
    @SneakyThrows
    private static List<Web3TransferRecord> scanEvmTokenByHash(
            Web3Coin coin,
            String address,
            String txHash
    ) {

        // ====== 1️⃣ txHash 强校验 ======
        if (txHash == null || !txHash.startsWith("0x") || txHash.length() != 66) {
            throw new IllegalArgumentException("非法 txHash: " + txHash);
        }

        String url = ETHERSCAN_API
                + "?chainid=" + coin.getChain().getChainId()
                + "&module=account"
                + "&action=tokentx"
                + "&txhash=" + txHash
                + "&contractaddress=" + coin.getTokenAddress()
                + "&sort=desc"
                + "&apikey=" + ETHERSCAN_KEY;

        JSONObject root = JSONObject.parseObject(httpGet(url, null));

        // ====== 2️⃣ result 类型校验（防止被降级成历史列表） ======
        Object resultObj = root.get("result");
        if (!(resultObj instanceof JSONArray)) {
            return new ArrayList<>();
        }

        JSONArray arr = (JSONArray) resultObj;
        List<Web3TransferRecord> list = new ArrayList<>();

        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);

            // ====== 3️⃣ hash 二次过滤（关键兜底） ======
            if (!txHash.equalsIgnoreCase(o.getString("hash"))) {
                continue;
            }

            String from = o.getString("from");
            String to = o.getString("to");

            // 可选：只保留与该 address 相关的
            if (!address.equalsIgnoreCase(from) && !address.equalsIgnoreCase(to)) {
                continue;
            }

            BigDecimal amount = new BigDecimal(o.getString("value"))
                    .divide(
                            BigDecimal.TEN.pow(coin.getDecimals()),
                            coin.getDecimals(),
                            RoundingMode.DOWN
                    );

            Web3TransferRecord r = new Web3TransferRecord();
            r.setHash(txHash);
            r.setFrom(from);
            r.setTo(to);
            r.setCoin(coin.getCoinName());
            r.setAmount(amount);
            r.setDirection(address.equalsIgnoreCase(to) ? "IN" : "OUT");
            r.setTxTime(o.getLongValue("timeStamp") * 1000);

            list.add(r);
        }

        return list;
    }


    /**
     * EVM：按 hash 查 Internal Transactions（txlistinternal）
     * 只返回和 address 相关的 in/out（更符合“查询某地址这笔 hash 的资金流”）
     */
    /**
     * EVM：按 txHash 查询 Internal Transactions
     * 使用官方 v2 接口：
     * module=account&action=txlistinternal&txhash=xxx
     */
    @SneakyThrows
    private static List<Web3TransferRecord> scanEvmInternalByHash(
            Web3Coin coin,
            String txHash
    ) {

        // 1️⃣ txHash 基本校验
        if (txHash == null || !txHash.startsWith("0x") || txHash.length() != 66) {
            throw new IllegalArgumentException("非法 txHash: " + txHash);
        }

        String url = ETHERSCAN_API
                + "?apikey=" + ETHERSCAN_KEY
                + "&chainid=" + coin.getChain().getChainId()
                + "&module=account"
                + "&action=txlistinternal"
                + "&txhash=" + txHash;

        JSONObject root = JSONObject.parseObject(httpGet(url, null));

        Object resultObj = root.get("result");
        if (!(resultObj instanceof JSONArray)) {
            // 接口异常或无 internal tx
            return new ArrayList<>();
        }

        JSONArray arr = (JSONArray) resultObj;

        List<Web3TransferRecord> list = new ArrayList<>();

        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);

            String from = o.getString("from");
            String to = o.getString("to");

            BigDecimal amount = new BigDecimal(o.getString("value"))
                    .divide(
                            BigDecimal.TEN.pow(coin.getDecimals()),
                            coin.getDecimals(),
                            RoundingMode.DOWN
                    );

            // 官方说明：只返回 non-zero，但这里再兜一层
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Web3TransferRecord r = new Web3TransferRecord();
            r.setHash(txHash);
            r.setFrom(from);
            r.setTo(to);
            r.setCoin(coin.getCoinName());
            r.setAmount(amount);

            // 这里不再和某地址绑定，只表示资金流向
            r.setDirection("OUT"); // from -> to，本身就是一次支出行为

            r.setTxTime(o.getLongValue("timeStamp") * 1000);

            list.add(r);
        }

        return list;
    }


    public static void main(String[] args) throws Exception {

    }

}
