package ro.raiffeisen.internet.mortgage_calculator.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationResponse;
import ro.raiffeisen.internet.mortgage_calculator.service.MortgageCalculatorService;
import ro.raiffeisen.internet.mortgage_calculator.web.controller.api.LoanCalculatorApi;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoanCalculatorController implements LoanCalculatorApi {

    private final MortgageCalculatorService mortgageCalculatorService;

    @Override
    public ResponseEntity<MortgageCalculationResponse> createCalculation(String requestId,
                                                                         String correlationId,
                                                                         String xIdempotencyKey,
                                                                         String riceNwuId,
                                                                         String deviceSessionId,
                                                                         String deviceSessionProvider,
                                                                         MortgageCalculationRequest mortgageCalculationRequest) {
        return new ResponseEntity<>(mortgageCalculatorService.createCalculation(mortgageCalculationRequest), HttpStatus.OK);
    }

}
