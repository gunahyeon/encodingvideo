package com.gu.encodingvideo;

import com.gu.encodingvideo.dto.ApiResponse;
import com.gu.encodingvideo.dto.ApiResponseCode;
import com.gu.encodingvideo.exception.BadRequestException;
import com.gu.encodingvideo.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j(topic = "EncodingController")
@RequestMapping("/api/v1")
@Tag(name = "Encoding API", description = "비디오 인코딩 API")
public class EncodingController {

    private final EncodingService encodingService;

    @PostMapping(value = "/encode", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "비디오 인코딩", description = "MP4 비디오를 WebM 형식으로 인코딩합니다.")
    public ResponseEntity<ApiResponse<String>> encodeVideo(@RequestParam("file") MultipartFile file) {
        try {
            CompletableFuture<String> future = encodingService.convertMp4ToWebmFile(file);

            String result = future.get();

            return ApiResponse.toResponseEntity(ApiResponseCode.RESPONSE_OK, result);
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(ErrorCode.INVALID_FILE);
        }
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "인코딩 상태 조회", description = "비디오 인코딩 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<String>> getEncodingStatus(@PathVariable("id") String id) {
        String status = "인코딩 중";
        return ApiResponse.toResponseEntity(ApiResponseCode.RESPONSE_OK, id);
    }
}
