package com.ragdocqa.backend.service;

import java.util.List;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.ragdocqa.backend.dto.SearchResult;

@Service 
public class SearchService {
    
    private final VectorStore vectorStore;

    public SearchService(VectorStore vectorStore){  
        this.vectorStore = vectorStore;
    }

    public List<SearchResult> search(String query, int topK){  
        SearchRequest request = SearchRequest.builder().query(query).topK(topK).similarityThresholdAll().build();

        return vectorStore.similaritySearch(request).stream().map(SearchResult::from).toList();
    }
}
