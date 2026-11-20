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
    private final TextChunker textChunker;
    private final ChunkEmbedder chunkEmbedder;
    private final Path uploadDir;

    public DocumentService(DocumentRepository documentRepository,  PdfTextExtractor pdfTextExtractor, TextChunker textChunker, ChunkEmbedder chunkEmbedder, @Value("${app.storage.upload-dir}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.chunkEmbedder = chunkEmbedder;
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

           List<float[]> vectors = chunkEmbedder.embed(chunks);
           log.info("Document {}: embedded {} chunks, {} dimensions each", doc.getId(), vectors.size(), vectors.get(0).length);

           for(int i = 0; i < chunks.size(); i++){
            String text = chunks.get(i).getText();
            float[] v = vectors.get(i);
            log.info("  page {} ({} chars) -> [{}, {}, {}, ...]: {}",
            chunks.get(i).getMetadata().get("page"),
            text.length(),
            v[0], v[1], v[2],
            text.substring(0, Math.min(100, text.length())).replaceAll("\\s+", " "));
           }

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
