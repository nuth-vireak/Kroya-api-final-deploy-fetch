package com.kshrd.kroya_api.controller;

import com.kshrd.kroya_api.entity.FileEntity;
import com.kshrd.kroya_api.payload.File.FileResponse;
import com.kshrd.kroya_api.service.File.FileService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fileView")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(
            summary = "📤 Upload Multiple Files",
            description = """
                    Uploads one or more files to the server.
                    - **Request Parameter**: **files** (List of `MultipartFile`): Files to be uploaded.
                    
                    **📩 Response Summary**:
                    - **201**: ✅ Files uploaded successfully, returns URLs of the uploaded files.
                    - **400**: 🚫 Invalid file format or missing files.
                    """
    )
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFiles(@RequestParam("files") List<MultipartFile> files) throws IOException {
//        List<String> fileUrl = new ArrayList<>();
//        for (MultipartFile file : files) {
//            String fileName = fileService.Uplaodfile(file);
//            String url = ServletUriComponentsBuilder.fromCurrentRequestUri()
//                    .replacePath("/api/v1/fileView/" + fileName)
//                    .toUriString();
//            FileEntity fileEntity = new FileEntity(url, fileName);
//            fileService.InsertFile(fileEntity);
//            fileUrl.add(url);
//        }
//        return ResponseEntity.ok().body(new FileResponse<>(
//                "Upload files successfully",
//                201,
//                fileUrl
//        ));
        return null;
    }

    @Operation(
            summary = "📥 Download File by Name",
            description = """
                    Retrieves a file from the server based on its name.
                    - **Path Variable**: **fileName** (String): Name of the file to be downloaded.
                    
                    **📩 Response Summary**:
                    - **200**: ✅ File retrieved successfully.
                    - **404**: 🚫 File not found.
                    """
    )
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) throws IOException {
//        Resource file = fileService.getFile(fileName);
//        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(file);
        return null;
    }
}

