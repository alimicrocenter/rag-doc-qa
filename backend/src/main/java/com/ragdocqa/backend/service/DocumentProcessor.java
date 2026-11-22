package com.ragdocqa.backend.service;

import java.nio.file.Path;
import java.util.List;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ragdocqa.backend.model.Document;
import com.ragdocqa.backend.model.DocumentStatus;
import com.ragdocqa.backend.repository.DocumentRepository;

import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j
public class DocumentProcessor {
    
    private final DocumentRepository documentRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;

    public DocumentProcessor(  
        DocumentRepository documentRepository, 
        PdfTextExtractor pdfTextExtractor, 
        TextChunker textChunker, 
        VectorStore vectorStore  
    ) {  
        this.documentRepository = documentRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.vectorStore = vectorStore;
    }

    @Async
    public void process(Long documentId, Path file){  
        Document doc = documentRepository.findById(documentId).orElse(null);
        if(doc == null){  
            log.warn("Document {} not found, skipping processing", documentId);
            return;
        }

        doc.setStatus(DocumentStatus.PROCESSING);
        doc = documentRepository.save(doc);

        try {  
            List<String> pages = pdfTextExtractor.extractPages(file);
            doc.setPageCount(pages.size());

            List<org.springframework.ai.document.Document> chunks = textChunker.chunk(doc.getId(), doc.getFilename(), pages);

            if(chunks.isEmpty()){  
                throw new IllegalStateException(  
                    "No text found in " + pages.size() + " pages. The PDF may be a scanned image and would need OCR"
                );
                
            }
            log.info("Document {}: {} pages -> {} chunks", doc.getId(), pages.size(), chunks.size());

            try{  
                vectorStore.add(chunks);
            } catch(RuntimeException e) {  
                throw new IllegalStateException("Could not create embedding. Check that Ollama is running and the bge-m3 model is pulled", e);
            }

            log.info("Document {}: saved {} chunks to vector_store", doc.getId(), chunks.size());
            doc.setStatus(DocumentStatus.READY);
       } catch (IllegalStateException e) {
            log.warn("Processing failed for document {}: {}", doc.getId(), e.getMessage());
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.warn("Processing failed for document {}", doc.getId(), e);
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage("Could not read this PDF. The file may be damaged or password-protected");
        }

        documentRepository.save(doc);
    }
}
