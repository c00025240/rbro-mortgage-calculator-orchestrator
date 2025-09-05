package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Area {

    @Schema(description = "The city", example = "Bucharest")
    @JsonProperty
    String city;

    @Schema(description = "The county", example = "Bucharest")
    @JsonProperty
    String county;
}
