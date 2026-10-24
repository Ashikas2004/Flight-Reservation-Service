package com.flightreservation.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @Test
    @DisplayName("notFound - creates ApiException with NOT_FOUND status")
    void notFound_SetsCorrectStatus() {
        ApiException ex = ApiException.notFound("Resource not found");
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Resource not found", ex.getMessage());
    }

    @Test
    @DisplayName("badRequest - creates ApiException with BAD_REQUEST status")
    void badRequest_SetsCorrectStatus() {
        ApiException ex = ApiException.badRequest("Invalid input");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Invalid input", ex.getMessage());
    }

    @Test
    @DisplayName("conflict - creates ApiException with CONFLICT status")
    void conflict_SetsCorrectStatus() {
        ApiException ex = ApiException.conflict("Already exists");
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Already exists", ex.getMessage());
    }

    @Test
    @DisplayName("forbidden - creates ApiException with FORBIDDEN status")
    void forbidden_SetsCorrectStatus() {
        ApiException ex = ApiException.forbidden("Access denied");
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    @DisplayName("internalError - creates ApiException with INTERNAL_SERVER_ERROR status")
    void internalError_SetsCorrectStatus() {
        ApiException ex = ApiException.internalError("Server error");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals("Server error", ex.getMessage());
    }
}

