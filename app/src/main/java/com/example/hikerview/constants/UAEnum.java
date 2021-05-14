package com.example.hikerview.constants;

/**
 * 作者：By 15968
 * 日期：On 2020/7/21
 * 时间：At 21:04
 */

public enum UAEnum {
    AUTO("auto", "自动提取（不建议使用）", ""),
    MOBILE("mobile", "移动端（mobile、Android）", "Mozilla/5.0 (Linux; Android 11; Mi 10 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.152 Mobile Safari/537.36"),
    PC("pc", "电脑端（pc、Windows）", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");

    UAEnum(String code, String name, String content) {
        this.code = code;
        this.name = name;
        this.content = content;
    }

    private final String code;
    private final String name;
    private final String content;

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static UAEnum getByName(String name) {
        if (name == null) {
            return AUTO;
        }
        for (UAEnum value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return AUTO;
    }

    public static UAEnum getByCode(String name) {
        if (name == null) {
            return AUTO;
        }
        for (UAEnum value : values()) {
            if (value.getCode().equals(name)) {
                return value;
            }
        }
        return AUTO;
    }

    public String getContent() {
        return content;
    }
}
