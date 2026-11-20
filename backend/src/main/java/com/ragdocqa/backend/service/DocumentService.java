package com.ragdocqa.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


import org.springframework.ai.vectorstore.VectorStore;

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
    private final TextChunker textChunker;
    private final VectorStore vectorStore;
    private final Path uploadDir;

    public DocumentService(DocumentRepository documentRepository,  PdfTextExtractor pdfTextExtractor, TextChunker textChunker, VectorStore vectorStore, @Value("${app.storage.upload-dir}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.vectorStore = vectorStore;
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

           List<org.springframework.ai.document.Document> chunks = textChunker.chunk(doc.getId(), doc.getFilename(), pages);

           if(chunks.isEmpty()){  
            throw new IllegalStateException(  
               "No text found in " + pages.size() + " pages. The PDF may be a scanned image and would need OCR."

            );
           }

           log.info("Document {}: {} pages -> {} chunks", doc.getId(), pages.size(), chunks.size());

           try {
            
            vectorStore.add(chunks);

           } catch (RuntimeException e) {
            throw new IllegalStateException(  
                "Could not create embeddings. Check that Ollama is running and the bge-m3 model is pulled", e
            );
           }
           log.info("Document {}: saved {} chunks to vector_store", doc.getId(), chunks.size());
            doc.setStatus(DocumentStatus.READY);
        }
        catch (IllegalStateException e){
           log.warn("Processing failed for document {}: {}", doc.getId(), e.getMessage());
           doc.setStatus(DocumentStatus.FAILED);
           doc.setErrorMessage(e.getMessage());
        }
        catch(Exception e){  
            log.warn("Processing failed for document {}", doc.getId(), e);
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage("Could not read this PDF. The file may be damaged or password-protected");

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
