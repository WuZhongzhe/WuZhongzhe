package com.papercheck.exception;

/**
 * 查重程序的异常基类。
 * 设计目标：把"参数错误""文件读写错误"等可预期的错误统一收拢，
 * 让 Main 只需要捕获一个类型就能给出提示并正常退出，避免异常直接抛到控制台导致程序异常退出。
 */
public class PaperCheckException extends Exception {

    private static final long serialVersionUID = 1L;

    public PaperCheckException(String message) {
        super(message);
    }

    public PaperCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}
