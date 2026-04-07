package com.sky.utils;

import java.util.regex.Pattern;

/**
 * XSS防护工具类
 * 提供HTML转义和危险模式过滤功能
 */
public class XssUtils {

    /**
     * 危险的XSS攻击模式
     */
    public static final Pattern[] DANGEROUS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data\\s*:", Pattern.CASE_INSENSITIVE)
    };

    /**
     * HTML实体转义
     * 将危险字符转为安全的HTML实体
     */
    public static String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    /**
     * 完整的XSS过滤
     * 1. 进行HTML转义
     * 2. 移除危险模式
     * 3. 去除首尾空格
     */
    public static String filterXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        value = escapeHtml(value);
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            value = pattern.matcher(value).replaceAll("");
        }
        return value.trim();
    }
}
