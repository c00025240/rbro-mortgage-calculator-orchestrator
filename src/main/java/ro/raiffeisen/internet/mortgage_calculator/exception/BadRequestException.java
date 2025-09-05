package ro.raiffeisen.internet.mortgage_calculator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import ro.raiffeisen.internet.mortgage_calculator.model.ErrorReason;

import java.util.List;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    private final transient List<ErrorReason> errorReasons;

    public BadRequestException(String message) {
        super(message);
        this.errorReasons = null;
    }

    public BadRequestException(String message, List<ErrorReason> errorReasons) {
        super(message);
        this.errorReasons = errorReasons;
    }
}
