package chuangyuan.ycj.videolibrary.upstream;

/**
 * 作者：By 15968
 * 日期：On 2021/8/13
 * 时间：At 21:08
 */

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

public final class ContentDataSource implements DataSource {
    private final ContentResolver resolver;
    private final TransferListener<? super ContentDataSource> listener;
    private Uri uri;
    private AssetFileDescriptor assetFileDescriptor;
    private FileInputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    public ContentDataSource(Context context) {
        this(context, (TransferListener) null);
    }

    public ContentDataSource(Context context, TransferListener<? super ContentDataSource> listener) {
        this.resolver = context.getContentResolver();
        this.listener = listener;
    }

    public long open(DataSpec dataSpec) throws ContentDataSource.ContentDataSourceException {
        try {
            this.uri = dataSpec.uri;
            this.assetFileDescriptor = this.resolver.openAssetFileDescriptor(this.uri, "r");
            if (this.assetFileDescriptor == null) {
                throw new FileNotFoundException("Could not open file descriptor for: " + this.uri);
            }

            this.inputStream = new FileInputStream(this.assetFileDescriptor.getFileDescriptor());
            long assetStartOffset = this.assetFileDescriptor.getStartOffset();
            long skipped = this.inputStream.skip(assetStartOffset + dataSpec.position) - assetStartOffset;
            if (skipped != dataSpec.position) {
                throw new EOFException();
            }

            if (dataSpec.length != -1L) {
                this.bytesRemaining = dataSpec.length;
            } else {
                long assetFileDescriptorLength = this.assetFileDescriptor.getLength();
                if (assetFileDescriptorLength == -1L) {
                    FileChannel channel = this.inputStream.getChannel();
                    long channelSize = channel.size();
                    this.bytesRemaining = channelSize == 0L ? -1L : channelSize - channel.position();
                } else {
                    this.bytesRemaining = assetFileDescriptorLength - skipped;
                }
            }
        } catch (IOException var11) {
            throw new ContentDataSource.ContentDataSourceException(var11);
        }

        this.opened = true;
        if (this.listener != null) {
            this.listener.onTransferStart(this, dataSpec);
        }

        return this.bytesRemaining;
    }

    public int read(byte[] buffer, int offset, int readLength) throws ContentDataSource.ContentDataSourceException {
        if (readLength == 0) {
            return 0;
        } else if (this.bytesRemaining == 0L) {
            return -1;
        } else {
            int bytesRead;
            try {
                int bytesToRead = this.bytesRemaining == -1L ? readLength : (int) Math.min(this.bytesRemaining, (long) readLength);
                bytesRead = this.inputStream.read(buffer, offset, bytesToRead);
            } catch (IOException var6) {
                throw new ContentDataSource.ContentDataSourceException(var6);
            }

            if (bytesRead == -1) {
                if (this.bytesRemaining != -1L) {
                    throw new ContentDataSource.ContentDataSourceException(new EOFException());
                } else {
                    return -1;
                }
            } else {
                if (this.bytesRemaining != -1L) {
                    this.bytesRemaining -= (long) bytesRead;
                }

                if (this.listener != null) {
                    this.listener.onBytesTransferred(this, bytesRead);
                }

                return bytesRead;
            }
        }
    }

    public Uri getUri() {
        return this.uri;
    }

    public void close() throws ContentDataSource.ContentDataSourceException {
        this.uri = null;

        try {
            if (this.inputStream != null) {
                this.inputStream.close();
            }
        } catch (IOException var26) {
            throw new ContentDataSource.ContentDataSourceException(var26);
        } finally {
            this.inputStream = null;

            try {
                if (this.assetFileDescriptor != null) {
                    this.assetFileDescriptor.close();
                }
            } catch (IOException var24) {
                throw new ContentDataSource.ContentDataSourceException(var24);
            } finally {
                this.assetFileDescriptor = null;
                if (this.opened) {
                    this.opened = false;
                    if (this.listener != null) {
                        this.listener.onTransferEnd(this);
                    }
                }

            }

        }

    }

    public static class ContentDataSourceException extends IOException {
        public ContentDataSourceException(IOException cause) {
            super(cause);
        }
    }
}
