package ro.raiffeisen.internet.mortgage_calculator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ro.raiffeisen.internet.mortgage_calculator.helper.MortgageCalculatorMapper;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationResponse;
import ro.raiffeisen.internet.mortgage_calculator.service.calculator.MortgageCalculator;
import ro.raiffeisen.internet.mortgage_calculator.service.calculator.MortgageCalculatorFactory;

import static ro.raiffeisen.internet.mortgage_calculator.config.CacheConfig.MORTGAGE_CALCULATION_CACHE;

/**
 * Main service for mortgage calculation orchestration.
 * This service has been refactored to use the Strategy pattern with specific calculators
 * for each credit type, eliminating code duplication and improving maintainability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MortgageCalculatorService {

    private final ServiceUtil serviceUtil;
    private final MortgageCalculatorMapper mapper;
    private final ValidationService validationService;
    private final MortgageCalculatorFactory calculatorFactory;

    /**
     * Creates a mortgage calculation based on the request.
     * Uses the factory to delegate to the appropriate calculator based on product code.
     * 
     * Results are cached for 24 hours (1 day) to maximize performance for repeated calculations.
     * Cache key is based on the entire request object.
     * 
     * @param request the mortgage calculation request
     * @return the calculated mortgage response
     */
    @Cacheable(value = MORTGAGE_CALCULATION_CACHE, key = "#request.toString()")
    public MortgageCalculationResponse createCalculation(MortgageCalculationRequest request) {
        log.info("/calculator/mortgage-calculator request body:" + mapper.mapToJson(request));
        validationService.validateRequest(request);

        // Calculate maximum tenor based on age
        int maxTenor = serviceUtil.calculateMaxPeriod(request.getAge(), request.getTenor());
        request.setTenor(maxTenor * 12);
        
        // Initialize response
        MortgageCalculationResponse response = MortgageCalculationResponse.builder()
                .tenor(maxTenor)
                .build();

        // Get the appropriate calculator and perform calculation
        MortgageCalculator calculator = calculatorFactory.getCalculator(request.getProductCode());
        calculator.calculate(request, response);

        return response;
    }
}
