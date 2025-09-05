package ro.raiffeisen.internet.mortgage_calculator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import ro.raiffeisen.internet.mortgage_calculator.model.ErrorReason;

import java.util.List;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    private final transient List<ErrorReason> errorReasons;

    public NotFoundException(String message) {
        super(message);
        this.errorReasons = null;
    }

    public NotFoundException(String message, List<ErrorReason> errorReasons) {
        super(message);
        this.errorReasons = errorReasons;
    }
}
