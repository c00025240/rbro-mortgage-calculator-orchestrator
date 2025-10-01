package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ro.raiffeisen.internet.mortgage_calculator.exception.BadRequestException;

import java.util.List;

/**
 * Factory for creating the appropriate mortgage calculator based on product code.
 * Uses Spring's dependency injection to automatically collect all MortgageCalculator implementations.
 */
@Component
@RequiredArgsConstructor
public class MortgageCalculatorFactory {

    private final List<MortgageCalculator> calculators;

    /**
     * Returns the appropriate calculator for the given product code.
     * 
     * @param productCode the product code from the request
     * @return the matching calculator
     * @throws BadRequestException if no calculator is found for the product code
     */
    public MortgageCalculator getCalculator(String productCode) {
        return calculators.stream()
                .filter(calculator -> calculator.supports(productCode))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Unsupported product code: " + productCode));
    }
}

