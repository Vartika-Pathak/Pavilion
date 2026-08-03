package com.pavilion.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Regression coverage for two bugs the Spring Security refactor surfaced: the catch-all
// Exception handler was swallowing AuthorizationDeniedException (from @PreAuthorize denials)
// and NoResourceFoundException (unmapped routes), turning both into a generic 500 instead of
// the 403/404 they should be. See SecurityConfig's accessDeniedHandler for the equivalent
// behavior on the filter-chain side of a denial (this class only covers denials raised from
// inside a controller method, which never reach that handler).
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionMapsToItsOwnStatusAndMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleApiException(new ApiException(HttpStatus.CONFLICT, "already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "already exists");
    }

    @Test
    void authorizationDeniedMapsToForbiddenWithGenericMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleAuthorizationDenied(new AuthorizationDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "You don't have permission to do that");
    }

    @Test
    void noResourceFoundMapsToNotFound() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNoResourceFound(new NoResourceFoundException(HttpMethod.GET, "/nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Not found");
    }

    @Test
    void validationFailureJoinsFieldErrorsIntoOneMessage() {
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(e.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new org.springframework.validation.FieldError("body", "email", "must not be blank")));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "email: must not be blank");
    }

    @Test
    void unexpectedExceptionMapsToGenericFiveHundredWithoutLeakingDetails() {
        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Something went wrong");
    }
}
