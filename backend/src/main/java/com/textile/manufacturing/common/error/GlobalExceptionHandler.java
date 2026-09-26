package com.textile.manufacturing.common.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.textile.manufacturing.catalog.bom.service.BomMaterialUnavailableException;
import com.textile.manufacturing.catalog.bom.service.BomNotFoundException;
import com.textile.manufacturing.catalog.material.service.DuplicateMaterialCodeException;
import com.textile.manufacturing.catalog.material.service.MaterialNotFoundException;
import com.textile.manufacturing.catalog.material.service.MaterialNotActiveException;
import com.textile.manufacturing.catalog.product.service.DuplicateProductCodeException;
import com.textile.manufacturing.catalog.product.service.ProductNotFoundException;
import com.textile.manufacturing.identity.service.DuplicateEmailException;
import com.textile.manufacturing.identity.service.InvalidCredentialsException;
import com.textile.manufacturing.identity.service.UnknownRoleException;
import com.textile.manufacturing.identity.service.UserNotFoundException;
import com.textile.manufacturing.inventory.service.InventoryItemNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private record FieldError(String field, String message) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setTitle("Validation failed");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "Invalid state", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request body is missing or malformed.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter",
            "Parameter '%s' has an invalid value.".formatted(exception.getName()));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleInvalidSortProperty(PropertyReferenceException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid sort",
            "Cannot sort by '%s'.".formatted(exception.getPropertyName()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage());
    }

    @ExceptionHandler({ProductNotFoundException.class, MaterialNotFoundException.class,
        BomNotFoundException.class, InventoryItemNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
    }

    @ExceptionHandler(BomMaterialUnavailableException.class)
    ProblemDetail handleBomMaterialUnavailable(BomMaterialUnavailableException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid reference", exception.getMessage());
    }

    @ExceptionHandler(MaterialNotActiveException.class)
    ProblemDetail handleMaterialNotActive(MaterialNotActiveException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid reference", exception.getMessage());
    }

    @ExceptionHandler({DuplicateProductCodeException.class, DuplicateMaterialCodeException.class})
    ProblemDetail handleDuplicateCatalogCode(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, "Duplicate resource", exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail handleDuplicateEmail(DuplicateEmailException exception) {
        return problem(HttpStatus.CONFLICT, "Duplicate resource", exception.getMessage());
    }

    @ExceptionHandler(UnknownRoleException.class)
    ProblemDetail handleUnknownRole(UnknownRoleException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid reference", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return problem(HttpStatus.CONFLICT, "Duplicate resource",
            "The resource already exists or violates a constraint.");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void rethrowSecurityException(RuntimeException exception) {
        throw exception;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
            "An unexpected error occurred.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
