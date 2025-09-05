package ro.raiffeisen.internet.mortgage_calculator.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoanProduct {

  @JsonProperty private Integer idLoan;

  @JsonProperty private String labelLoan;

  @JsonProperty private String productLoan;

  @JsonProperty private LocalDateTime createDate;
}
