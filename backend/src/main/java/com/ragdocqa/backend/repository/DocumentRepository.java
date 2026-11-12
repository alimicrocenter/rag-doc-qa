package com.ragdocqa.backend.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ragdocqa.backend.model.Document;



public interface DocumentRepository extends JpaRepository<Document, Long>{

    //List<Document> findAllByOrderByUploadedAtDesc();
    List<Document> findAllByOrderByUploadedAtDescIdDesc();

    
}
