package com.ragdocqa.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ragdocqa.backend.exception.InvalidFileException;
import com.ragdocqa.backend.model.Document;
import com.ragdocqa.backend.model.DocumentStatus;
import com.ragdocqa.backend.repository.DocumentRepository;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class DocumentService {
    
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final DocumentRepository documentRepository;
    private final Path uploadDir;
    private final DocumentProcessor documentProcessor;

    public DocumentService(DocumentRepository documentRepository,  DocumentProcessor documentProcessor, @Value("${app.storage.upload-dir}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.documentProcessor = documentProcessor;
    }

    public Document upload(MultipartFile file) throws IOException{

        if(file.isEmpty()){  
            throw new InvalidFileException( "File is empty");
        }

        if(!isPdf(file)){ 
            throw new InvalidFileException("Only PDF files are allowed");
        }

        Document doc = new Document();
        doc.setFilename(file.getOriginalFilename() !=null ? file.getOriginalFilename() : "unnamed.pdf");
        doc.setStatus(DocumentStatus.UPLOADED);
        doc = documentRepository.save(doc);

        Files.createDirectories(uploadDir);
        try {
            file.transferTo(filePath(doc));
        } catch (IOException e){
            documentRepository.delete(doc);
            throw e;

        }

        documentProcessor.process(doc.getId(), filePath(doc));
        return doc;
    }


    private Path filePath(Document doc){  
        return uploadDir.resolve(doc.getId() + ".pdf");
    }

    private boolean isPdf(MultipartFile file) throws IOException{  
            try (InputStream in = file.getInputStream()) {  
                return Arrays.equals(in.readNBytes(PDF_MAGIC.length), PDF_MAGIC);
            }
        }

}
