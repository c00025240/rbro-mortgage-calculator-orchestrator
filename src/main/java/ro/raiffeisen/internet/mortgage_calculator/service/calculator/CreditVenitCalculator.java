package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.springframework.stereotype.Component;
import ro.raiffeisen.internet.mortgage_calculator.model.Amount;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationResponse;
import ro.raiffeisen.internet.mortgage_calculator.model.client.LoanProduct;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateTypeFormula;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator implementation for "Credit Venit" (Income-based credit) product.
 * This product can handle two scenarios:
 * 1. Client provides loan amount
 * 2. Calculate maximum loan amount based on income
 */
@Component
public class CreditVenitCalculator extends AbstractMortgageCalculator {

    public CreditVenitCalculator(ServiceUtil serviceUtil) {
        super(serviceUtil);
    }

    @Override
    public boolean supports(String productCode) {
        return "CreditVenit".equals(productCode);
    }

    @Override
    protected void calculateProductSpecificDetails(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate) {

        if (request.getLoanAmount() != null) {
            processLoanAmountProvided(request, response, additionalInfo, interestRateAdditionalInfo, rateTypeFormula, availableRate);
        } else {
            processLoanAmountNotProvided(request, response, additionalInfo, interestRateAdditionalInfo, rateTypeFormula, availableRate);
        }
    }

    private void processLoanAmountProvided(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate) {

        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal downPayment = request.getDownPayment();
        
        validateDownPaymentNotGreaterThanAmount(downPayment, loanAmount);

        boolean shouldApplyDownPaymentDiscount = shouldApplyDownPaymentDiscount(downPayment, request.getLoanAmount().getAmount());
        if (shouldApplyDownPaymentDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }

        calculateMaxAmount(request, response, additionalInfo.getCurrency(), rateTypeFormula, availableRate, loanAmount);
        
        response.setLoanAmount(new Amount(additionalInfo.getCurrency(), 
                serviceUtil.getAmountWithAnalysisCommission(loanAmount.subtract(downPayment), additionalInfo.getAnalysisCommission())));
        response.setLoanAmountWithFee(response.getLoanAmount());
        response.setDownPayment(new Amount(additionalInfo.getCurrency(), downPayment));
        response.setHousePrice(new Amount(additionalInfo.getCurrency(), loanAmount.add(downPayment)));

        request.setLoanAmount(new Amount(additionalInfo.getCurrency(), loanAmount.subtract(downPayment)));
    }

    private void processLoanAmountNotProvided(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate) {

        LoanProduct loanProduct = serviceUtil.retrieveLoanProduct(request.getProductCode());
        
        double maxLoanAmount = serviceUtil.calculatePV(rateTypeFormula.getInterestRate(), request.getTenor(), availableRate.doubleValue());
        request.setLoanAmount(new Amount("RON", BigDecimal.valueOf(maxLoanAmount)));

        // Recalculate additional info with the calculated loan amount
        additionalInfo = retrieveAdditionalInfo(request);

        request.setLoanAmount(new Amount(additionalInfo.getCurrency(), 
                BigDecimal.valueOf(maxLoanAmount).subtract(additionalInfo.getAnalysisCommission())));
        response.setMaxAmount(new Amount(additionalInfo.getCurrency(),
                BigDecimal.valueOf(maxLoanAmount).setScale(2, RoundingMode.HALF_DOWN)));

        BigDecimal garantie = calculateGuaranteeAmount(additionalInfo, maxLoanAmount);
        BigDecimal downPayment = garantie.subtract(BigDecimal.valueOf(maxLoanAmount));

        boolean shouldApplyDownPaymentDiscount = shouldApplyDownPaymentDiscount(downPayment, request.getLoanAmount().getAmount());
        if (shouldApplyDownPaymentDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }

        response.setHousePrice(new Amount(additionalInfo.getCurrency(), BigDecimal.valueOf(maxLoanAmount).add(downPayment)));
        response.setDownPayment(new Amount(additionalInfo.getCurrency(), downPayment));
        response.setLoanAmount(new Amount(additionalInfo.getCurrency(), BigDecimal.valueOf(maxLoanAmount)));
        response.setLoanAmountWithFee(response.getLoanAmount());
        response.setMinGuaranteeAmount(garantie);
    }

    @Override
    protected boolean shouldApplyGuaranteeDiscount(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo) {
        if (request.getLoanAmount() != null && request.getDownPayment() != null) {
            return shouldApplyDownPaymentDiscount(request.getDownPayment(), request.getLoanAmount().getAmount());
        }
        return false;
    }
}

