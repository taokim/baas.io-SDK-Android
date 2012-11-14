
package com.kth.baasio.callback;

public class ProgressInfo {
    private Exception e;

    private Long total;

    private Long current;

    /**
     * @return the total size
     */
    public Long getTotalSize() {
        return total;
    }

    /**
     * @param total the total size to set
     */
    public void setTotalSize(long total) {
        this.total = total;
    }

    /**
     * @return the current size
     */
    public Long getCurrentSize() {
        return current;
    }

    /**
     * @param current the total size to set
     */
    public void setCurrentSize(long current) {
        this.current = current;
    }

    /**
     * @return the e
     */
    public Exception getException() {
        return e;
    }

    /**
     * @param e the e to set
     */
    public void setException(Exception e) {
        this.total = null;
        this.current = null;
        this.e = e;
    }

}
