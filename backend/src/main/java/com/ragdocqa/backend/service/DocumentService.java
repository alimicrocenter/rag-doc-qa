package com.ragdocqa.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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
    private final PdfTextExtractor pdfTextExtractor;
    private final Path uploadDir;

    public DocumentService(DocumentRepository documentRepository,  PdfTextExtractor pdfTextExtractor, @Value("${app.storage.upload-dir}") String uploadDir) {  
        this.documentRepository = documentRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
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

        return process(doc);
        
    }

    private Document process(Document doc) {  
        doc.setStatus(DocumentStatus.PROCESSING);
        doc = documentRepository.save(doc);

        try{
            List<String> pages = pdfTextExtractor.extractPages(filePath(doc));
            doc.setPageCount(pages.size());

            for(int i = 0; i < pages.size(); i++){  
                String text = pages.get(i);
                log.info("Document {} page {} ({} char): {}", doc.getId(), i + 1, text.length(), text.substring(0, Math.min(200, text.length())));
            }

            doc.setStatus(DocumentStatus.READY);
        }
        catch (Exception e){
            log.warn("Processing failed for document {}", doc.getId(), e);
            doc.setStatus(DocumentStatus.FAILED);
        }

        return documentRepository.save(doc);
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
