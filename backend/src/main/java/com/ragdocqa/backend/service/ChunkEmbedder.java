package com.ragdocqa.backend.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class ChunkEmbedder {

    private final EmbeddingModel embeddingModel;

    public ChunkEmbedder(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /** Returns one vector per chunk, in the same order as the chunks. */
    public List<float[]> embed(List<Document> chunks) {
        List<String> texts = chunks.stream().map(Document::getText).toList();

        try {
            return embeddingModel.embed(texts);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Could not create embeddings. Check that Ollama is running and the bge-m3 model is pulled.", e);
        }
    }
}
