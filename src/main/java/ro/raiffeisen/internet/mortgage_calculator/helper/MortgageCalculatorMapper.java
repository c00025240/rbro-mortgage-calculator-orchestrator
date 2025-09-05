package ro.raiffeisen.internet.mortgage_calculator.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ro.raiffeisen.internet.mortgage_calculator.model.client.LoanAllParameters;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;

import java.math.BigDecimal;

@Component
@Slf4j
public class MortgageCalculatorMapper {

    public AdditionalCalculationInfo buildAllAdditionalInfo(LoanAllParameters loanAllParameters, Integer ltv) {
        return AdditionalCalculationInfo.builder()
                .lifeInsurance(loanAllParameters.getLifeInsurance())
                .monthlyCurrentAccountCommission(BigDecimal.valueOf(loanAllParameters.getMonthlyCurrentAccountCommission()))
                .analysisCommission(BigDecimal.valueOf(loanAllParameters.getAnalysisCommission()))
                .feeCommission(BigDecimal.valueOf(loanAllParameters.getAssessmentFee()))
                .paymentOrderCommission(BigDecimal.valueOf(loanAllParameters.getPaymentOrderCommission()))
                .postGrantCommission(BigDecimal.valueOf(loanAllParameters.getPostGrantCommission()))
                .buildingPADInsurancePremiumRate(loanAllParameters.getBuildingPADInsurancePremiumRateEuro())
                .buildingInsurancePremiumRate(BigDecimal.valueOf(loanAllParameters.getCompulsoryPremiumInsuranceRate()))
                .oneTimeInsuranceCostCalculationFormula(loanAllParameters.getOneTimeInsuranceCostCalculationFormula())
                .monthlyInsuranceCostCalculationFormula(loanAllParameters.getMonthlyInsuranceCostCalculationFormula())
                .ltv(ltv)
                .ircc(loanAllParameters.getIrcc())
                .build();
    }


    public String mapToJson(Object obj) {
        String defaultMap = "{}";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.registerModule(new JavaTimeModule()).writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while mapping java object to json", e);
            return defaultMap;
        }
    }
}
