package org.wx.core.wxBase.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * web 配置类
 * @author Administrator
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 生产环境建议替换为具体域名，如 "https://your-frontend.com"
                .allowedOriginPatterns("*")
                .allowedHeaders("*")
                // 允许携带Cookie/认证信息
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "OPTIONS", "DELETE", "PUT", "PATCH")
                .maxAge(3600);
    }

    public final String filepathWin = "D:\\File\\Work\\Java\\002_DP\\";
    public final String filepathLinux = "/www/wx/file/";


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        boolean isWin = isWinOs();
        String filepath = isWin ? filepathWin.replace("\\", "/") : filepathLinux;
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + filepath);
    }

    public static Boolean isWinOs() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("win");
    }
}