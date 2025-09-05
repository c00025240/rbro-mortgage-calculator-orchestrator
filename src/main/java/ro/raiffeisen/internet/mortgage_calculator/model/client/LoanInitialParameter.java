package ro.raiffeisen.internet.mortgage_calculator.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoanInitialParameter {

  @JsonProperty private Integer idParameter;

  @JsonProperty private String currency;

  @JsonProperty private Boolean ourClient;

  @JsonProperty private Boolean hasLifeInsurance;

  @JsonProperty private String legalForm;

  @JsonProperty private String interestRateType;

  @JsonProperty private float sum;

  @JsonProperty private String periodType;

  @JsonProperty private Integer period;

  @JsonProperty private Integer fkLoan;
}
