package com.project.springai.springAi.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RagServiceTest {

    @Autowired
    private RagService ragService;

    @Test
    public void ingest(){
        ragService.ingestion();
    }

    @Test
    public void askAi(){
        String joke=ragService.askAI("What is Machine Learning?");
        System.out.println(joke);
    }
}
