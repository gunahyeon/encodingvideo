package com.gu.encodingvideo;

import com.gu.encodingvideo.dto.ApiResponse;
import com.gu.encodingvideo.dto.ApiResponseCode;
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
    @Operation(summary = "비디오 인코딩", description = "MP4 비디오를 WebM 형식으로 인코딩")
    public ResponseEntity<ApiResponse<String>> encodeVideo(@RequestParam("file") MultipartFile file) throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = encodingService.convertMp4ToWebmFile(file);

        String result = future.get();

        return ApiResponse.toResponseEntity(ApiResponseCode.RESPONSE_OK, result);
    }

    @DeleteMapping("/stop")
    @Operation(summary = "인코딩 중지", description = "비디오 인코딩 중지")
    public ResponseEntity<ApiResponse<Void>> stopAllProcesses() {
        encodingService.stopAllProcesses();
        return ApiResponse.toResponseEntity(ApiResponseCode.RESPONSE_OK);
    }
}
