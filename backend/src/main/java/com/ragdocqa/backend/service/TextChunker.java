package com.ragdocqa.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {

    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .build();

    public List<Document> chunk(Long documentId, String filename, List<String> pages) {
        List<Document> chunks = new ArrayList<>();

        for (int i = 0; i < pages.size(); i++) {
            String text = pages.get(i);
            if (text.isBlank()) {
                continue;
            }

            Document page = new Document(text, Map.of(
                    "documentId", documentId,
                    "filename", filename,
                    "page", i + 1));

            chunks.addAll(splitter.split(page));
        }

        return chunks;
    }
}
