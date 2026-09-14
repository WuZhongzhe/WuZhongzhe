package com.papercheck.exception;

/**
 * 文件读写异常。
 * 设计目标：文件不存在、路径其实是个文件夹、没有读权限、编码读不了、
 * 答案文件写不进去等情况统一抛出，并把原始 IOException 作为原因保存下来方便排查。
 */
public class FileProcessException extends PaperCheckException {

    private static final long serialVersionUID = 1L;

    public FileProcessException(String message) {
        super(message);
    }

    public FileProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
