package com.project.springai.springAi.service;

import com.project.springai.springAi.advisor.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RagService {
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;

    @Value("classpath:sample-1000pages.pdf")
    private String pdfLocation;

    @Autowired
    private ChatMemory  chatMemory;

    public String askAIWithAdvisor(String prompt, String userId){

        return chatClient.prompt()
                .system("""
        You are an AI assistant helping a developer.
        Greet User using your name aang and the username if you know their name.
        Answer in a friendly, conversational tone.
        """)
                .user(prompt)
                .advisors(

//                        new SafeGuardAdvisor(List.of("politics","religion")),

                        MessageChatMemoryAdvisor.builder(chatMemory)
                                        .conversationId(userId)
                                                .build(),

                       VectorStoreChatMemoryAdvisor.builder(vectorStore)
                               .conversationId(userId)
                               .defaultTopK(4)
                               .build(),

                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .filterExpression("file_name == 'ai1.pdf'")
                                        .build())
                                .build(),

                        new TokenUsageAdvisor()
                )
                .call()
                .content();
    }

    public String askAI(String prompt) {

        // System prompt template
        String template = """
        You are an AI assistant helping a developer.

        Rules:
        - Use ONLY the information provided in the context
        - You MAY rephrase, summarize, and explain in natural language
        - Do NOT introduce new concepts or facts
        - If multiple context sections are relevant, combine them into a single explanation
        - If the answer is not present, say "I don't know"

        Context:
        {context}

        Answer in a friendly, conversational tone.
        """;

        // Retrieve relevant documents from vector store
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(prompt)
                        .topK(5)
                        .similarityThreshold(0.5)
                        .filterExpression(
                                        "file_name == 'ai1.pdf'"

                        )
                        .build()
        );

        // Build context from retrieved documents
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // Render final prompt
        PromptTemplate promptTemplate = new PromptTemplate(template);
        String systemPrompt = promptTemplate.render(
                Map.of("context", context)
        );

        // Call LLM
        return chatClient
                .prompt(systemPrompt)
                .user(prompt)
                .advisors(
                        new SimpleLoggerAdvisor()
                )
                .call()
                .content();
    }

//    public void ingestion(){
//        long startTotal = System.nanoTime();
//
//// 1. PDF → TEXT
//        long startPdf = System.nanoTime();
//
//        PagePdfDocumentReader pdf = new PagePdfDocumentReader(pdfLocation);
//        List<Document> pages = pdf.get();
//
//        long endPdf = System.nanoTime();
//        System.out.println("PDF to text time: " + (endPdf - startPdf) / 1_000_000 + " ms");
//
//
//// 2. CHUNKING
//        long startChunk = System.nanoTime();
//
//        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
//                .withChunkSize(200)
//                .build();
//
//        List<Document> chunks = tokenTextSplitter.apply(pages);
//
//        long endChunk = System.nanoTime();
//        System.out.println("Chunking time: " + (endChunk - startChunk) / 1_000_000 + " ms");
//
//
//// 3. VECTOR DB INSERT
//        long startDb = System.nanoTime();
//
//        vectorStore.add(chunks);
//
//        long endDb = System.nanoTime();
//        System.out.println("Vector DB insert time: " + (endDb - startDb) / 1_000_000 + " ms");
//
//
//        long endTotal = System.nanoTime();
//        System.out.println("Total pipeline time: " + (endTotal - startTotal) / 1_000_000 + " ms");
//    }

//    public void ingestion() {
//        long startTotal = System.nanoTime();
//
//// 1. PDF → TEXT
//        long startPdf = System.nanoTime();
//
//        PagePdfDocumentReader pdf = new PagePdfDocumentReader(pdfLocation);
//        List<Document> pages = pdf.get();
//
//        long endPdf = System.nanoTime();
//        System.out.println("PDF to text time: " + (endPdf - startPdf) / 1_000_000 + " ms");
//
//
//// 2. SEMANTIC CHUNKING (PARALLEL - INLINE)
//        long startChunk = System.nanoTime();
//
//        List<Document> chunks = pages.parallelStream()
//                .flatMap(page -> {
//
//                    // FIX 1: Null/blank page guard
//                    String text = page.getText();
//                    if (text == null || text.isBlank()) return Stream.empty();
//
//                    List<String> sentences = Arrays.stream(text.split("(?<=[.!?])\\s+"))
//                            .map(String::trim)
//                            .filter(s -> !s.isEmpty())
//                            .toList();
//
//                    List<Document> result = new ArrayList<>();
//                    StringBuilder currentChunk = new StringBuilder();
//                    String previousSentence = null;
//
//                    // FIX 2: Lowered max chunk size for better granularity
//                    int maxChunkSize = 800;
//
//                    for (String sentence : sentences) {
//
//                        if (previousSentence == null) {
//                            currentChunk.append(sentence);
//                        } else {
//
//                            Set<String> words1 = new HashSet<>(Arrays.asList(previousSentence.toLowerCase().split("\\W+")));
//                            Set<String> words2 = new HashSet<>(Arrays.asList(sentence.toLowerCase().split("\\W+")));
//
//                            // FIX 3: Remove empty strings from word sets (from split artifacts)
//                            words1.remove("");
//                            words2.remove("");
//
//                            double similarity = 0.0;
//                            if (!words1.isEmpty() && !words2.isEmpty()) {
//                                Set<String> intersection = new HashSet<>(words1);
//                                intersection.retainAll(words2);
//
//                                Set<String> union = new HashSet<>(words1);
//                                union.addAll(words2);
//
//                                similarity = (double) intersection.size() / union.size();
//                            }
//
//                            // FIX 4: Lowered similarity threshold from 0.5 → 0.15
//                            // Jaccard similarity of 0.5 is too strict; most related sentences score 0.1–0.2
//                            if (similarity > 0.15 && currentChunk.length() < maxChunkSize) {
//                                currentChunk.append(" ").append(sentence);
//                            } else {
//                                // FIX 5: Defensive metadata copy to avoid shared mutable map references
//                                result.add(new Document(currentChunk.toString(), new HashMap<>(page.getMetadata())));
//                                currentChunk = new StringBuilder(sentence);
//                            }
//                        }
//
//                        previousSentence = sentence;
//                    }
//
//                    // Last chunk
//                    if (!currentChunk.isEmpty()) {
//                        result.add(new Document(currentChunk.toString(), new HashMap<>(page.getMetadata())));
//                    }
//
//                    return result.stream();
//                })
//                .toList();
//
//        long endChunk = System.nanoTime();
//        System.out.println("Semantic Chunking time: " + (endChunk - startChunk) / 1_000_000 + " ms");
//
//
//// 3. VECTOR DB INSERT
//        long startDb = System.nanoTime();
//
//        vectorStore.add(chunks);
//
//        long endDb = System.nanoTime();
//        System.out.println("Vector DB insert time: " + (endDb - startDb) / 1_000_000 + " ms");
//
//
//        long endTotal = System.nanoTime();
//        System.out.println("Total pipeline time: " + (endTotal - startTotal) / 1_000_000 + " ms");
//    }

public void ingestion(){
    long startTotal = System.nanoTime();

// 1. PDF → TEXT
    long startPdf = System.nanoTime();
    PagePdfDocumentReader pdf = new PagePdfDocumentReader(pdfLocation);
    List<Document> pages = pdf.get();
    long endPdf = System.nanoTime();
    System.out.println("PDF to text time: " + (endPdf - startPdf) / 1_000_000 + " ms");


// 2. SEMANTIC CHUNKING (parallel per page)
    long startChunk = System.nanoTime();

    List<Document> chunks = pages.parallelStream()
            .flatMap(page -> chunkPage(page).stream())
            .toList();

    long endChunk = System.nanoTime();
    System.out.println("Semantic Chunking time: " + (endChunk - startChunk) / 1_000_000 + " ms");
    System.out.println("Total chunks: " + chunks.size());


// 3. PARALLEL EMBEDDING GENERATION
    long startEmbed = System.nanoTime();

    int EMBED_THREADS = 16;
    ExecutorService embedExecutor = Executors.newFixedThreadPool(EMBED_THREADS);

    List<Future<Document>> embedFutures = new ArrayList<>();

    for (Document chunk : chunks) {
        embedFutures.add(embedExecutor.submit(() -> {
            float[] embedding = embeddingModel.embed(chunk.getText());

            return new Document(
                    chunk.getId(),
                    chunk.getText(),
                    chunk.getMetadata()
            );
        }));
    }

// Collect embedded documents
    List<Document> embeddedChunks = new ArrayList<>();
    for (Future<Document> f : embedFutures) {
        try {
            embeddedChunks.add(f.get());
        } catch (Exception e) {
            System.err.println("Embedding failed: " + e.getMessage());
        }
    }

    embedExecutor.shutdown();

    long endEmbed = System.nanoTime();
    System.out.println("Embedding generation time: " + (endEmbed - startEmbed) / 1_000_000 + " ms");


// 4. BATCHED DB INSERT
    long startDb = System.nanoTime();

    int BATCH_SIZE = 200;
    for (int i = 0; i < embeddedChunks.size(); i += BATCH_SIZE) {
        List<Document> batch = embeddedChunks.subList(i, Math.min(i + BATCH_SIZE, embeddedChunks.size()));
        vectorStore.add(batch);
    }

    long endDb = System.nanoTime();
    System.out.println("Vector DB insert time: " + (endDb - startDb) / 1_000_000 + " ms");


    long endTotal = System.nanoTime();
    System.out.println("Total pipeline time: " + (endTotal - startTotal) / 1_000_000 + " ms");
}

// ─── chunkPage() METHOD ───────────────────────────────────────────────────────

private List<Document> chunkPage(Document page) {

    String text = page.getText();
    if (text == null || text.isBlank()) return Collections.emptyList();

    List<String> sentences = Arrays.stream(text.split("(?<=[.!?])\\s+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

    List<Document> result = new ArrayList<>();
    StringBuilder currentChunk = new StringBuilder();
    String previousSentence = null;
    int maxChunkSize = 800;

    for (String sentence : sentences) {

        if (previousSentence == null) {
            currentChunk.append(sentence);

        } else {
            Set<String> words1 = new HashSet<>(Arrays.asList(previousSentence.toLowerCase().split("\\W+")));
            Set<String> words2 = new HashSet<>(Arrays.asList(sentence.toLowerCase().split("\\W+")));

            words1.remove("");
            words2.remove("");

            double similarity = 0.0;
            if (!words1.isEmpty() && !words2.isEmpty()) {
                Set<String> intersection = new HashSet<>(words1);
                intersection.retainAll(words2);

                Set<String> union = new HashSet<>(words1);
                union.addAll(words2);

                similarity = (double) intersection.size() / union.size();
            }

            if (similarity > 0.15 && currentChunk.length() < maxChunkSize) {
                currentChunk.append(" ").append(sentence);
            } else {
                result.add(new Document(currentChunk.toString(), new HashMap<>(page.getMetadata())));
                currentChunk = new StringBuilder(sentence);
            }
        }

        previousSentence = sentence;
    }

    // Last chunk
    if (!currentChunk.isEmpty()) {
        result.add(new Document(currentChunk.toString(), new HashMap<>(page.getMetadata())));
    }

    return result;
}
}
