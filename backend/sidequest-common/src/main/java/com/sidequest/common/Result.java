package com.sidequest.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> error(int code, String message) {
        // 生产环境下建议屏蔽详细堆栈，仅返回通用错误码
        return new Result<>(code, message, null);
    }
    
    public static <T> Result<T> forbidden() {
        return new Result<>(403, "Permission Denied", null);
    }
}
