package com.gu.encodingvideo.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    SOMETHING_WRONG(INTERNAL_SERVER_ERROR, "뭔가 잘못됐습니다🙈"),

    NO_DATA(BAD_REQUEST, "값이 입력되지 않았습니다"),
    NO_IMG_DATA(BAD_REQUEST, "이미지 값이 입력되지 않았습니다"),
    TYPE_MISMATCH(BAD_REQUEST, "타입이 올바르지 않습니다"),
    TASK_IS_NOT_EXIST(BAD_REQUEST, "존재하지 않는 태스크 입니다"),
    TASK_IS_NOT_COMPlETE(BAD_REQUEST, "완료되지 않은 태스크 입니다"),
    ROOM_IS_NOT_EXIST(BAD_REQUEST, "존재하지 않는 방입니다"),
    FOLDER_IS_NOT_EXIST(BAD_REQUEST, "존재하지 않는 폴더 입니다"),

    /* FILE : 파일 */
    EXCEED_MAX_UPLOAD_SIZE(PAYLOAD_TOO_LARGE, "서버에서 허용한 파일 크기를 초과했습니다."),
    INVALID_FILE(UNPROCESSABLE_ENTITY, "부적절한 파일입니다."),
    UPLOAD_FAILED(INTERNAL_SERVER_ERROR, "업로드를 실패하였습니다."),
    FILE_NOT_FOUND(NOT_FOUND, "존재하지 않는 파일입니다."),
    EXCEED_MAX_UPLOAD_COUNT(BAD_REQUEST, "서버에서 허용한 태스크 개수를 초과했습니다. 잠시후 시도해주세요."),
    DELETED_FAILED(INTERNAL_SERVER_ERROR, "삭제를 실패하였습니다."),
    NO_REQUIRED_FILE(BAD_REQUEST, "필수 파일이 입력되지 않았습니다.");

    private final HttpStatus httpStatus;
    private final String detail;
}
