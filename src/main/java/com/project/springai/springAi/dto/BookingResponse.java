package com.project.springai.springAi.dto;


import com.project.springai.springAi.entity.BookingStatus;

import java.time.Instant;

public record BookingResponse(Long id, String destination, Instant departureTime, BookingStatus status) {}