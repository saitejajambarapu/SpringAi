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

    @Test
    public void askAiWithAdvisor(){
//        String joke=ragService.askAIWithAdvisor("What is Machine Learning? and my name is sai", "sai2714");
        String joke=ragService.askAIWithAdvisor("What is Spring AI ?", "shiva123");
        System.out.println(joke);
    }


}
