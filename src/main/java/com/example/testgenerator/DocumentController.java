package com.example.testgenerator;

import com.example.testgenerator.api.DocumentApi;
import com.example.testgenerator.client.ApiClient;
import com.example.testgenerator.client.ApiException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
public class DocumentController implements DocumentApi {
    private static final Log log = LogFactory.getLog(DocumentController.class);

    private com.example.testgenerator.client.api.DocumentApi client;

    @PostConstruct
    void init() {
        var api = new ApiClient();
        api.updateBaseUri("http://localhost:8080");

        this.client = new com.example.testgenerator.client.api.DocumentApi(api);
    }

    @Override
    public ResponseEntity<Void> uploadDocuments(List<MultipartFile> files) {
        saveFile(files);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/document-test", produces = "application/json", consumes = { "multipart/form-data", "multipart/mixed" })
    ResponseEntity<Object> testVerifyDocument(
            @Parameter(name = "files", description = "", required = true) @RequestPart(value = "files") List<MultipartFile> files
    ) {
        try {
            // When invoking this client method the API responses with
            // org.springframework.web.multipart.support.MissingServletRequestPartException: Required part 'files' is not present.
            var response = client.uploadDocumentsWithHttpInfo(files.stream().map(this::toFile).toList());
            log.info("Received response with status code " + response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).build();
        } catch(ApiException e) {
            log.error("Error received " + e.getCode(), e);
            return ResponseEntity.internalServerError().body(e.getResponseBody());
        }
    }

    public File toFile(MultipartFile multipartFile) {
        try {
            byte[] fileBytes = multipartFile.getBytes();

            String fileName = multipartFile.getOriginalFilename();
            Path filePath = Files.createTempFile(fileName, "");

            File convertedFile = new File(filePath.toUri());

            Files.write(convertedFile.toPath(), fileBytes);

            return convertedFile;
        } catch(IOException e) {
            log.error("Error during converting MultipartFile " + multipartFile.getOriginalFilename(), e);
            throw new IllegalStateException("Error during converting MultipartFile " + multipartFile.getOriginalFilename(), e);
        }
    }

    private void saveFile(List<MultipartFile> files) {
        files.forEach(this::save);
    }

    private void save(MultipartFile file) {
        if (!file.isEmpty()) {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path uploadPath = Path.of("UPLOAD_DIR");

            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                fileName = System.currentTimeMillis() + "-" + fileName;
                Path filePath = uploadPath.resolve(fileName);

                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Saved file " + fileName + " in " + filePath);
            } catch(IOException e) {
                log.error("Error during saving MultipartFile " + fileName, e);
            }
        }
    }
}
