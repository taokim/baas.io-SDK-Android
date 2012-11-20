
package com.kth.baasio.callback;

import com.kth.baasio.Baasio;

import org.apache.http.entity.AbstractHttpEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileEntityWidthProgress extends AbstractHttpEntity implements Cloneable {

    protected final File file;

    private final ProgressListener listener;

    private long transferredBytes;

    private boolean isCancelled = false;

    private int mUploadBuffSize = Baasio.getUploadBuffSize();

    public FileEntityWidthProgress(final File file, final String contentType,
            ProgressListener listener) {
        super();
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        this.file = file;
        this.listener = listener;
        this.transferredBytes = 0;
        setContentType(contentType);
    }

    public void cancel() {
        isCancelled = true;
    }

    public boolean isRepeatable() {
        return true;
    }

    public long getContentLength() {
        return this.file.length();
    }

    public InputStream getContent() throws IOException {
        return new FileInputStream(this.file);
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        InputStream instream = new FileInputStream(this.file);
        try {
            byte[] tmp = new byte[mUploadBuffSize];
            int l;
            while ((l = instream.read(tmp)) != -1 && !isCancelled) {
                outstream.write(tmp, 0, l);
                this.transferredBytes += l;
                this.listener.updateTransferred(this.transferredBytes);
            }
            outstream.flush();
        } finally {
            instream.close();

            if (isCancelled) {
                throw new IOException("Upload Cancelled");
            }
        }
    }

    public boolean isStreaming() {
        return false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
