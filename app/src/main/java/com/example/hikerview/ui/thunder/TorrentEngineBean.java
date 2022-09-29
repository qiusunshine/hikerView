package com.example.hikerview.ui.thunder;

/**
 * 作者：By 15968
 * 日期：On 2022/9/20
 * 时间：At 20:32
 */

public class TorrentEngineBean {
    public TorrentEngineBean() {
    }

    public TorrentEngineBean(String name, String className, String checkClass, String soFile, String soUrl64, String soUrl32, String soMd5) {
        this.name = name;
        this.className = className;
        this.checkClass = checkClass;
        this.soFile = soFile;
        this.soUrl64 = soUrl64;
        this.soUrl32 = soUrl32;
        this.soMd5 = soMd5;
    }

    private String name;
    private String className;
    private String checkClass;
    private String soFile;
    private String soUrl64;
    private String soUrl32;
    private String soMd5;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSoMd5() {
        return soMd5;
    }

    public void setSoMd5(String soMd5) {
        this.soMd5 = soMd5;
    }

    public String getSoFile() {
        return soFile;
    }

    public void setSoFile(String soFile) {
        this.soFile = soFile;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCheckClass() {
        return checkClass;
    }

    public void setCheckClass(String checkClass) {
        this.checkClass = checkClass;
    }

    public String getSoUrl64() {
        return soUrl64;
    }

    public void setSoUrl64(String soUrl64) {
        this.soUrl64 = soUrl64;
    }

    public String getSoUrl32() {
        return soUrl32;
    }

    public void setSoUrl32(String soUrl32) {
        this.soUrl32 = soUrl32;
    }
}