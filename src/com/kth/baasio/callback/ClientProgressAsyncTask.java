
package com.kth.baasio.callback;

import android.os.AsyncTask;

public abstract class ClientProgressAsyncTask<T> extends AsyncTask<Void, ProgressInfo, T> {

    ClientProgressCallback<T> mCallback;

    public ClientProgressAsyncTask(ClientProgressCallback<T> callback) {
        this.mCallback = callback;
    }

    @Override
    protected T doInBackground(Void... v) {
        try {
            return doTask();
        } catch (Exception e) {
            ProgressInfo info = new ProgressInfo();
            info.setException(e);
            this.publishProgress(info);
        }
        return null;
    }

    public abstract T doTask();

    public abstract void doCancel();

    @Override
    protected void onCancelled() {
        doCancel();
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(T response) {
        if (mCallback != null) {
            mCallback.onResponse(response);
        }
    }

    @Override
    protected void onProgressUpdate(ProgressInfo... info) {
        if ((mCallback != null) && (info != null) && (info.length > 0)) {
            if (info[0].getException() != null) {
                mCallback.onException(info[0].getException());
            } else if (info[0].getTotalSize() != null && info[0].getCurrentSize() != null) {
                mCallback.onProgress(info[0].getTotalSize(), info[0].getCurrentSize());
            }
        }
    }
}
