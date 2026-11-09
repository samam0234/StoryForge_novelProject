package com.storyforge.controller;

import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 표지 이미지 파일. 읽기는 공개, 올리기는 로그인 사용자만. */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private final Path root = Path.of("uploads").toAbsolutePath().normalize();

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidStateException("이미지 파일이 없습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidStateException("이미지는 5MB 이하만 올릴 수 있습니다.");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!TYPES.contains(type)) {
            throw new InvalidStateException("jpeg, png, webp, gif 이미지만 올릴 수 있습니다.");
        }
        String ext = switch (type) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
        Files.createDirectories(root);
        String name = UUID.randomUUID() + "." + ext;
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root)) {
            throw new InvalidStateException("잘못된 파일 경로입니다.");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }
        return Map.of("url", "/api/v1/media/" + name);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> read(@PathVariable String name) {
        if (!name.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp|gif)")) {
            throw new NotFoundException("이미지를 찾을 수 없습니다.");
        }
        Path file = root.resolve(name).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new NotFoundException("이미지를 찾을 수 없습니다.");
        }
        String type = switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "image/jpeg";
        };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(new FileSystemResource(file));
    }
}
