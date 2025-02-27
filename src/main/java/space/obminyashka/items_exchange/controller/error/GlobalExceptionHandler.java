package space.obminyashka.items_exchange.controller.error;

import io.jsonwebtoken.JwtException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import space.obminyashka.items_exchange.exception.*;

import javax.persistence.EntityNotFoundException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Log4j2
@AllArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler({UsernameNotFoundException.class, EntityNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessage handleNotFoundExceptions(Exception e, ServletWebRequest request) {
        return logAndGetErrorMessage(request, e, Level.WARN);
    }

    @ExceptionHandler(DataConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorMessage handleDataConflictException(DataConflictException e, ServletWebRequest request) {
       return logAndGetErrorMessage(request, e, Level.INFO);
    }

    @ExceptionHandler({
            InvalidDtoException.class,
            InvalidLocationInitFileCreatingDataException.class,
            IllegalIdentifierException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleBadRequestExceptions(Exception e, ServletWebRequest request) {
        return logAndGetErrorMessage(request, e, Level.WARN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleValidationExceptions(MethodArgumentNotValidException e, ServletWebRequest request) {
        final var validationErrors = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("\n", "Validation error(s): ", ""));

        final var errorMessage = new ErrorMessage(validationErrors, request.getRequest().getRequestURI(), request.getHttpMethod());
        log.log(Level.WARN, errorMessage);
        return errorMessage;
    }

    @ExceptionHandler({IllegalOperationException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorMessage handleIllegalOperation(Exception e, ServletWebRequest request) {
        return logAndGetErrorMessage(request, e, Level.WARN);
    }

    @ExceptionHandler({JwtException.class, RefreshTokenException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorMessage handleUnauthorizedExceptions(Exception e, ServletWebRequest request) {
        return logAndGetErrorMessage(request, e, Level.ERROR);
    }

    @ExceptionHandler({RuntimeException.class, ServletException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleRuntimeExceptions(Exception e, ServletWebRequest request) {
        return logAndGetErrorMessage(request, e, Level.ERROR);
    }

    @ExceptionHandler({IOException.class, ElementsNumberExceedException.class})
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ErrorMessage handleIOException(Exception ex, ServletWebRequest request){
        return logAndGetErrorMessage(request, ex, Level.ERROR);
    }

    @ExceptionHandler(UndeclaredThrowableException.class)
    public ResponseEntity<ErrorMessage> handleSneakyThrownException(UndeclaredThrowableException ex, ServletWebRequest request){
        final var cause = ex.getCause();
        if (cause instanceof UnsupportedMediaTypeException e) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(logAndGetErrorMessage(request, e, Level.WARN));
        } else if (cause instanceof IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(logAndGetErrorMessage(request, e, Level.WARN));
        }
        return ResponseEntity.internalServerError().body(logAndGetErrorMessage(request, ex, Level.ERROR));
    }

    private ErrorMessage logAndGetErrorMessage(ServletWebRequest request, Exception e, Level level) {
        if (e instanceof UndeclaredThrowableException) {
            e = (Exception) ((UndeclaredThrowableException) e).getUndeclaredThrowable();
        }
        var errorMessage = new ErrorMessage(e.getLocalizedMessage(), request.getRequest().getRequestURI(), request.getHttpMethod());
        log.log(level, errorMessage);
        return errorMessage;
    }
}
