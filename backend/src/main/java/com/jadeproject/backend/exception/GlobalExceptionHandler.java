package com.jadeproject.backend.exception;

import com.jadeproject.backend.dto.StandardErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    //1. Erros de validação dos DTOs (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardErrorDTO> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        StandardErrorDTO errorResponse = StandardErrorDTO.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Erro na validação dos campos.")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    //2. Conflito de dados (ex: email já cadastrado)
    @ExceptionHandler(DataConflictException.class)
    public ResponseEntity<StandardErrorDTO> handleDataConflict(DataConflictException ex, HttpServletRequest request) {
        StandardErrorDTO errorResponse = StandardErrorDTO.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    //3. JSON malformado
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<StandardErrorDTO> handleJsonErrors(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        StandardErrorDTO errorResponse = StandardErrorDTO.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed JSON Request")
                .message("O corpo da requisição está inválido ou contém valores incorretos.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    //4. Captura os ResponseStatusException (erros 404 e 403 lançados nos controllers)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<StandardErrorDTO> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        StandardErrorDTO errorResponse = StandardErrorDTO.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(ex.getStatusCode().value())
                .error(ex.getStatusCode().toString())
                .message(ex.getReason())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    //5. Catch-all (para erros inesperados 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardErrorDTO> handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        //Podemos colocar um log.error("Erro inesperado", ex) para registrar no terminal
        StandardErrorDTO errorResponse = StandardErrorDTO.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Ocorreu um erro interno no servidor.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
