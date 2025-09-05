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
public class NomenclatureDistrict {

    @JsonProperty private Integer id;
    @JsonProperty private String county;
    @JsonProperty private String city;
    @JsonProperty private Integer zone;
}
