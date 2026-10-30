package backend.error;

import java.util.Map;

public record ErrorResponse(String code, String message, Map<String, String> details) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Map.of());
    }
}
