package com.ragdocqa.backend.dto;
import java.time.Instant;

import com.ragdocqa.backend.model.Document;
import com.ragdocqa.backend.model.DocumentStatus;



public record DocumentResponse(  
    Long id,
    String filename,
    DocumentStatus status,
    Integer pageCount,
    Instant uploadedAt
){  
    public static DocumentResponse from(Document doc){
        return new DocumentResponse(
            doc.getId(),
            doc.getFilename(),
            doc.getStatus(),
            doc.getPageCount(),
            doc.getUploadedAt()
        );
    }
}