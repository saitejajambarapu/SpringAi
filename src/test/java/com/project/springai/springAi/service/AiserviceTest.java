package com.project.springai.springAi.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class AiserviceTest {

    @Autowired
    private Aiservice aiservice;

    @Test
    public void testGetJoke(){
        String joke=aiservice.askAI("What is Machine Learning?");
        System.out.println(joke);
    }

    @Test
    public void testEmbededText(){
        var embed=aiservice.getEmbedding("Dogs");
        System.out.println(embed.length);
        for(float e : embed) System.out.println(e+" ");
    }
    @Test
    public void testStoreData(){
       aiservice.ingestDataToVectorStore();
    }
    @Test
    public void testSimilaritySearch(){
        List<Document> doc = aiservice.similarityCheck("A team of people travel through a wormhole");
        for(Document d : doc) System.out.println(d);
    }


}
