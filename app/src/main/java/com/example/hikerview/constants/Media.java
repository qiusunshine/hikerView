package com.example.hikerview.constants;

/**
 * 作者：By hdy
 * 日期：On 2018/12/1
 * 时间：At 12:03
 */
public class Media {
    public static String VIDEO = MediaType.VIDEO.getName();
    public static String VIDEO_MUSIC = MediaType.VIDEO_MUSIC.getName();
    public static String IMAGE = MediaType.IMAGE.getName();
    public static String MUSIC = MediaType.MUSIC.getName();
    public static String HTML = MediaType.HTML.getName();
    public static String OTHER = MediaType.OTHER.getName();
    public static String BLOCK = MediaType.BLOCK.getName();

    public Media(MediaType mediaType) {
        this.name = mediaType.getName();
    }

    public Media(String name) {
        this.name = name;
    }

    public Media(String name, String type) {
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
    }
}
