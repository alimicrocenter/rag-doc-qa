package com.ragdocqa.backend.dto;

import java.util.Map;

import org.springframework.ai.document.Document;

public record SearchResult (
    String chunkId,
    String text,
    Double score,
    Long documentId,
    String filename,
    Integer page
    
 ) {  

    public static SearchResult from(Document chunk) {  
        Map<String, Object> md = chunk.getMetadata();
        return new SearchResult(  
            chunk.getId(),
            chunk.getText(),
            chunk.getScore(),
            md.get("documentId") instanceof Number n ? n.longValue() : null,
            (String) md.get("filename"),
            md.get("page") instanceof Number n ? n.intValue() : null
        );
    }
}
