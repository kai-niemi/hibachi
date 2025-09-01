package io.cockroachdb.hibachi.web.common;

public class Toast {
    public static Toast of(String message) {
        return new Toast(message);
    }

    private final String message;

    public Toast(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
