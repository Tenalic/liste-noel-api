package sc.liste.noel.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sc.liste.noel.account.dto.response.AccountResponse;

import static sc.liste.noel.common.constant.Constants.API_RETURN_KO;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AccountResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Erreur de validation");

        return ResponseEntity.badRequest()
                .body(new AccountResponse(null, message, API_RETURN_KO));
    }

}
