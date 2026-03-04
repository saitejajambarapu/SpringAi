package com.project.springai.springAi.dto;

import java.util.List;

public record BookingsListResponse(List<BookingResponse> bookings, String message) {}
