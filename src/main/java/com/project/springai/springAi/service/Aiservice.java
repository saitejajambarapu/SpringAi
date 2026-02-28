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

@Service
public class Aiservice {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;



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

        vectorStore.add(movies);
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
