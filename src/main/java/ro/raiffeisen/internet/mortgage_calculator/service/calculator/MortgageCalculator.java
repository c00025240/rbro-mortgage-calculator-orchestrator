package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationResponse;

/**
 * Interface for mortgage calculation strategies.
 * Each credit product type will have its own implementation.
 */
public interface MortgageCalculator {
    
    /**
     * Checks if this calculator can handle the given product code.
     * 
     * @param productCode the product code from the request
     * @return true if this calculator supports the product code
     */
    boolean supports(String productCode);
    
    /**
     * Calculates the mortgage details for the given request.
     * 
     * @param request the mortgage calculation request
     * @param response the response object to be populated
     */
    void calculate(MortgageCalculationRequest request, MortgageCalculationResponse response);
}

