package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import lombok.RequiredArgsConstructor;
import ro.raiffeisen.internet.mortgage_calculator.exception.UnprocessableEntityException;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;
import ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.CalculatedValues;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateTypeFormula;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for all mortgage calculators.
 * Contains common logic shared across all credit types using Template Method pattern.
 */
@RequiredArgsConstructor
public abstract class AbstractMortgageCalculator implements MortgageCalculator {

    protected final ServiceUtil serviceUtil;

    /**
     * Template method that defines the skeleton of the calculation algorithm.
     * Subclasses implement specific steps while common logic is handled here.
     */
    @Override
    public void calculate(MortgageCalculationRequest request, MortgageCalculationResponse response) {
        // Retrieve common data
        AdditionalCalculationInfo additionalInfo = retrieveAdditionalInfo(request);
        InterestRateAdditionalInfo interestRateAdditionalInfo = retrieveInterestRate(request, additionalInfo.getProductId());
        
        // Calculate interest rate based on initially selected discounts
        InterestRateTypeFormula rateTypeFormula = calculateInterestRateBasedOnChosenDiscounts(interestRateAdditionalInfo, request);
        
        // Calculate available rate
        BigDecimal availableRate = serviceUtil.calculateAvailableRate(request.getIncome());
        
        // Execute product-specific calculation logic
        calculateProductSpecificDetails(request, response, additionalInfo, interestRateAdditionalInfo, rateTypeFormula, availableRate);
        
        // Calculate common details (repayment plan, installments, etc.)
        calculateCommonDetails(request, additionalInfo, response, interestRateAdditionalInfo, rateTypeFormula, shouldApplyGuaranteeDiscount(request, additionalInfo));
    }

    /**
     * Product-specific calculation logic to be implemented by subclasses.
     * This method should set product-specific fields in the response.
     */
    protected abstract void calculateProductSpecificDetails(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate);

    /**
     * Determines if the guarantee/down payment discount should be applied.
     * Can be overridden by subclasses for product-specific logic.
     */
    protected boolean shouldApplyGuaranteeDiscount(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo) {
        return false; // Default implementation, can be overridden
    }

    // ===== Common calculation methods =====

    protected AdditionalCalculationInfo retrieveAdditionalInfo(MortgageCalculationRequest request) {
        return serviceUtil.retrieveAdditionalInfo(request);
    }

    protected InterestRateAdditionalInfo retrieveInterestRate(MortgageCalculationRequest request, Integer productId) {
        return serviceUtil.retrieveInterestRate(request, productId);
    }

    protected InterestRateTypeFormula calculateInterestRateBasedOnChosenDiscounts(
            InterestRateAdditionalInfo additionalInfo, 
            MortgageCalculationRequest request) {
        double interestRate = additionalInfo.getDefaultInterestRate();
        double variableInterestRate = additionalInfo.getDefaultVariableInterestAfterFixedInterest();
        double bankMargin = additionalInfo.getDefaultBankMarginRate();
        InterestRateTypeFormula interestRateTypeFormula = new InterestRateTypeFormula(interestRate, bankMargin, variableInterestRate);

        if (request.getSpecialOfferRequirements().isHasSalaryInTheBank())
            applyDiscount(additionalInfo, "client", interestRateTypeFormula);

        if (request.getSpecialOfferRequirements().isCasaVerde())
            applyDiscount(additionalInfo, "green house", interestRateTypeFormula);

        if (request.isHasInsurance())
            applyDiscount(additionalInfo, "asigurare", interestRateTypeFormula);

        return interestRateTypeFormula;
    }

    protected void applyDiscount(InterestRateAdditionalInfo additionalInfo, String discountName, InterestRateTypeFormula formula) {
        double discountValue = getDiscountValue(additionalInfo, discountName);

        formula.setInterestRate(formula.getInterestRate() - discountValue);
        formula.setVariableInterestAfterFixedInterest(formula.getVariableInterestAfterFixedInterest() - discountValue);
        formula.setBankMarginRate(formula.getBankMarginRate() - discountValue);
    }

    protected double getDiscountValue(InterestRateAdditionalInfo additionalInfo, String discountName) {
        return additionalInfo.getDiscounts().stream()
                .filter(discount -> discount.getDiscountName().equals(discountName))
                .map(Discount::getDiscountValue)
                .findFirst()
                .orElse(0d);
    }

    protected void calculateMaxAmount(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            String currency,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate,
            BigDecimal loanAmount) {
        double maxLoanAmount = serviceUtil.calculatePV(rateTypeFormula.getInterestRate(), request.getTenor(), availableRate.doubleValue());
        BigDecimal maxLoanAmountBd = BigDecimal.valueOf(maxLoanAmount).setScale(2, RoundingMode.HALF_DOWN);
        response.setMaxAmount(new Amount(currency, maxLoanAmountBd));

        if (maxLoanAmountBd.compareTo(loanAmount) < 0) {
            throw new UnprocessableEntityException(String.format("Ne pare rau! ☹️\n" +
                    "Valoarea creditului este prea mare pentru venitul si cheltuielile tale! Te rugam sa incerci o suma mai mica decat %f Lei", maxLoanAmountBd), maxLoanAmountBd);
        }
    }

    protected boolean shouldApplyDownPaymentDiscount(BigDecimal downPayment, BigDecimal amount) {
        return downPayment.compareTo(BigDecimal.valueOf(0.2).multiply(amount)) >= 0;
    }

    protected BigDecimal calculateGuaranteeAmountBigDecimal(Integer ltv, BigDecimal valoareCredit) {
        return BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(ltv), 6, RoundingMode.HALF_UP)
                .multiply(valoareCredit);
    }

    protected BigDecimal calculateGuaranteeAmount(AdditionalCalculationInfo additionalInfo, double maxLoanAmount) {
        return BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(additionalInfo.getLtv()), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(maxLoanAmount).add(additionalInfo.getAnalysisCommission()));
    }

    protected BigDecimal calculateNoDocAmount(BigDecimal valoareCredit) {
        return BigDecimal.valueOf(0.3).multiply(valoareCredit);
    }

    protected void validateDownPaymentNotGreaterThanAmount(BigDecimal downPayment, BigDecimal loanAmount) {
        if (downPayment.compareTo(loanAmount) > 0) {
            throw new UnprocessableEntityException("Ne pare rau! Contributia proprie nu poate fi mai mare decat suma solicitata", null);
        }
    }

    private void calculateCommonDetails(
            MortgageCalculationRequest request,
            AdditionalCalculationInfo additionalInfo,
            MortgageCalculationResponse response,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            boolean shouldApplyDownPaymentDiscount) {
        
        additionalInfo.setBuildingInsurancePremiumRate(serviceUtil.calculateBuildingInsurancePremiumRate(
                additionalInfo.getCurrency(),
                request.getLoanAmount(),
                additionalInfo.getAnalysisCommission(),
                additionalInfo.getLtv(),
                additionalInfo.getBuildingInsurancePremiumRate()));

        LoanCosts loanCosts = new LoanCosts();
        CalculatedValues calculatedValues = calculateBasedOnDiscounts(additionalInfo, request, loanCosts, interestRateAdditionalInfo);
        calculateTotalDiscounts(calculatedValues, loanCosts, request, shouldApplyDownPaymentDiscount);

        interestRateAdditionalInfo.setInterestRate(rateTypeFormula.getInterestRate());
        interestRateAdditionalInfo.setVariableInterestAfterFixedInterest(rateTypeFormula.getVariableInterestAfterFixedInterest());
        interestRateAdditionalInfo.setBankMarginRate(rateTypeFormula.getBankMarginRate());

        List<RepaymentPlanEntry> repaymentPlanEntries = createRepaymentPlanEntries(request, additionalInfo, interestRateAdditionalInfo);

        response.setMonthlyInstallment(serviceUtil.calculateMonthlyInstallment(
                request.isHasInsurance(),
                repaymentPlanEntries,
                request.getInterestRateType(),
                additionalInfo.getMonthlyLifeInsurance().getValue().getAmount()));
        response.setInterestRateType(request.getInterestRateType());
        response.setInterestRateFormula(getInterestRateFormattedFormula(interestRateAdditionalInfo.getBankMarginRate(), additionalInfo.getIrcc()));
        response.setNominalInterestRate(BigDecimal.valueOf(interestRateAdditionalInfo.getInterestRate()).setScale(2, RoundingMode.HALF_DOWN));

        List<BigDecimal> cashFlows = repaymentPlanEntries.stream()
                .map(repaymentPlanEntry -> repaymentPlanEntry.getTotalPaymentAmount().getAmount())
                .collect(Collectors.toList());

        BigDecimal amountWithAnalysisCommission = serviceUtil.getAmountWithAnalysisCommission(
                request.getLoanAmount().getAmount(),
                additionalInfo.getAnalysisCommission());
        BigDecimal dae = serviceUtil.calculateDAE(cashFlows, amountWithAnalysisCommission, additionalInfo);

        loanCosts.setLifeInsurance(List.of(additionalInfo.getMonthlyLifeInsurance()));

        response.setLoanCosts(loanCosts);
        response.setAnnualPercentageRate(dae);
        response.setTotalPaymentAmount(serviceUtil.calculateTotalPayment(cashFlows, additionalInfo));
        response.setCommissionDescription(additionalInfo.getCommissionDescription());
    }

    private InterestRateFormula getInterestRateFormattedFormula(double bankMarginRate, Float ircc) {
        DecimalFormat df = new DecimalFormat("#.00");
        double formattedBankMarginRate = Double.parseDouble(df.format(bankMarginRate));
        double formattedIrcc = Double.parseDouble(df.format(ircc));

        return InterestRateFormula.builder()
                .bankMarginRate(formattedBankMarginRate)
                .irccRate(formattedIrcc)
                .build();
    }

    private List<RepaymentPlanEntry> createRepaymentPlanEntries(
            MortgageCalculationRequest request,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo) {
        int tenor = request.getTenor();
        // Pre-allocate capacity to avoid resizing during adds
        List<RepaymentPlanEntry> repaymentPlanEntries = new ArrayList<>(tenor + 1);
        
        for (int i = 0; i <= tenor; i++) {
            repaymentPlanEntries.add(serviceUtil.createRepaymentPlanEntry(i, request, additionalInfo, interestRateAdditionalInfo));
        }
        
        return repaymentPlanEntries;
    }

    private void calculateTotalDiscounts(
            CalculatedValues calculatedValues,
            LoanCosts loanCosts,
            MortgageCalculationRequest request,
            boolean shouldApplyDownPaymentDiscount) {
        BigDecimal totalDiscountInstallment = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        if (request.getSpecialOfferRequirements().isHasSalaryInTheBank()) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountHasSalaryInTheBank());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountHasSalaryInTheBank(), request, loanCosts.getDiscounts().getDiscountAmountHasSalaryInTheBank()));
        }

        if (request.getSpecialOfferRequirements().isCasaVerde()) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountCasaVerde());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountCasaVerde(), request, loanCosts.getDiscounts().getDiscountAmountCasaVerde()));
        }

        if (request.isHasInsurance()) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountInsurance());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountInsurance(), request, loanCosts.getDiscounts().getDiscountAmountInsurance()));
        }

        if (shouldApplyDownPaymentDiscount) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountDownPayment());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountDownPayment(), request, loanCosts.getDiscounts().getDiscountAmountDownPayment()));
        }

        loanCosts.setTotalDiscountsValues(new TotalDiscountsValues(totalDiscountInstallment, totalDiscountAmount));
    }

    private BigDecimal calculateTotalDiscountAmount(BigDecimal calculatedVariableValue, MortgageCalculationRequest request, BigDecimal discountAmount) {
        if (request.getInterestRateType() instanceof MixedInterestRateType interestRateType) {
            return discountAmount.multiply(BigDecimal.valueOf(interestRateType.getFixedPeriod())).multiply(new BigDecimal(12))
                    .add(calculatedVariableValue.multiply(BigDecimal.valueOf(request.getTenor() - interestRateType.getFixedPeriod() * 12L)));
        }
        return discountAmount.multiply(BigDecimal.valueOf(request.getTenor()));
    }

    private CalculatedValues calculateBasedOnDiscounts(
            AdditionalCalculationInfo additionalInfo,
            MortgageCalculationRequest request,
            LoanCosts loanCosts,
            InterestRateAdditionalInfo interestRateAdditionalInfo) {
        double defaultInterestRate = interestRateAdditionalInfo.getDefaultInterestRate();
        
        // Extract all discount values in a single pass using a Map
        Map<String, Double> discountMap = interestRateAdditionalInfo.getDiscounts().stream()
                .collect(Collectors.toMap(
                        ro.raiffeisen.internet.mortgage_calculator.model.client.Discount::getDiscountName,
                        ro.raiffeisen.internet.mortgage_calculator.model.client.Discount::getDiscountValue
                ));
        
        // Get individual discount values with defaults
        double avansDiscount = discountMap.getOrDefault("avans", 0.0);
        double casaVerdeDiscount = discountMap.getOrDefault("green house", 0.0);
        double asigurareDiscount = discountMap.getOrDefault("asigurare", 0.0);
        double clientDiscount = discountMap.getOrDefault("client", 0.0);
        
        DiscountsValues discountsValues = new DiscountsValues();
        
        // Calculate base payment once (no discount)
        interestRateAdditionalInfo.setInterestRate(defaultInterestRate);
        List<RepaymentPlanEntry> baseEntries = createPartialRepaymentPlanEntries(request, additionalInfo, interestRateAdditionalInfo);
        BigDecimal basePayment = baseEntries.get(1).getTotalPaymentAmount().getAmount();
        
        // Calculate each discount's impact (only if discount value > 0)
        discountsValues.setDiscountAmountDownPayment(
                calculateDiscountImpact(avansDiscount, defaultInterestRate, basePayment, request, additionalInfo, interestRateAdditionalInfo));
        discountsValues.setDiscountAmountCasaVerde(
                calculateDiscountImpact(casaVerdeDiscount, defaultInterestRate, basePayment, request, additionalInfo, interestRateAdditionalInfo));
        discountsValues.setDiscountAmountInsurance(
                calculateDiscountImpact(asigurareDiscount, defaultInterestRate, basePayment, request, additionalInfo, interestRateAdditionalInfo));
        discountsValues.setDiscountAmountHasSalaryInTheBank(
                calculateDiscountImpact(clientDiscount, defaultInterestRate, basePayment, request, additionalInfo, interestRateAdditionalInfo));

        loanCosts.setDiscounts(discountsValues);

        CalculatedValues calculatedValues = new CalculatedValues();
        if (request.getInterestRateType() instanceof MixedInterestRateType) {
            // For mixed rate, calculate variable discount impacts
            defaultInterestRate = interestRateAdditionalInfo.getDefaultVariableInterestAfterFixedInterest();
            
            interestRateAdditionalInfo.setInterestRate(defaultInterestRate);
            List<RepaymentPlanEntry> varBaseEntries = createPartialRepaymentPlanEntries(request, additionalInfo, interestRateAdditionalInfo);
            BigDecimal varBasePayment = varBaseEntries.get(1).getTotalPaymentAmount().getAmount();
            
            calculatedValues.setVariableDiscountAmountDownPayment(
                    calculateDiscountImpact(avansDiscount, defaultInterestRate, varBasePayment, request, additionalInfo, interestRateAdditionalInfo));
            calculatedValues.setVariableDiscountAmountCasaVerde(
                    calculateDiscountImpact(casaVerdeDiscount, defaultInterestRate, varBasePayment, request, additionalInfo, interestRateAdditionalInfo));
            calculatedValues.setVariableDiscountAmountInsurance(
                    calculateDiscountImpact(asigurareDiscount, defaultInterestRate, varBasePayment, request, additionalInfo, interestRateAdditionalInfo));
            calculatedValues.setVariableDiscountAmountHasSalaryInTheBank(
                    calculateDiscountImpact(clientDiscount, defaultInterestRate, varBasePayment, request, additionalInfo, interestRateAdditionalInfo));
        }

        return calculatedValues;
    }

    /**
     * Calculate the impact of a single discount on the payment amount.
     * Returns ZERO if discount value is 0, otherwise calculates the difference.
     */
    private BigDecimal calculateDiscountImpact(
            double discountValue,
            double baseInterestRate,
            BigDecimal basePayment,
            MortgageCalculationRequest request,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo) {
        
        if (discountValue == 0) {
            return BigDecimal.ZERO;
        }
        
        interestRateAdditionalInfo.setInterestRate(baseInterestRate - discountValue);
        List<RepaymentPlanEntry> discountEntries = createPartialRepaymentPlanEntries(request, additionalInfo, interestRateAdditionalInfo);
        BigDecimal discountPayment = discountEntries.get(1).getTotalPaymentAmount().getAmount();
        
        return basePayment.subtract(discountPayment);
    }


    private List<RepaymentPlanEntry> createPartialRepaymentPlanEntries(
            MortgageCalculationRequest request,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo) {
        List<RepaymentPlanEntry> repaymentPlanEntries = new ArrayList<>();
        for (int i = 0; i <= 1; i++) {
            repaymentPlanEntries.add(serviceUtil.createRepaymentPlanEntry(i, request, additionalInfo, interestRateAdditionalInfo));
        }
        return repaymentPlanEntries;
    }
}

