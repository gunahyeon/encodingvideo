package com.gu.encodingvideo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "요청 결과 정보", description = "요청 결과 정보")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiResponse<T> {

    LocalDateTime timestamp = LocalDateTime.now();
    int status;
    T result;
    String message;

    public static <T> ResponseEntity<ApiResponse<T>> toResponseEntity(ApiResponseCode responseCode,
        T result) {
        return ResponseEntity
            .status(responseCode.getHttpStatus())
            .body(ApiResponse.<T>builder()
                .status(responseCode.getHttpStatus().value())
                .result(result)
                .message(responseCode.getDetail())
                .build()
            );
    }

    public static <T> ResponseEntity<ApiResponse<T>> toResponseEntity(
        ApiResponseCode responseCode) {
        return toResponseEntity(responseCode, (T) "ok");
    }
}
