package ro.raiffeisen.internet.mortgage_calculator.service;


import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ro.raiffeisen.internet.mortgage_calculator.exception.InternalServerException;
import ro.raiffeisen.internet.mortgage_calculator.model.client.ExchangeRate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@Data
@RequiredArgsConstructor
public class FxClientRetrieve {

    private final RestTemplate restTemplate;
    @Value("${fx-rates.client.endpoints.fx-rates-service}")
    private String url;

    public List<ExchangeRate> getExchangeRates(String currency) {
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("currency", currency)
                    .queryParam("validityDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .encode()
                    .toUriString();

            ParameterizedTypeReference<List<ExchangeRate>> responseType = new ParameterizedTypeReference<>() {
            };

            return restTemplate.exchange(urlTemplate, HttpMethod.GET, new HttpEntity<>(getHeaders()), responseType).getBody();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new InternalServerException("Unexpected error occurred during call fx-rates");
        }
    }

    public HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set("X-Correlation-ID", MDC.get("correlation_id"));
        httpHeaders.set("X-Request-ID", MDC.get("request_id"));
        httpHeaders.set("RICE-NWU-ID", MDC.get("nwu_id"));

        return httpHeaders;
    }
}
