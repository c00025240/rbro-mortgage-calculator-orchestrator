package ro.raiffeisen.internet.mortgage_calculator.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoanAllParameters {

  @JsonProperty private Integer idParameter;

  @JsonProperty private String currency;

  @JsonProperty private Boolean ourClient;

  @JsonProperty private Boolean hasLifeInsurance;

  @JsonProperty private String legalForm;

  @JsonProperty private String interestRateType;

  @JsonProperty private float analysisCommission;

  @JsonProperty private float paymentOrderCommission;

  @JsonProperty private float monthlyCurrentAccountCommission;

  @JsonProperty private float compulsoryPremiumInsuranceRate;

  @JsonProperty private float fngcimmCommission;

  @JsonProperty private float fixedMonthlyCommission;

  @JsonProperty private float postGrantCommission;

  @JsonProperty private float earlyRepaymentCommissionVariableInterest;

  @JsonProperty private float assessmentFee;

  @JsonProperty private Integer yearsWithFixedInterest;

  @JsonProperty private float variableInterestAfterFixedInterest;

  @JsonProperty private Integer maximumTerm;

  @JsonProperty private Integer minimumTerm;

  @JsonProperty private Integer minimumFixedInterestTerm;

  @JsonProperty private Integer minimumVariableInterestTerm;

  @JsonProperty private BigDecimal buildingPADInsurancePremiumRateEuro;

  @JsonProperty private BigDecimal buildingInsurancePremiumRate;

  @JsonProperty private BigDecimal lifeInsurance;

  @JsonProperty private Integer fkLoanProduct;

  @JsonProperty private Float discountValue;

  @JsonProperty private Boolean isDigital;

  @JsonProperty private Float monthlyInsuranceCostCalculationFormula;

  @JsonProperty private Float oneTimeInsuranceCostCalculationFormula;

  @JsonProperty private String assesmentFeeDescription;

  @JsonProperty private String postGrantCommissionDescription;

  @JsonProperty private String fngcimmCommissionDescription;

  @JsonProperty private String earlyRepaymentCommissionDescription;

  @JsonProperty private String monthlyInsuranceCostCalculationFormulaDescription;

  @JsonProperty private String oneTimeInsuranceCostCalculationFormulaDescription;

  @JsonProperty private Float ircc;
}
