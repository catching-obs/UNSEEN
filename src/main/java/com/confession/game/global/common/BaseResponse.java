package com.confession.game.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private String type;
    private T data;

    public static <T> BaseResponse<T> of(String type, T data) {
        return new BaseResponse<>(type, data);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>("error", (T) new ErrorData(message));
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorData {
        private String message;
    }
}