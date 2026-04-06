package org.wx.core.web3unit;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class HttpRequestUnit {

    public String host;
    public String secondUrl;
    public String url;
    public Object sendData;
    public JSONObject sendJson;
    public String backData;
    public RequestMethod method;
    public Map<String, String> header;
    public ContentType contentType = ContentType.Json;
    public String joinData;

    public enum RequestMethod { GET, POST }

    @Getter
    public enum ContentType {
        Json("application/json"),
        FormData("multipart/form-data"),
        PostForm("application/x-www-form-urlencoded"),
        OctetStream("application/octet-stream");

        public final String val;
        ContentType(String val) { this.val = val; }
    }

    public static HttpRequestUnit init() { return new HttpRequestUnit(); }

    public static Map<String, String> defaultHeader() { return new HashMap<>(); }

    public void ready() {
        this.url = (this.url == null) ? (this.host + this.secondUrl) : this.url;
        this.header = (this.header == null) ? defaultHeader() : this.header;
        this.method = (this.method == null) ? RequestMethod.POST : this.method;
        this.header.put("Content-Type", contentType.getVal());
    }

    @SneakyThrows
    public String request() {
        ready();

        if (sendJson != null) {
            joinData = join(sendJson);
        } else if (sendData != null) {
            joinData = JSON.toJSONString(sendData);
        }

        if (method == RequestMethod.GET && joinData != null && !joinData.isEmpty()) {
            url = url + "?" + joinData;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(600000);
        conn.setReadTimeout(600000);
        header.forEach(conn::setRequestProperty);
        conn.setRequestMethod(method.name());
        conn.setDoInput(true);
        conn.setDoOutput(method == RequestMethod.POST);

        if (method == RequestMethod.POST && joinData != null) {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(joinData.getBytes());
            }
        }

        String back;
        if (conn.getResponseCode() == 200) {
            back = readResponseContent(conn.getInputStream());
        } else {
            back = readResponseContent(conn.getErrorStream());
            logBody();
        }
        backData = back;
        return back;
    }

    public static String join(JSONObject data) {
        StringBuilder sb = new StringBuilder();
        for (String key : data.keySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(key).append("=").append(data.get(key));
        }
        return sb.toString();
    }

    public static String readResponseContent(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    public void logBody() {
        log.error(JSON.toJSONString(this));
    }

    public HttpRequestUnit copy() {
        return JSON.parseObject(JSON.toJSONString(this), HttpRequestUnit.class);
    }
}
