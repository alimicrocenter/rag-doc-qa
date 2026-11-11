package com.ragdocqa.backend.model;
import java.time.Instant;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name= "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    private Integer pageCount;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void onCreate() {  
        if(uploadedAt == null){  
            uploadedAt = Instant.now();
        }
    } 
    
    
}
