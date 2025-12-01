package com.example.StreetB.Controller;

import com.example.StreetB.Service.aiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// 응답을 위한 간단한 DTO 클래스 (UploadResponse.java 파일로 별도 생성 권장)
class UploadResponse {
    public String status;
    public String message;
    public UploadResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
}

@Controller
public class uploadController {
    @Autowired
    private aiService aiService;
    private final String UPLOAD_DIR = "/Users/ichangjun/Documents/StreetB/uploads";
    private final String FILE_DIR = "/Users/ichangjun/Documents/StreetB/uploads";

    @RequestMapping("uploadPage")
    public String uploadPage(){
        return "uploadPage"; // uploadPage.html 뷰 반환
    }

    // JSON 응답을 위해 @ResponseBody 추가, 반환 타입 UploadResponse로 변경
    @PostMapping("/upload")
    @ResponseBody
    public UploadResponse uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new UploadResponse("error", "업로드할 파일이 없습니다.");
        }
        try {
            // 폴더 없으면 생성
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            File dest = Paths.get(UPLOAD_DIR, filename).toFile();
            file.transferTo(dest);
            aiService.initializeDocumentStore();
            return new UploadResponse("success", "파일 업로드 성공: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            return new UploadResponse("error", "파일 업로드 실패: " + e.getMessage());
        }
    }

    // [수정] Thymeleaf 뷰 반환 엔드포인트
    @GetMapping("/files")
    public String listFiles(Model model) {
        File folder = new File(UPLOAD_DIR);

        // 파일 필터링: 숨김파일 제외, 파일만 표시
        String[] files = folder.list((dir, name) -> {
            // 숨김 파일 제외
            if (name.startsWith(".")) return false;

            // 확장자 필터링(원하면 추가)
            String lower = name.toLowerCase();
            return lower.endsWith(".pdf") ||
                    lower.endsWith(".txt") ||
                    lower.endsWith(".hwp") ||
                    lower.endsWith(".jpg") ||
                    lower.endsWith(".png") ||
                    lower.endsWith(".jpeg");
        });

        model.addAttribute("files", files);
        return "fileListPage";
    }

    // [추가] JavaScript fetch API를 위한 JSON 반환 엔드포인트
    @GetMapping("/api/files")
    @ResponseBody // JSON 반환
    public List<String> apiListFiles() {
        File folder = new File(FILE_DIR);
        String[] fileNames = folder.list();
        if (fileNames != null) {
            return Arrays.stream(fileNames).collect(Collectors.toList());
        }
        return List.of(); // 빈 리스트 반환
    }


    @GetMapping("/files/delete")
    @ResponseBody
    public String deleteFile(@RequestParam String filename) {
        // ... (삭제 로직 유지) ...
        File file = new File(FILE_DIR, filename);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                aiService.initializeDocumentStore();
                return "삭제 성공: " + filename;
            } else {
                return "삭제 실패: " + filename;
            }
        } else {
            return "파일이 존재하지 않습니다: " + filename;
        }
    }

    @GetMapping("/files/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) {
        try {
            // ... (다운로드 로직 유지) ...
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = "application/octet-stream";
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
