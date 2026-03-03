package com.project.springai.springAi.service;

import com.project.springai.springAi.dto.Joke;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class Aiservice {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;



    public String askAI(String prompt){

        return  chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }


    public void ingestDataToVectorStore(){
        List<Document> movies = List.of(

                new Document(
                        "A thief who steals corporate secrets through dream-sharing technology.",
                        Map.of(
                                "title", "Inception",
                                "genre", "Sci-Fi",
                                "year", "2010"
                        )
                ),

                new Document(
                        "A team of explorers travel through a wormhole in space to ensure humanity's survival.",
                        Map.of(
                                "title", "Interstellar",
                                "genre", "Sci-Fi",
                                "year", "2014"
                        )
                ),

                new Document(
                        "A poor yet passionate young man falls in love with a rich young woman.",
                        Map.of(
                                "title", "The Notebook",
                                "genre", "Romance",
                                "year", "2004"
                        )
                )
        );
        List<Document> aiFaqs = List.of(

                new Document(
                        "Artificial Intelligence (AI) is the simulation of human intelligence in machines that are programmed to think, learn, and make decisions.",
                        Map.of(
                                "title", "What is Artificial Intelligence?",
                                "category", "AI Basics",
                                "difficulty", "Beginner",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Machine Learning is a subset of AI that enables systems to learn from data and improve performance without being explicitly programmed.",
                        Map.of(
                                "title", "What is Machine Learning?",
                                "category", "Machine Learning",
                                "difficulty", "Beginner",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Deep Learning is a subset of Machine Learning that uses neural networks with multiple layers to model complex patterns in data.",
                        Map.of(
                                "title", "What is Deep Learning?",
                                "category", "Deep Learning",
                                "difficulty", "Intermediate",
                                "year", "2024"
                        )
                ),

                new Document(
                        "A Large Language Model (LLM) is a deep learning model trained on vast amounts of text data to understand and generate human-like language.",
                        Map.of(
                                "title", "What is a Large Language Model?",
                                "category", "LLM",
                                "difficulty", "Intermediate",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Retrieval-Augmented Generation (RAG) is an architecture that combines information retrieval with text generation to provide more accurate and context-aware responses.",
                        Map.of(
                                "title", "What is Retrieval-Augmented Generation?",
                                "category", "RAG",
                                "difficulty", "Intermediate",
                                "year", "2024"
                        )
                ),

                new Document(
                        "An embedding is a numerical representation of text, image, or other data in vector form so that similar items are closer together in vector space.",
                        Map.of(
                                "title", "What is an Embedding?",
                                "category", "Vector Search",
                                "difficulty", "Intermediate",
                                "year", "2024"
                        )
                ),

                new Document(
                        "A vector database is a specialized database designed to store and query vector embeddings efficiently using similarity search algorithms.",
                        Map.of(
                                "title", "What is a Vector Database?",
                                "category", "Vector Search",
                                "difficulty", "Intermediate",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Prompt engineering is the practice of designing effective prompts to guide AI models toward generating accurate and relevant outputs.",
                        Map.of(
                                "title", "What is Prompt Engineering?",
                                "category", "LLM",
                                "difficulty", "Beginner",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Fine-tuning is the process of training a pre-trained model on a smaller, domain-specific dataset to improve its performance on specialized tasks.",
                        Map.of(
                                "title", "What is Fine-Tuning?",
                                "category", "Model Training",
                                "difficulty", "Advanced",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Hallucination in AI refers to when a language model generates information that appears correct but is factually incorrect or fabricated.",
                        Map.of(
                                "title", "What is AI Hallucination?",
                                "category", "LLM",
                                "difficulty", "Intermediate",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Transfer learning is a technique where a model trained on one task is reused as the starting point for a related task.",
                        Map.of(
                                "title", "What is Transfer Learning?",
                                "category", "Machine Learning",
                                "difficulty", "Intermediate",
                                "year", "2024"
                        )
                ),

                new Document(
                        "Natural Language Processing (NLP) is a field of AI focused on enabling machines to understand, interpret, and generate human language.",
                        Map.of(
                                "title", "What is Natural Language Processing?",
                                "category", "NLP",
                                "difficulty", "Beginner",
                                "year", "2024"
                        )
                ),

                new Document(
                        "A transformer model is a neural network architecture that uses self-attention mechanisms to process sequential data efficiently.",
                        Map.of(
                                "title", "What is a Transformer Model?",
                                "category", "Deep Learning",
                                "difficulty", "Advanced",
                                "year", "2024"
                        )
                )

        );

        vectorStore.add(aiFaqs);

//        vectorStore.add(movies);
    }


    public List<Document> similarityCheck(String text){
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(text)
                        .topK(3)
                        .similarityThreshold(0.5)
                .build());
    }

    public float[] getEmbedding(String text){
        return embeddingModel.embed(text);
    }


    public String getjoke(String topic){

        String systemPrompt = """
                you are a sarcaastic joker, you make poetic jokes in 4 lines.
                you don't make jokes about politics.
                Give a joke on the topic: {topic}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(systemPrompt);
        String renderText = promptTemplate.render(Map.of("topic",topic));

//       var response = chatClient.prompt()
//                .user(renderText)
//               .advisors(
//                       new SimpleLoggerAdvisor()
//               )
//                .call().chatClientResponse();
        var response = chatClient.prompt()
                .user(renderText)
                .advisors( //-------------------------------------------------------
                        new SimpleLoggerAdvisor() // form logging
                )//---------------------------------------------------------------
                .call().
                entity(Joke.class);
//       return response.chatResponse().getResult().getOutput().getText();

        return response.text();
    }

}
