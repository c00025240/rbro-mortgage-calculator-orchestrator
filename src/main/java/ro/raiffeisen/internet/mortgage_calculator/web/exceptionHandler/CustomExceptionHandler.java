package ro.raiffeisen.internet.mortgage_calculator.web.exceptionHandler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.raiffeisen.internet.mortgage_calculator.exception.BadRequestException;
import ro.raiffeisen.internet.mortgage_calculator.exception.InternalServerException;
import ro.raiffeisen.internet.mortgage_calculator.exception.NotFoundException;
import ro.raiffeisen.internet.mortgage_calculator.exception.UnprocessableEntityException;
import ro.raiffeisen.internet.mortgage_calculator.model.Error;
import ro.raiffeisen.internet.mortgage_calculator.model.ErrorReason;
import ro.raiffeisen.internet.mortgage_calculator.model.StandardHttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ro.raiffeisen.internet.mortgage_calculator.helper.MarkerFields.*;
import static ro.raiffeisen.internet.mortgage_calculator.model.StandardHttpHeaders.*;


@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    private static final HttpHeaders httpHeaders;
    public static final String COMMON_INVALID_PARAMETER = "COMMON_INVALID_PARAMETER";
    public static final String COMMON_INTERNAL_ERROR = "COMMON_INTERNAL_ERROR";
    public static final String COMMON_NOT_FOUND = "COMMON_NOT_FOUND";

    static {
        httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @ExceptionHandler({InternalServerException.class})
    protected ResponseEntity<Error> handleUnhandledExceptions(InternalServerException ex) {
        log.error(ex.getMessage(), ex);
        List<ErrorReason> errorReasons =
                getErrorReasons(ex.getErrorReasons(), ex.getMessage(), COMMON_INTERNAL_ERROR);

        return new ResponseEntity<>(
                buildErrorFromRequestParamters(errorReasons, HttpStatus.INTERNAL_SERVER_ERROR),
                httpHeaders,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({UnprocessableEntityException.class})
    public ResponseEntity<Error> handleUnprocessableEntityException(UnprocessableEntityException ex) {
        log.error(ex.getMessage(), ex);
        List<ErrorReason> errorReasons =
                getErrorReasons(ex.getErrorReasons(), ex.getMessage(), COMMON_INVALID_PARAMETER);

        Error error = Error.builder()
                .errorId(UUID.randomUUID().toString())
                .correlationId(MDC.get(CONTEXT_CORRELATION_ID))
                .requestId(MDC.get(CONTEXT_REQUEST_ID))
                .reasons(errorReasons)
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .displayedValue(ex.getDisplayedValue())
                .build();

        return new ResponseEntity<>(
                error,
                httpHeaders,
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private List<ErrorReason> getErrorReasons(
            List<ErrorReason> errorReasons, String ex, String code) {
        if (errorReasons == null || errorReasons.isEmpty()) {
            errorReasons = new ArrayList<>();
            errorReasons.add(
                    ErrorReason.builder().code(code).message(ex).severity(ErrorReason.SeverityType.ERROR).build());
        }
        return errorReasons;
    }

    @ExceptionHandler
    protected ResponseEntity<Error> handleAllExceptions(Exception ex) {
        log.error(ex.getMessage(), ex);

        List<ErrorReason> errorReasons = new ArrayList<>();
        errorReasons.add(
                ErrorReason.builder()
                        .code(COMMON_INTERNAL_ERROR)
                        .message(ex.getMessage())
                        .severity(ErrorReason.SeverityType.ERROR)
                        .build());

        return new ResponseEntity<>(
                buildErrorFromRequestParamters(errorReasons, HttpStatus.INTERNAL_SERVER_ERROR),
                httpHeaders,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    public ResponseEntity<Error> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        // The logging interceptor doesn't apply here and that's why logging is setup here
        setupLoggingParametersFromWebRequest(request);
        log.warn(ex.getMessage(), ex);

        List<ErrorReason> errorReasons = new ArrayList<>();
        errorReasons.add(
                ErrorReason.builder()
                        .code(COMMON_NOT_FOUND)
                        .message(ex.getMessage())
                        .severity(ErrorReason.SeverityType.WARNING)
                        .build());

        Error error = buildErrorFromRequestParamters(errorReasons, HttpStatus.NOT_FOUND);
        String correlationId = MDC.get(CONTEXT_CORRELATION_ID);
        MDC.clear();

        return new ResponseEntity<>(
                error, StandardHttpHeaders.getStandardResponseHeaders(correlationId), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<Error> handleNotFoundException(NotFoundException ex) {
        log.warn(ex.getMessage(), ex);

        List<ErrorReason> errorReasons = new ArrayList<>();
        errorReasons.add(
                ErrorReason.builder()
                        .code(COMMON_NOT_FOUND)
                        .message(ex.getMessage())
                        .severity(ErrorReason.SeverityType.ERROR)
                        .build());

        return new ResponseEntity<>(
                buildErrorFromRequestParamters(errorReasons, HttpStatus.NOT_FOUND),
                httpHeaders,
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            HttpMediaTypeNotSupportedException.class,
            ServletRequestBindingException.class,
            ConstraintViolationException.class,
            BadRequestException.class
    })
    public ResponseEntity<Error> handleBadRequestSimilarExceptions(Exception ex) {
        log.error(ex.getMessage(), ex);

        List<ErrorReason> errorReasons = new ArrayList<>();
        errorReasons.add(
                ErrorReason.builder()
                        .code(COMMON_INVALID_PARAMETER)
                        .message(ex.getMessage())
                        .severity(ErrorReason.SeverityType.ERROR)
                        .build());

        return new ResponseEntity<>(
                buildErrorFromRequestParamters(errorReasons, HttpStatus.BAD_REQUEST),
                httpHeaders,
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class,
            TypeMismatchException.class
    })
    public ResponseEntity<Error> handleBadArgumentExceptions(Exception ex) {
        log.error(ex.getMessage(), ex);

        List<ErrorReason> errorReasons = new ArrayList<>();
        errorReasons.add(
                ErrorReason.builder()
                        .code(COMMON_INVALID_PARAMETER)
                        .message(getBadArgumentMessage(ex))
                        .severity(ErrorReason.SeverityType.ERROR)
                        .path(getBadArgument(ex))
                        .build());

        return new ResponseEntity<>(
                buildErrorFromRequestParamters(errorReasons, HttpStatus.BAD_REQUEST),
                httpHeaders,
                HttpStatus.BAD_REQUEST);
    }

    private String getBadArgumentMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException exception) {
            FieldError fieldError = exception.getBindingResult().getFieldErrors().get(0);

            return "Invalid "
                    + fieldError.getRejectedValue()
                    + " value submitted for "
                    + fieldError.getField();
        } else if (ex instanceof TypeMismatchException) {
            return "Argument type mismatch";
        } else if (ex instanceof IllegalArgumentException) {
            return "Illegal argument";
        }

        return "Bad argument exception";
    }

    private String getBadArgument(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException exception) {
            FieldError fieldError = exception.getBindingResult().getFieldErrors().get(0);

            return fieldError.getField();
        } else if (ex instanceof TypeMismatchException exception) {
            return exception.getPropertyName();
        }

        return null;
    }

    @ExceptionHandler({HttpClientErrorException.BadRequest.class})
    public ResponseEntity<Error> handleBadRequestException(Exception ex) {
        log.error(ex.getMessage(), ex);

        List<ErrorReason> errorReasons = new ArrayList<>();
        errorReasons.add(
                ErrorReason.builder()
                        .code(COMMON_INVALID_PARAMETER)
                        .message(ex.getMessage())
                        .severity(ErrorReason.SeverityType.ERROR)
                        .build());

        return new ResponseEntity<>(
                buildErrorFromRequestParamters(errorReasons, HttpStatus.BAD_REQUEST),
                httpHeaders,
                HttpStatus.BAD_REQUEST);
    }

    private Error buildErrorFromRequestParamters(List<ErrorReason> errorReasons, HttpStatus status) {
        return Error.builder()
                .errorId(UUID.randomUUID().toString())
                .correlationId(MDC.get(CONTEXT_CORRELATION_ID))
                .requestId(MDC.get(CONTEXT_REQUEST_ID))
                .reasons(errorReasons)
                .status(status.value())
                .build();
    }

    private void setupLoggingParametersFromWebRequest(WebRequest request) {
        final String urlPath = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        final Map<String, String[]> queryParams = request.getParameterMap();

        MDC.put(CONTEXT_REQUEST_ID, request.getHeader(X_REQUEST_ID));
        MDC.put(CONTEXT_CORRELATION_ID, request.getHeader(X_CORRELATION_ID));
        MDC.put(CONTEXT_TRACE_ID, request.getHeader(X_CORRELATION_ID));
        MDC.put(CONTEXT_SPAN_ID, UUID.randomUUID().toString());
        MDC.put(CONTEXT_PARENT_SPAN_ID, request.getHeader(X_CORRELATION_ID));
        MDC.put(CONTEXT_NWU_ID, request.getHeader(RICE_NWU_ID));
        MDC.put(CONTEXT_API_VERSION, request.getHeader(ACCEPT_VERSION));

        MDC.put(CONTEXT_REQUEST_URL, urlPath);
        MDC.put(CONTEXT_DATA, queryParams.toString());

        log.info("Request processing started...");
        MDC.remove("data");
    }
}
