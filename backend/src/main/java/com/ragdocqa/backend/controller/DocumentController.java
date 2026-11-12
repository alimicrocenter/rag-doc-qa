package com.ragdocqa.backend.controller;
import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ragdocqa.backend.dto.DocumentResponse;
import com.ragdocqa.backend.repository.DocumentRepository;
import com.ragdocqa.backend.service.DocumentService;




@RestController
@RequestMapping("api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;

    private final DocumentService documentService;

    public DocumentController(DocumentRepository documentRepository, DocumentService documentService) {
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestParam("file") MultipartFile file) throws IOException {  
        return DocumentResponse.from(documentService.upload(file));
    }

    @GetMapping
    public List<DocumentResponse> list(){  
        return documentRepository.findAllByOrderByUploadedAtDescIdDesc()
        .stream()
        .map(DocumentResponse::from)
        .toList();
    }
    
}
