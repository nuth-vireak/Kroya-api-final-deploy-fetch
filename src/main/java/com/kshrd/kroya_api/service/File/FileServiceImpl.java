package com.kshrd.kroya_api.service.File;

import com.kshrd.kroya_api.entity.FileEntity;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.repository.File.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private final FileRepository fileRepository;
    private final Path root = Paths.get("src/main/resources/Datauplaod");
//    private final Path root= Paths.get("/home/hrd123/easycartImage/");

    public FileServiceImpl(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public FileEntity InsertFile(FileEntity fileEntity) {
        return fileRepository.save(fileEntity);
    }

    @Override
    public String Uplaodfile(MultipartFile file) throws IOException {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName != null) {
                fileName = UUID.randomUUID() + "." + StringUtils.getFilenameExtension(fileName);
                if (!Files.exists(root)) {
                    Files.createDirectories(root);
                }
                Files.copy(file.getInputStream(), root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                return fileName;
            } else {
                return "File Not Found!";
            }
        } catch (IOException ex) {
            throw new IOException("File not found!");
        }
    }

    @Override
    public Resource getFile(String fileName) {
        try {
            // Find the file entity from the repository
            FileEntity files = fileRepository.findByFileName(fileName);
            if (files == null) {
                throw new FileNotFoundException("File not found with name: " + fileName);
            }

            // Define the path to the file
            Path path = Paths.get("src/main/resources/Datauplaod/" + files.getFileName());
            // Read the file into a Resource object
            Resource file = new ByteArrayResource(Files.readAllBytes(path));
            return file;

        } catch (FileNotFoundException e) {
            // Handle the case when the file entity is not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);

        } catch (IOException e) {
            // Handle issues with file access
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading file: " + fileName, e);

        } catch (Exception e) {
            // Handle any other unexpected errors
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", e);
        }
    }

}
