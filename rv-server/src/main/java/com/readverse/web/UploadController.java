package com.readverse.web;

import com.readverse.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class UploadController {

    @Value("${rv.upload-path:./uploads/}")
    private String uploadPath;

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @PostMapping("/cover")
    @PreAuthorize("hasAuthority('content:book:add') or hasAuthority('content:book:edit')")
    public R<String> cover(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return R.fail(40000, "请选择图片文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return R.fail(40000, "图片不能超过 5MB");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "cover.png" : file.getOriginalFilename());
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "png";
        if (!ALLOWED.contains(ext)) {
            return R.fail(40000, "仅支持 jpg/png/webp/gif 图片");
        }
        Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        file.transferTo(dir.resolve(filename).toFile());
        return R.ok("/uploads/" + filename);
    }
}
