package com.papercheck.exception;

/**
 * 命令行参数异常。
 * 设计目标：命令行的参数个数不对、或者路径是空字符串时抛出，
 * 提示用户正确的用法，而不是让数组下标越界把程序崩掉。
 */
public class ArgumentException extends PaperCheckException {

    private static final long serialVersionUID = 1L;

    public ArgumentException(String message) {
        super(message);
    }
}
