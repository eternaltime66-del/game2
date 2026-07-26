package org.wx.core.wxBase.unit;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBusiness.log.entity.WxLogThirdParty;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 无心
 * @date 2022/4/20
 * @msg 发送请求工具
 * @demo HttpRequest
 */
@Data
@Slf4j
public class HttpRequestUnit {

    /**
     * 域名
     */
    public String host;

    /**
     * 二级地址
     */
    public String secondUrl;

    /**
     * 总地址
     * - 发送请求时 实际请求url地址
     * - 可直接setUrl
     * - 或者 ip 复用
     * setHost后 暂存对象 不同的方法 再set不同的 secondUrl
     * - 请求判断
     * url != null 请求url
     * url == null 请求 host + secondUrl
     * url == null && host == null 抛出异常
     */
    public String url;

    /**
     * 请求数据 Object
     */
    public Object sendData;

    /**
     * 请求数据 Json
     */
    public JSONObject sendJson;

    /**
     * 请求返回数据
     */
    public String backData;

    /**
     * 请求方式 POST/GET
     */
    public RequestMethod method;

    /**
     * 请求头
     */
    public Map<String, String> header;

    /**
     * 数据类型 默认 Json
     */
    public ContentType contentType = ContentType.Json;

    /**
     * 拼接参数
     */
    public String joinData;

    public enum RequestMethod {
        /**
         * 请求方式
         */
        GET, POST
    }

    public static HttpRequestUnit init() {
        return new HttpRequestUnit();
    }

    public static Map<String, String> defaultHeader() {
        return new HashMap<>();
    }

    /**
     * 初始化参数
     */
    public void ready() {
        this.url = (this.url == null) ? (this.host + this.secondUrl) : this.url;
        this.header = (this.header == null) ? defaultHeader() : this.header;
        this.method = (this.method == null) ? RequestMethod.POST : this.method;

        if (this.sendJson != null) {
            this.joinData = join(this.sendJson);
        }


        this.header.put("Content-Type", contentType.getVal());
    }

    /**
     * 发起请求
     */
    @SneakyThrows
    public String request() {
        // 1. 初始化日志实体
        WxLogThirdParty logEntity = new WxLogThirdParty();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        logEntity.setTraceId(traceId);
        logEntity.setStartTime(LocalDateTime.now());
        logEntity.setReqState("RUNNING");

        // 记录请求开始时间（计算耗时用）
        long startTimeMs = System.currentTimeMillis();

        try {
            // ========== 初始化请求 ==========
            ready();

            // ========== 填充日志基础信息 ==========
            logEntity.setReqUrl(this.url);
            logEntity.setReqMethod(this.method != null ? this.method.name() : "POST");

            // ========== 处理请求参数 ==========
            if (sendJson != null) {
                joinData = join(sendJson);
                logEntity.setReqData(JSON.toJSONString(sendJson));

            } else if (sendData != null) {
                joinData = JSON.toJSONString(sendData);
                logEntity.setReqData(joinData);

            } else {
                logEntity.setReqData("无请求参数");
            }

            // ========== GET请求参数拼接 ==========
            if (joinData != null && !joinData.isEmpty()) {
                url = url + "?" + joinData;
                logEntity.setReqUrl(url); // 更新拼接后的URL
            }

            // ========== 发起HTTP请求 ==========

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(600000);
            conn.setReadTimeout(600000);
            // 设置请求头并记录
            if (header != null && !header.isEmpty()) {
                header.forEach(conn::setRequestProperty);
            }
            conn.setRequestMethod(method.name());
            conn.setDoInput(true);
            conn.setDoOutput(method == RequestMethod.POST);

            // ========== POST请求写入参数 ==========
            if (method == RequestMethod.POST && sendJson != null) {
//                log.debug("【三方请求体】traceId: {} | POST请求体: {}", traceId, joinData);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(sendJson.toJSONString().getBytes());
                }
            }

            // ========== 处理响应 ==========
            int responseCode = conn.getResponseCode();
            String back;
            if (responseCode == 200) {
                back = readResponseContent(conn.getInputStream());
                logEntity.setReqState("SUCCESS");
            } else {
                back = readResponseContent(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
                logEntity.setReqState("FAIL");
                logEntity.setErrorMsg("响应码异常: " + responseCode);
                logBody(); // 打印详细请求体日志
            }

            // 记录响应数据
            logEntity.setResData(back);
            // ========== 保存响应结果 ==========
            backData = back;
            return back;

        } catch (Exception e) {
            // 捕获所有异常，记录到日志实体
            logEntity.setReqState("FAIL");
            logEntity.setErrorMsg("请求异常: " + e.getMessage());
            throw e; // 抛出异常，不影响上层业务处理
        } finally {
            // ========== 补全日志并保存到数据库 ==========
            logEntity.setFinishTime(LocalDateTime.now());
            logEntity.setUseTimeMs((int) (System.currentTimeMillis() - startTimeMs));
            try {
                if (Wx.LogAsyncService != null) {
                    Wx.LogAsyncService.saveThirdParty(logEntity);
                }
            } catch (Exception e) {
                log.error("【三方日志保存】traceId: {} | 提交异步日志失败: {}", traceId, e.getMessage(), e);
            }
        }
    }

    /**
     * Json 参数转 URL 参数
     */
    public static String join(JSONObject map) {
        String join = "&";
        String and = "=";
        AtomicReference<String> str = new AtomicReference<>("");
        map.forEach((key, val) -> {
            String baseStr = str.get();
            String thisStr = key + and + val;
            if (!baseStr.isEmpty()) {
                thisStr = join + thisStr;
            }
            baseStr += thisStr;
            str.set(baseStr);
        });
        return str.get();
    }

    /**
     * 读取响应字节流并将之转为字符串
     */
    public static String readResponseContent(InputStream in) throws IOException {
        Reader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            char[] buffer = new char[1024];
            int head;
            while ((head = reader.read(buffer)) > 0) {
                content.append(new String(buffer, 0, head));
            }
            return content.toString();
        } finally {
            if (null != in) in.close();
            if (null != reader) reader.close();
        }
    }

    public enum ContentType {
        Json("application/json"),
        FormData("multipart/form-data"),
        PostForm("application/x-www-form-urlencoded"),
        OctetStream("application/octet-stream");

        public String val;

        ContentType(String val) {
            this.val = val;
        }

        public String getVal() {
            return this.val;
        }
    }

    /**
     * 打印请求与响应日志（用于接口异常排查）
     */
    public void logBody() {
        try {
            JSONObject json = new JSONObject();
            json.put("url", this.url);
            json.put("method", this.method);
            json.put("joinData", this.joinData);
            json.put("header", this.header);
            json.put("contentType", this.contentType);

            if (this.sendJson != null) {
                json.put("sendJson", this.sendJson);
            }

            if (this.backData != null) {
                json.put("backData", this.backData.length() > 2000
                        ? this.backData.substring(0, 2000) + "..."
                        : this.backData);
            }
            System.out.println(json.toJSONString());
        } catch (Exception e) {
            log.error("HttpRequestUnit logBody error", e);
        }
    }

}
