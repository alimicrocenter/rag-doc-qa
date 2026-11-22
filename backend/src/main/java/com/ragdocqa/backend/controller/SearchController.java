package com.ragdocqa.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ragdocqa.backend.dto.SearchResult;
import com.ragdocqa.backend.service.SearchService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController 
@RequestMapping("api/search")
public class SearchController {
    
    private final SearchService searchService;

    public SearchController(SearchService searchService){  
        this.searchService = searchService;
    }

    @GetMapping
    public List<SearchResult> search(  
        @RequestParam @NotBlank String q,
        @RequestParam(defaultValue="4") @Min(1) @Max(20) int topK){  
            return searchService.search(q, topK);
        }
}
