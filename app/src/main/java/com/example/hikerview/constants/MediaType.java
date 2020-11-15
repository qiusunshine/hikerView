package com.example.hikerview.constants;

/**
 * 作者：By hdy
 * 日期：On 2018/12/1
 * 时间：At 12:03
 */
public enum MediaType {

    VIDEO("VIDEO", ""), VIDEO_MUSIC("VIDEO_MUSIC", ""), IMAGE("IMAGE", ""), MUSIC("MUSIC", ""), HTML("HTML",""), OTHER("OTHER",""), BLOCK("BLOCK","");

    MediaType(String name, String type) {
        this.name = name;
        this.type = type;
    }

    private String name;
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }}
