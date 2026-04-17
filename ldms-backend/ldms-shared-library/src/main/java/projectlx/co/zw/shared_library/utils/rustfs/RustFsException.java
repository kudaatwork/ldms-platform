package projectlx.co.zw.shared_library.utils.rustfs;

import lombok.Getter;

@Getter
public class RustFsException extends RuntimeException {

    private final int statusCode;
    private final String errorType;

    public RustFsException(int statusCode, String errorType, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public RustFsException(int statusCode, String errorType, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }
}
