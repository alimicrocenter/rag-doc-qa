package com.ragdocqa.backend.controller;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ragdocqa.backend.dto.DocumentResponse;
import com.ragdocqa.backend.repository.DocumentRepository;




@RestController
@RequestMapping("api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public List<DocumentResponse> list(){  
        return documentRepository.findAllByOrderByUploadedAtDescIdDesc()
        .stream()
        .map(DocumentResponse::from)
        .toList();
    }
    
}
