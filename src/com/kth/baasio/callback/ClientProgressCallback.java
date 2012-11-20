
package com.kth.baasio.callback;

public interface ClientProgressCallback<T> {
    public void onResponse(T response);

    public void onException(Exception e);

    public void onProgress(long total, long current);
}
