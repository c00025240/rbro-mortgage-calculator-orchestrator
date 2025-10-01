package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.springframework.stereotype.Component;
import ro.raiffeisen.internet.mortgage_calculator.model.Amount;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationResponse;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateTypeFormula;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.math.BigDecimal;

/**
 * Calculator implementation for "Constructie" (Construction) credit product.
 */
@Component
public class ConstructieCalculator extends AbstractMortgageCalculator {

    public ConstructieCalculator(ServiceUtil serviceUtil) {
        super(serviceUtil);
    }

    @Override
    public boolean supports(String productCode) {
        return "Constructie".equals(productCode);
    }

    @Override
    protected void calculateProductSpecificDetails(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate) {

        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        String currency = additionalInfo.getCurrency();
        BigDecimal contributieProprie = request.getDownPayment() == null ? BigDecimal.ZERO : request.getDownPayment();

        if (contributieProprie.compareTo(loanAmount) > 0) {
            validateDownPaymentNotGreaterThanAmount(contributieProprie, loanAmount);
        }
        response.setDownPayment(new Amount(currency, contributieProprie));

        BigDecimal valoareCredit = request.getLoanAmount().getAmount().subtract(contributieProprie);
        BigDecimal garantie = calculateGuaranteeAmountBigDecimal(additionalInfo.getLtv(), valoareCredit);
        BigDecimal garantiePentruDiscount = calculateGuaranteeAmountBigDecimal(80, valoareCredit);
        BigDecimal sumeFaraJustificare = calculateNoDocAmount(valoareCredit);

        response.setMinGuaranteeAmount(garantie);
        response.setNoDocAmount(sumeFaraJustificare);
        response.setHousePrice(new Amount(additionalInfo.getCurrency(), garantiePentruDiscount));

        boolean shouldApplyGuaranteeDiscount = garantiePentruDiscount.compareTo(garantie) <= 0;
        if (shouldApplyGuaranteeDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }

        calculateMaxAmount(request, response, currency, rateTypeFormula, availableRate, valoareCredit);

        response.setLoanAmount(new Amount(currency, serviceUtil.getAmountWithAnalysisCommission(
                loanAmount.subtract(contributieProprie), additionalInfo.getAnalysisCommission())));
        response.setLoanAmountWithFee(response.getLoanAmount());

        request.setLoanAmount(new Amount(currency, loanAmount.subtract(contributieProprie)));
    }

    @Override
    protected boolean shouldApplyGuaranteeDiscount(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo) {
        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal contributieProprie = request.getDownPayment() == null ? BigDecimal.ZERO : request.getDownPayment();
        BigDecimal valoareCredit = loanAmount.subtract(contributieProprie);
        BigDecimal garantie = calculateGuaranteeAmountBigDecimal(additionalInfo.getLtv(), valoareCredit);
        BigDecimal garantiePentruDiscount = calculateGuaranteeAmountBigDecimal(80, valoareCredit);
        return garantiePentruDiscount.compareTo(garantie) <= 0;
    }
}

