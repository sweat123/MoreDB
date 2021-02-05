package com.laomei.moredb.common.exception;

/**
 * @author luobo.hwz on 2021/01/19 17:18
 */
public class MoreDBException extends RuntimeException {

    public MoreDBException(String message) {
        super(message);
    }

    public MoreDBException(Throwable cause) {
        super(cause);
    }

    public MoreDBException(String message, Throwable cause) {
        super(message, cause);
    }
}
