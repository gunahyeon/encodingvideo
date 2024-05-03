package com.gu.encodingvideo;

import com.gu.encodingvideo.exception.BadRequestException;
import com.gu.encodingvideo.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    String basePath;

    @Value("${app-config.file-upload.encoding-path}")
    String encodingPath;

        /**
         * 변환
         */
        @Async
        @Transactional
        public CompletableFuture<String> convertMp4ToWebmFile(MultipartFile file) {
            String[] defaultCmd = new String[]{
                    getCWebMPath(),
                    "-i"
            };

            List<String> cmd = new ArrayList<>(List.of(defaultCmd));
            ProcessBuilder builder = new ProcessBuilder();
            List<Process> processes = new ArrayList<>();
            List<String> videoExtensions = Arrays.asList(".mp4", ".avi", ".mov", ".webm", ".wmv"); // todo: webm 일때 바로 저장
            String savedFilePath = basePath + encodingPath + File.separator + FilenameUtils.removeExtension(file.getOriginalFilename());
            String savedFilePathString = basePath + encodingPath + File.separator + file.getOriginalFilename();
            Path encodingFolderPath = Path.of(basePath, encodingPath);

            if (videoExtensions.stream().noneMatch(file.getOriginalFilename().toLowerCase()::endsWith)) {
                throw new BadRequestException(ErrorCode.INVALID_FILE);
            }

            try (BufferedInputStream input = new BufferedInputStream(file.getInputStream());
                    BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(Path.of(savedFilePathString)))
            ) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                log.error("Failed to save the file: {}", e.getMessage());
            }

            File f = new File(savedFilePathString);

            String outputFile = encodingFolderPath.resolve(
                    savedFilePath + ".webm"
            ).toAbsolutePath().toString();

            builder.directory(f.getParentFile());
            cmd.add(f.getName());
            cmd.add("-c:v");
            cmd.add("libvpx-vp9"); // 비디오 인코딩(VP9 코덱)
            cmd.add("-b:v"); // 비디오 비트레이트 설정, 파일 크기 조정 가능
            cmd.add("1M"); // 1Mbps
            cmd.add("-crf"); // VP9 코덱의 화질 설정
            cmd.add("30"); // 값이 클수록 화질이 낮아짐
//            cmd.add("-vf"); // 비디오의 높이
//            cmd.add("scale=-1:2160"); // 720픽셀로 조정
            cmd.add("-an"); // 오디오 제거
            cmd.add("-y"); // 덮어쓰기
            cmd.add(outputFile);
            builder.command(cmd);
            // todo: 업로드 가능한 파일 수는 1개여서 다른 태스크를 넣을 때 진행중이던 프로세스(쓰레드) 중지하고 클리어 한다음에 재 시작 필요
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
                try {
                    // 프로세스 출력 및 에러 스트림을 읽기 위한 스레드 생성
                    Thread outputThread = new Thread(() -> {
                        try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(prc.getErrorStream()))) {
                            String outputLine;
                            while ((outputLine = outputReader.readLine()) != null) {
                                log.info("FFmpeg Output: {}", outputLine);
                            }
                        } catch (IOException e) {
                            log.error("Failed to read FFmpeg output: {}", e.getMessage());
                        }
                    });

                    // 출력 읽기 스레드 시작
                    outputThread.start();
                    int exitCode = prc.waitFor();
                    log.info("FFmpeg Exit Code: {}", exitCode);
                    prc.destroy();
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
            /* 변환 완료 된 파일 삭제 */
            f.delete();

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
    }
