package ro.raiffeisen.internet.mortgage_calculator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class Income {

    @Schema(description = "Income",
            pattern = "-?[0-9]{1,14}(\\.[0-9]{1,3})?",
            example = "5877.78",
            required = true)
    private BigDecimal currentIncome;

    @Schema(description = "otherInstallments",
            pattern = "-?[0-9]{1,14}(\\.[0-9]{1,3})?",
            example = "5877.78",
            required = true)
    private BigDecimal otherInstallments;

}
