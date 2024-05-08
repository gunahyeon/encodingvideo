package com.gu.encodingvideo;

import com.gu.encodingvideo.exception.BadRequestException;
import com.gu.encodingvideo.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class EncodingService {
    final String OS = System.getProperty("os.name").toLowerCase();
    public boolean IS_MAC = (OS.contains("mac"));
    public boolean IS_UNIX = (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0);
    public boolean IS_WINDOWS = OS.contains("windows");

    @Value("${app-config.file-upload.base-path}")
    private String basePath;

    @Value("${app-config.file-upload.encoding-path}")
    private String encodingPath;

    private final SimpMessagingTemplate messagingTemplate;

    /* 현재 실행 중인 프로세스 목록 */
    private final List<Process> processes = new CopyOnWriteArrayList<>();

    /* 프로세스 진행 상태 */
    private Boolean status = false;

    /**
     * 변환
     */
    @Async
    @Transactional
    public CompletableFuture<String> convertMp4ToWebmFile(MultipartFile file) {
        stopAllProcesses();

        if (status)
            throw new BadRequestException(ErrorCode.EXCEED_MAX_UPLOAD_COUNT);

        ProcessBuilder builder = new ProcessBuilder();
        List<String> videoExtensions = Arrays.asList("mp4", "avi", "mov", "mpg", "wmv", "mpeg", "webm");
        String inputFileString = basePath + encodingPath + File.separator + file.getOriginalFilename();
        Path encodingFolderPath = Path.of(basePath, encodingPath);

        if (videoExtensions.stream().noneMatch(file.getOriginalFilename().toLowerCase()::endsWith)) {
            throw new BadRequestException(ErrorCode.INVALID_FILE);
        }

        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream());
                BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(Path.of(inputFileString)))
        ) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error("Failed to save the file: {}", e.getMessage());
        }

        /* .webm 확장자일때 변환 생략 */
        if (!file.getOriginalFilename().toLowerCase().endsWith(".webm")) {
            File inputFile = new File(inputFileString);

            File outputFile = new File(encodingFolderPath.resolve(
                FilenameUtils.removeExtension(inputFileString) + ".webm"
            ).toAbsolutePath().toString());

            String outputFileString = String.valueOf(outputFile);

            builder.directory(inputFile.getParentFile());

            List<String> cmd = new ArrayList<>(List.of(
                    getCWebMPath(),
                    "-i",
                    inputFile.getName(),
                    "-c:v",
                    "libvpx-vp9",
                    "-vf",
                    "scale=1920:1080",
                    "-an",
                    "-y",
                    outputFileString
            ));

            builder.command(cmd);

            try {
                log.info("BUILDER: {}", builder.directory());
                log.info("COMMAND: {}", builder.command());
                Process process = builder.start();
                processes.add(process);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new BadRequestException(ErrorCode.INVALID_FILE);
            }

            for (Process prc : processes) {
                status = true;
                try {
                    /* 프로세스 출력 및 에러 스트림을 읽기 위한 스레드 */
                    Thread outputThread = new Thread(() -> {
                        try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(prc.getErrorStream()))) {
                            String outputLine;
                            String duration = null;
                            String time;
                            while ((outputLine = outputReader.readLine()) != null && status) {
                                /* 인코딩 할 총 시간 */
                                if (outputLine.contains("Duration: ")) {
                                    String remaining = outputLine.substring(outputLine.indexOf("Duration: ") + "Duration: ".length()).trim();
                                    duration = remaining.substring(0, 11);
                                    messagingTemplate.convertAndSend("/sub/message/duration", duration);
                                }

                                /* 인코딩 진행 중인 구간 */
                                if (duration != null) {
                                    if (outputLine.contains("time=") && !outputLine.contains("time=N/A")) {
                                        String remaining = outputLine.substring(outputLine.indexOf("time=") + "time=".length()).trim();
                                        time = remaining.substring(0, 11);
                                        messagingTemplate.convertAndSend("/sub/message/time", time);
                                    }
                                }

                                log.info("FFmpeg Output: {}", outputLine);
                            }
                        } catch (IOException e) {
                            log.error("Failed to read FFmpeg output: {}", e.getMessage());
                        }
                    });

                    /* 출력 읽기 스레드 시작 */
                    outputThread.start();
                    int exitCode = prc.waitFor();
                    log.info("FFmpeg Exit Code: {}", exitCode);
                    prc.destroy();
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
            /* 변환 완료 된 파일 삭제 */
            inputFile.delete();
        }

        status = false;

        return CompletableFuture.completedFuture(file.getOriginalFilename());
    }

    /**
     * FFmpeg WebM 변환 도구를 사용하여 비디오 변환
     */
    public String getCWebMPath() {
        Path binaryPaths = Paths.get(
                basePath,
                "binary",
                IS_UNIX ? "linux" :
                        IS_MAC ? "mac" :
                                IS_WINDOWS ? "windows" : "",
                "bin",
                "ffmpeg");
        if (IS_UNIX || IS_WINDOWS || IS_MAC) {
            return binaryPaths.toAbsolutePath().toString();
        } else {
            return null;
        }
    }

    /**
     * 모든 프로세스 중지
     */
    public void stopAllProcesses() {
        if (!processes.isEmpty()) {
            for (Process process : processes) {
                process.destroy();
            }

            processes.clear();
            status = false;
        }

        try {
            File encodingFolder = new File(basePath + File.separator + encodingPath);
            FileUtils.cleanDirectory(encodingFolder); //하위 폴더와 파일 모두 삭제
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
