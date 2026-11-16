package com.washmycar.exception;

import com.washmycar.dto.Dtos.MessageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<MessageResponse> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<MessageResponse> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return ResponseEntity.badRequest().body(new MessageResponse(msg));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    ResponseEntity<MessageResponse> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(new MessageResponse("Requête invalide"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<MessageResponse> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse("E-mail déjà utilisé"));
    }
}
