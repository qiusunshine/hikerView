package com.jeffmony.videocache.socket.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jeffmony
 */

public class ContentType {

    private static final String ASCII_ENCODING = "US-ASCII";
    private static final String MULTIPART_FORM_DATA_HEADER = "multipart/form-data";

    private static final String CONTENT_REGEX = "[ |\t]*([^/^ ^;^,]+/[^ ^;^,]+)";
    private static final Pattern MIME_PATTERN = Pattern.compile(CONTENT_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String CHARSET_REGEX = "[ |\t]*(charset)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";
    private static final Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String BOUNDARY_REGEX = "[ |\t]*(boundary)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_REGEX, Pattern.CASE_INSENSITIVE);

    private final String mHeader;
    private final String mContentType;
    private final String mEncoding;
    private final String mBoundary;

    public ContentType(String header) {
        this.mHeader = header;
        if (header != null) {
            mContentType = getDetailFromContentHeader(header, MIME_PATTERN, "", 1);
            mEncoding = getDetailFromContentHeader(header, CHARSET_PATTERN, null, 2);
        } else {
            mContentType = "";
            mEncoding = "UTF-8";
        }
        if (MULTIPART_FORM_DATA_HEADER.equalsIgnoreCase(mContentType)) {
            mBoundary = getDetailFromContentHeader(header, BOUNDARY_PATTERN, null, 2);
        } else {
            mBoundary = null;
        }
    }

    private String getDetailFromContentHeader(String contentTypeHeader,
                                              Pattern pattern,
                                              String defaultValue, int group) {
        Matcher matcher = pattern.matcher(contentTypeHeader);
        return matcher.find() ? matcher.group(group) : defaultValue;
    }

    public String getEncoding() {
        return mEncoding == null ? ASCII_ENCODING : mEncoding;
    }
}
