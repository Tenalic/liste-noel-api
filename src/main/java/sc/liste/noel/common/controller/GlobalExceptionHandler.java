package sc.liste.noel.common.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sc.liste.noel.common.dto.response.GenericResponse;
import sc.liste.noel.common.service.MessageService;

import java.util.Locale;

import static sc.liste.noel.common.constant.Constants.API_ERROR_GENERIC_KEY;
import static sc.liste.noel.common.constant.Constants.API_RETURN_KO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger(GlobalExceptionHandler.class);

    private final MessageService messageService;

    public GlobalExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleUnexpected(
            Exception e,
            Locale locale) {
        LOGGER.error("Unexpected error : {}",e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(new GenericResponse(
                        messageService.getMessage(API_ERROR_GENERIC_KEY, locale),
                        API_RETURN_KO));
    }
}
