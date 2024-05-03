package com.gu.encodingvideo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.OK;

@Getter
@AllArgsConstructor
public enum ApiResponseCode {

    RESPONSE_OK(OK, "요청이 완료됐습니다."),
    UPLOAD_OK(OK, "업로드가 완료되었습니다."),
    //********************************
    ;

    private final HttpStatus httpStatus;
    private final String detail;
}
