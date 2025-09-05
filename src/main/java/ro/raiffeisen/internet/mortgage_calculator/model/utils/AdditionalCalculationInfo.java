package ro.raiffeisen.internet.mortgage_calculator.model.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.raiffeisen.internet.mortgage_calculator.model.CommissionDescription;
import ro.raiffeisen.internet.mortgage_calculator.model.LifeInsurance;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdditionalCalculationInfo {

    @JsonProperty private Float ircc;

    @JsonProperty private double variableInterestAfterFixedInterest;

    @JsonProperty private double defaultVariableInterestAfterFixedInterest;

    /** Comision lunar cont curent */
    @JsonProperty private BigDecimal monthlyCurrentAccountCommission;

    /** Comision OP */
    @JsonProperty private BigDecimal paymentOrderCommission;

    /** Comision de analiza dosar credit */
    @JsonProperty private BigDecimal analysisCommission;

    /** Taxa de evaluare */
    @JsonProperty private BigDecimal feeCommission;

    /** Cota prima asigurare imobil */
    @JsonProperty private BigDecimal buildingInsurancePremiumRate;

    /** Cota prima asigurare PAD imobil */
    @JsonProperty private BigDecimal buildingPADInsurancePremiumRate;

    /** Comision OPC */
    @JsonProperty private BigDecimal postGrantCommission;

    @JsonProperty private BigDecimal principal;

    @JsonProperty private BigDecimal interest;


    /** Comisioanele pe fiecare luna */
    @JsonProperty private BigDecimal fee;

    /** Sold */
    @JsonProperty private BigDecimal balance;

    /** Soldul anterior pentru calcul dobanda */
    @JsonProperty private BigDecimal previousBalance;

    /** Soldul pentru calcul principal */
    @JsonProperty private BigDecimal referenceBalance;

    @JsonProperty private String currency;

    @JsonProperty private BigDecimal rate;

    @JsonProperty private boolean isMortgageLoan;

    @JsonProperty private boolean isDigital;

    @JsonProperty private BigDecimal lifeInsurance;

    @JsonProperty private Float discountValue;

    @JsonProperty private CommissionDescription commissionDescription;

    /** Valoare folista in calculul asigurarii lunare */
    @JsonProperty private Float monthlyInsuranceCostCalculationFormula;

    /** Valoare folista in calculul asigurarii unice */
    @JsonProperty private Float oneTimeInsuranceCostCalculationFormula;

    /** Valoare procent Fngcimm */
    @JsonProperty private Float fngcimmPercent;

    @JsonProperty private Integer ltv;
    // pentru a obtine restul de informatii
    @JsonProperty private Integer productId;
    @JsonProperty private LifeInsurance monthlyLifeInsurance;
}
