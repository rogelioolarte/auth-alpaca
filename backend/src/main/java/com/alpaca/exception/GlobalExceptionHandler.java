package com.alpaca.exception;

import com.alpaca.dto.response.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(value = {SpecificException.class})
  public ResponseEntity<String> handleSpecificException(SpecificException specificException) {
    return ResponseEntity.status(specificException.getStatusCode())
        .body(specificException.getReason());
  }

  @ExceptionHandler(value = {MethodArgumentNotValidException.class})
  public ResponseEntity<HashMap<String, String>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    HashMap<String, String> errors = new HashMap<>();
    for (FieldError fieldError : exception.getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleGlobalException(
      Exception exception, WebRequest webRequest) {
    return new ResponseEntity<>(
        new ErrorResponseDTO(
            webRequest.getDescription(false), exception.getMessage(), LocalDateTime.now()),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponseDTO> handleResponseStatusException(
      ResponseStatusException exception, WebRequest webRequest) {
    return new ResponseEntity<>(
        new ErrorResponseDTO(
            webRequest.getDescription(false), exception.getReason(), LocalDateTime.now()),
        HttpStatus.valueOf(exception.getStatusCode().value()));
  }
}
