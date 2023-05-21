package com.jeffmony.videocache.socket.request;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author jeffmony
 */

public class ChunkedOutputStream extends FilterOutputStream {
    public ChunkedOutputStream(OutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public void write(int b) throws IOException {
        byte[] data = {(byte) b};
        write(data, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0)
            return;
        out.write(String.format("%x\r\n", len).getBytes());
        out.write(b, off, len);
        out.write("\r\n".getBytes());
    }

    public void finish() throws IOException {
        out.write("0\r\n\r\n".getBytes());
    }
}
