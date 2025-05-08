package com.example.securefile.controller;

import com.example.securefile.model.SharedFile;
import com.example.securefile.repository.SharedFileRepository;
import com.example.securefile.service.EmailService;
import com.example.securefile.utils.AESUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class FileController {

    @Autowired
    private SharedFileRepository repository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                            @RequestParam String uploader,
                            @RequestParam String receiverEmail,
                            @RequestParam int expiryMinutes) throws Exception, MessagingException {

        File folder = new File("uploads");
        if (!folder.exists()) folder.mkdirs();

        String code = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        byte[] encrypted = AESUtil.encrypt(file.getBytes());

        try (FileOutputStream fos = new FileOutputStream("uploads/" + code)) {
            fos.write(encrypted);
        }

        LocalDateTime now = LocalDateTime.now();
        SharedFile sharedFile = new SharedFile(
                code,
                originalFilename,
                "uploads/" + code,
                uploader,
                receiverEmail,
                now,
                now.plusMinutes(expiryMinutes),
                false
        );

        repository.save(sharedFile);

        String downloadLink = "http://localhost:8080/download/" + code;

        String receiverSubject = "You Have Received a Secure File";
        String receiverBody = String.format(
                "Hello,<br><br>You have received a file from <b>%s</b>.<br>" +
                        "Here is the download link:<br>" +
                        "<a href='%s'>%s</a><br><br>Please note: This link will expire in %d minutes or after one-time use.",
                uploader, downloadLink, downloadLink, expiryMinutes
        );
        emailService.sendEmail(sharedFile.getReceiverEmail(), receiverSubject, receiverBody);

        return "redirect:/success.html?code=" + code;
    }

    @GetMapping("/download/{code}")
    public ResponseEntity<?> download(@PathVariable String code) throws Exception {
        SharedFile file = repository.findById(code).orElse(null);
        if (file == null || file.getEncryptedPath() == null ||
                file.isDownloaded() || file.getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(410)
                    .body("⚠️ This file has expired or has already been downloaded. Please upload again.");
        }

        file.setDownloaded(true);
        repository.save(file);

        byte[] encrypted = Files.readAllBytes(Paths.get(file.getEncryptedPath()));
        byte[] decrypted = AESUtil.decrypt(encrypted);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decrypted));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .contentLength(decrypted.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        List<SharedFile> files = repository.findAll().stream()
                .filter(f -> !f.isDownloaded() && f.getExpiryTime().isAfter(LocalDateTime.now()) && !f.isTrashed())
                .sorted((f1, f2) -> f2.getUploadTime().compareTo(f1.getUploadTime()))
                .collect(Collectors.toList());

        model.addAttribute("files", files);
        return "dashboard";
    }

    @GetMapping("/api/files")
    @ResponseBody
    public List<FileInfo> getFiles(@RequestParam(defaultValue = "uploadTime") String sortBy) {
        List<SharedFile> allFiles = repository.findAll().stream()
                .filter(f -> !f.isTrashed())
                .collect(Collectors.toList());

        if ("size".equalsIgnoreCase(sortBy)) {
            allFiles.sort((f1, f2) -> {
                try {
                    long size1 = Files.size(Paths.get(f1.getEncryptedPath()));
                    long size2 = Files.size(Paths.get(f2.getEncryptedPath()));
                    return Long.compare(size2, size1);
                } catch (IOException e) {
                    return 0;
                }
            });
        } else {
            allFiles.sort((f1, f2) -> f2.getUploadTime().compareTo(f1.getUploadTime()));
        }

        return allFiles.stream().map(FileInfo::from).collect(Collectors.toList());
    }

    @GetMapping("/history")
    public String historyPage(Model model) {
        List<SharedFile> allUploadedFiles = repository.findAll().stream()
                .filter(f -> !f.isTrashed()) // Exclude trashed files for history
                .sorted((f1, f2) -> f2.getUploadTime().compareTo(f1.getUploadTime())) // Latest first
                .collect(Collectors.toList());
    
        model.addAttribute("allFiles", allUploadedFiles);
        return "history";
    }
    
    @GetMapping("/api/history")
    @ResponseBody
    public List<FileInfo> getHistory() {
        return repository.findAll().stream()
                .filter(f -> !f.isManuallyDeleted())  // Exclude manually deleted files only
                .map(FileInfo::from)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/delete/{code}")
    public ResponseEntity<String> deleteFile(@PathVariable String code) {
        System.out.println("Received code: " + code); // Debug log
        
        // Check if the file exists in the database
        Optional<SharedFile> fileOptional = repository.findById(code);
        if (fileOptional.isPresent()) {
            SharedFile file = fileOptional.get();
            System.out.println("File found: " + file.getOriginalFilename());
            
            // Check if the file exists on disk
            Path filePath = Paths.get(file.getEncryptedPath());
            if (Files.exists(filePath)) {
                // Mark file as trashed instead of deleting it
                file.setTrashed(true);  // Move to trash
                file.setManuallyDeleted(true);  // Set manually deleted flag
                file.setTrashedAt(LocalDateTime.now()); // Set trashed time
                repository.save(file); // Save changes to the database
                
                System.out.println("🧹 Moved to Trash: " + file.getOriginalFilename());
                return ResponseEntity.ok("File moved to trash successfully.");
            } else {
                System.out.println("File not found on disk: " + file.getEncryptedPath());
                // Optionally, clean up the database entry if the file is missing on disk
                file.setTrashed(true);  // Move to trash
                file.setManuallyDeleted(true);  // Set manually deleted flag
                file.setTrashedAt(LocalDateTime.now()); // Set trashed time
                repository.save(file); // Save changes to the database
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found on disk but moved to trash in database.");
            }
        } else {
            System.out.println("File not found in database for code: " + code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
        }
    }
      

    @GetMapping("/api/cleaned")
    @ResponseBody
    public List<TrashedFileDTO> getTrashedFiles() {
        LocalDateTime now = LocalDateTime.now();

        return repository.findAll().stream()
                .filter(file ->
                        file.isTrashed() ||  // manually trashed
                        file.isDownloaded() || // auto-trash (this will now happen manually)
                        file.getExpiryTime().isBefore(now) // expired (this will not auto-delete)
                )
                .map(file -> {
                    boolean isManual = file.isTrashed() && file.getTrashedAt() != null;
                    long daysLeft = isManual
                            ? Math.max(0, Duration.between(now, file.getTrashedAt().plusDays(3)).toDays())
                            : -1;

                    return new TrashedFileDTO(
                            file.getCode(),
                            file.getOriginalFilename(),
                            file.getReceiverEmail(),
                            file.getTrashedAt(),
                            daysLeft,
                            file.isManuallyDeleted()  // Fetching the manually deleted flag
                    );
                })
                .collect(Collectors.toList());
    }

    @PutMapping("/api/cleaned/restore/{code}")
    public ResponseEntity<?> restoreFile(@PathVariable String code) {
        Optional<SharedFile> optionalFile = repository.findByCode(code);
        if (optionalFile.isPresent()) {
            SharedFile file = optionalFile.get();

            // Check if the file was manually deleted
            if (file.isManuallyDeleted()) {
                file.setTrashed(false);
                file.setTrashedAt(null);
                file.setManuallyDeleted(false);  // Reset the manually deleted flag
                repository.save(file);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("File was not manually deleted and cannot be restored.");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public static class FileInfo {
        public String code;
        public String originalFilename;
        public String uploaderName;
        public String receiverEmail;
        public LocalDateTime uploadTime;
        public LocalDateTime expiryTime;
        public boolean downloaded;
        public boolean expired;

        public static FileInfo from(SharedFile f) {
            FileInfo info = new FileInfo();
            info.code = f.getCode();
            info.originalFilename = f.getOriginalFilename();
            info.uploaderName = f.getUploaderName();
            info.receiverEmail = f.getReceiverEmail();
            info.uploadTime = f.getUploadTime();
            info.expiryTime = f.getExpiryTime();
            info.downloaded = f.isDownloaded();
            info.expired = LocalDateTime.now().isAfter(f.getExpiryTime()) || f.isDownloaded();
            return info;
        }
    }

    public record TrashedFileDTO(
            String code,
            String originalFilename,
            String receiverEmail,
            LocalDateTime trashedAt,
            long daysLeftToDelete,
            boolean manuallyDeleted  // Added manuallyDeleted flag here
    ) {}
}
