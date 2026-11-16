package com.ragdocqa.backend.exception;

public class DocumentNotFoundException extends RuntimeException {  
    public DocumentNotFoundException(Long id) {  
        super("Document " + id + " not found");  
    }
}