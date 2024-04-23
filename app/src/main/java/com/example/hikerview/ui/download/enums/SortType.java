package com.example.hikerview.ui.download.enums;

/**
 * 作者：By 15968
 * 日期：On 2021/1/30
 * 时间：At 10:59
 */

public enum SortType {
    NAME(0, "按名称排序"),
    DEFAULT(1, "按时间排序"),
    TIME_REVERSE(2, "按时间倒序");


    private int code;
    private String name;

    SortType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static SortType getByName(String name) {
        for (SortType value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return DEFAULT;
    }

    public static SortType getByCode(int code) {
        for (SortType value : values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return DEFAULT;
    }

    public static String[] getNames() {
        SortType[] types = values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getName();
        }
        return names;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
