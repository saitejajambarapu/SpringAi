package com.project.springai.springAi.dto;

public record Joke(
        String text,
        String category,
        String laughScore,
        Boolean isNSFW
) {

}
