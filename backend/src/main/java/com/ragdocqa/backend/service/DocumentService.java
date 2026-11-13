package com.ragdocqa.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ragdocqa.backend.model.Document;
import com.ragdocqa.backend.model.DocumentStatus;
import com.ragdocqa.backend.repository.DocumentRepository;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class DocumentService {
    
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final DocumentRepository documentRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final Path uploadDir;

    public DocumentService(DocumentRepository documentRepository,  PdfTextExtractor pdfTextExtractor, @Value("${app.storage.upload-dir}") String uploadDir) {  
        this.documentRepository = documentRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional(rollbackFor = IOException.class)
    public Document upload(MultipartFile file) throws IOException{

        if(file.isEmpty()){  
            throw new ResponseStatusException(BAD_REQUEST, "File is empty");
        }

        if(!isPdf(file)){ 
            throw new ResponseStatusException(BAD_REQUEST, "Only PDF files are allowed");
        }

        Document doc = new Document();
        doc.setFilename(file.getOriginalFilename() !=null ? file.getOriginalFilename() : "unnamed.pdf");
        doc.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(doc);

        Files.createDirectories(uploadDir);

        Path savedFile = uploadDir.resolve(doc.getId() + ".pdf");
        file.transferTo(savedFile);

        List<String> pages = pdfTextExtractor.extractPages(savedFile);
        doc.setPageCount(pages.size());

        for(int i = 0;  i < pages.size(); i++){  
            String text = pages.get(i);
            log.info("Document {} page {} ({} chars): {}",
             doc.getId(), 
             i + 1, text.length(), 
             text.substring(0, Math.min(200, text.length())));
        }

        return doc;
        // file.transferTo(uploadDir.resolve(doc.getId() + ".pdf"));
        // return doc;

        
    }

    private boolean isPdf(MultipartFile file) throws IOException{  
            try (InputStream in = file.getInputStream()) {  
                return Arrays.equals(in.readNBytes(PDF_MAGIC.length), PDF_MAGIC);
            }
        }

}
