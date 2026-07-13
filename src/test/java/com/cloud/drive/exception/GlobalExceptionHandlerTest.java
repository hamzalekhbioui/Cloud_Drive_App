package com.cloud.drive.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGeneric_returnsGenericMessage() {
        var response = handler.handleGeneric(new RuntimeException("SQL syntax error near 'DROP TABLE'"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
    }

    @Test
    void handleApi_preservesIntentionalMessage() {
        var response = handler.handleApi(new ApiException("File not found", HttpStatus.NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "File not found");
    }
}
