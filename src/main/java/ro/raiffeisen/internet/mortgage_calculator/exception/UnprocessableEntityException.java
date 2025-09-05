package ro.raiffeisen.internet.mortgage_calculator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import ro.raiffeisen.internet.mortgage_calculator.model.ErrorReason;

import java.math.BigDecimal;
import java.util.List;

@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class UnprocessableEntityException extends RuntimeException {
    private final transient List<ErrorReason> errorReasons;
    private final transient BigDecimal displayedValue;

    public UnprocessableEntityException(String message, BigDecimal displayedValue) {
        super(message);
        this.displayedValue = displayedValue;
        this.errorReasons = null;
    }

    public UnprocessableEntityException(String message, List<ErrorReason> errorReasons, BigDecimal displayedValue) {
        super(message);
        this.errorReasons = errorReasons;
        this.displayedValue = displayedValue;
    }
}
