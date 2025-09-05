package ro.raiffeisen.internet.mortgage_calculator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ro.raiffeisen.internet.mortgage_calculator.exception.InternalServerException;
import ro.raiffeisen.internet.mortgage_calculator.model.client.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class RetrieveService {
    @Autowired
    private final RestTemplate restTemplate;
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${loan-admin-service.client.endpoints.rbro-loan-calculation-admin-service}")
    private String url;


    public LoanAllParameters getLoanAllParametersByMultipleArguments(Integer fkLoanProduct,
                                                                     boolean ourClient,
                                                                     String currency,
                                                                     String interestRateType, boolean isDigital) {
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url + "/v1/get-all-parameters/")
                    .queryParam("fkLoanProduct", fkLoanProduct)
                    .queryParam("ourClient", ourClient)
                    .queryParam("currency", currency)
                    .queryParam("interestRateType", interestRateType)
                    .queryParam("isDigital", isDigital)
                    .encode()
                    .toUriString();

            return restTemplate.exchange(urlTemplate, HttpMethod.GET, new HttpEntity<>(getHeaders()), LoanAllParameters.class).getBody();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new InternalServerException("Unexpected error occurred during call rbro-loan-calculator-admin-service - path: /v1/get-all-parameters/");
        }
    }

    public List<LoanInterestRate> getAllLoanInterestRatesByLoanProduct(Integer fkLoanProduct, boolean ourClient, boolean isDigital) {
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url + "/v1/get-loan-interest-rates/")
                    .queryParam("fkLoanProduct", fkLoanProduct)
                    .queryParam("isDigital", isDigital)
                    .queryParam("ourClient", ourClient)
                    .encode()
                    .toUriString();

            ParameterizedTypeReference<List<LoanInterestRate>> responseType = new ParameterizedTypeReference<>() {
            };

            return restTemplate.exchange(urlTemplate, HttpMethod.GET, new HttpEntity<>(getHeaders()), responseType).getBody();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new InternalServerException("Unexpected error occurred during call rbro-loan-calculator-admin-service - path: /v1/get-loan-interest-rates/");
        }
    }

    public Integer getLtvByAreaOwnerAndSum(Double amount, Boolean isOwner, Integer financingZone, Integer idLoan) {
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url + "/v1/ltv")
                    .queryParam("amount", amount)
                    .queryParam("isOwner", isOwner)
                    .queryParam("financingZone", financingZone)
                    .queryParam("idLoan", idLoan)
                    .encode()
                    .toUriString();

            return restTemplate.exchange(urlTemplate, HttpMethod.GET, new HttpEntity<>(getHeaders()), Integer.class).getBody();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new InternalServerException("Unexpected error occurred during call rbro-loan-calculator-admin-service - path: /v1/ltv/");
        }
    }

    public HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.set("X-Correlation-ID", MDC.get("correlation_id"));
        httpHeaders.set("X-B3-TraceId", MDC.get("trace_id"));
        httpHeaders.set("X-B3-SpanId", MDC.get("trace_id"));
        httpHeaders.set("X-RBRO-Request-ID", MDC.get("request_id"));
        httpHeaders.set("X-Idempotency-Key", MDC.get("idempotency_key"));
        httpHeaders.set("X-RBRO-ApplicationName", applicationName);
        httpHeaders.set("X-RBRO-ApplicationUsername", "APP_USER");
        httpHeaders.set("X-Idempotency-Key", MDC.get("idempotency_key"));
        httpHeaders.set("RICE-NWU-ID", MDC.get("nwu_id"));

        return httpHeaders;
    }

    public LoanProduct getLoanProduct(String productCode) {
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url + "/v1/product")
                    .queryParam("productCode", productCode)
                    .encode()
                    .toUriString();

            return restTemplate.exchange(urlTemplate, HttpMethod.GET, new HttpEntity<>(getHeaders()), LoanProduct.class).getBody();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new InternalServerException("Unexpected error occurred during call rbro-loan-calculator-admin-service - path: /v1/product");
        }
    }

    public List<NomenclatureDistrict> getDistricts() {
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url + "/v1/districts")
                    .encode()
                    .toUriString();

            ParameterizedTypeReference<List<NomenclatureDistrict>> responseType = new ParameterizedTypeReference<>() {
            };
            return restTemplate.exchange(urlTemplate, HttpMethod.GET, new HttpEntity<>(getHeaders()), responseType).getBody();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new InternalServerException("Unexpected error occurred during call rbro-loan-calculator-admin-service - path: /v1/districts/");
        }
    }

    public List<Discount> getDiscounts(Integer idLoan) {
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url + "/v1/discounts")
                    .queryParam("idLoan", idLoan)
                    .encode()
                    .toUriString();

            ParameterizedTypeReference<List<Discount>> responseType = new ParameterizedTypeReference<>() {
            };
            return restTemplate.exchange(urlTemplate, HttpMethod.GET, new HttpEntity<>(getHeaders()), responseType).getBody();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new InternalServerException("Unexpected error occurred during call rbro-loan-calculator-admin-service - path: /v1/discounts/");
        }
    }
}
