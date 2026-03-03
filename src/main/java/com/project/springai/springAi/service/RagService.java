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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;

    @Value("classpath:ai1.pdf")
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

    public void ingestion(){
        PagePdfDocumentReader pdf = new PagePdfDocumentReader(pdfLocation);
        List<Document> pages = pdf.get();

        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(200)
                .build();
        List<Document> chunks = tokenTextSplitter.apply(pages);
        vectorStore.add(chunks);
    }
}
