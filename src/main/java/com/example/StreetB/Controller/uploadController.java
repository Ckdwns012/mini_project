package com.example.StreetB.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

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

            return new UploadResponse("success", "파일 업로드 성공: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            return new UploadResponse("error", "파일 업로드 실패: " + e.getMessage());
        }
    }

    // ... (listFiles, deleteFile 메서드는 그대로 유지) ...
    @GetMapping("/files")
    public String listFiles(Model model) {
        File folder = new File(FILE_DIR);
        String[] fileNames = folder.list(); // 해당 폴더의 파일 이름 배열

        if (fileNames != null) {
            model.addAttribute("files", fileNames);
        }
        return "fileListPage"; // fileListPage.html 뷰 반환
    }

    @GetMapping("/files/delete")
    @ResponseBody
    public String deleteFile(@RequestParam String filename) {
        // ... (삭제 로직 유지) ...
        File file = new File(FILE_DIR, filename);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return "삭제 성공: " + filename;
            } else {
                return "삭제 실패: " + filename;
            }
        } else {
            return "파일이 존재하지 않습니다: " + filename;
        }
    }
}
