package ro.raiffeisen.internet.mortgage_calculator.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ro.raiffeisen.internet.mortgage_calculator.exception.InternalServerException;
import ro.raiffeisen.internet.mortgage_calculator.model.client.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RetrieveServiceTest {
    private static final RestTemplate restTemplate = mock(RestTemplate.class);
    private static final RetrieveService clientRetrieve = new RetrieveService(restTemplate);
    private final static String URL = "http://localhost:8090/app/loan-admin";

    @BeforeAll
    public static void setUp() {
        ReflectionTestUtils.setField(clientRetrieve, "url", URL);
    }

    @Test
    public void getLoanAllParametersByMultipleArguments_successfullyCase() {
        String expectedUrl = UriComponentsBuilder.fromHttpUrl(URL + "/v1/get-all-parameters/")
                .queryParam("fkLoanProduct", 1)
                .queryParam("ourClient", false)
                .queryParam("currency", "RON")
                .queryParam("interestRateType", "Dobanda fixa")
                .queryParam("isDigital", false)
                .encode()
                .toUriString();
        LoanAllParameters expectedResponse = getLoanAllParameters();

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(LoanAllParameters.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        LoanAllParameters actualResponse = clientRetrieve.
                getLoanAllParametersByMultipleArguments(
                        1,
                        false,
                        "RON",
                        "Dobanda fixa",
                        false);

        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(LoanAllParameters.class));
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void getLtvByAreaOwnerAndSum_successfullyCase() {
        String expectedUrl = UriComponentsBuilder.fromHttpUrl(URL + "/v1/ltv")
                .queryParam("amount", 1000d)
                .queryParam("isOwner", false)
                .queryParam("financingZone", 1)
                .queryParam("idLoan", 1)
                .encode()
                .toUriString();

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(Integer.class)))
                .thenReturn(new ResponseEntity<>(1, HttpStatus.OK));

        Integer actualResponse = clientRetrieve.getLtvByAreaOwnerAndSum(
                1000d,
                false,
                1,
                1);

        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(Integer.class));
        assertThat(actualResponse).isEqualTo(1);
    }

    @Test
    public void getDistricts_successfullyCase() {
        String expectedUrl = UriComponentsBuilder.fromHttpUrl(URL + "/v1/districts")
                .encode()
                .toUriString();
        ResponseEntity<List<NomenclatureDistrict>> responseMock = mock(ResponseEntity.class);
        List<NomenclatureDistrict> expectedExchangeRates = new ArrayList<>();
        when(responseMock.getBody()).thenReturn(expectedExchangeRates);

        when(restTemplate.exchange(anyString(), any(), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseMock);

        clientRetrieve.getDistricts();

        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    public void getDiscounts_successfullyCase() {
        String expectedUrl = UriComponentsBuilder.fromHttpUrl(URL + "/v1/discounts")
                .queryParam("idLoan", 1)
                .encode()
                .toUriString();

        ResponseEntity<List<Discount>> responseMock = mock(ResponseEntity.class);
        List<Discount> expectedExchangeRates = new ArrayList<>();
        when(responseMock.getBody()).thenReturn(expectedExchangeRates);

        when(restTemplate.exchange(anyString(), any(), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseMock);

        clientRetrieve.getDiscounts(1);

        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    public void getAllLoanInterestRatesByLoanProduct_successfullyCase() {
        String expectedUrl = UriComponentsBuilder.fromHttpUrl(URL + "/v1/get-loan-interest-rates/")
                .queryParam("fkLoanProduct", 1)
                .queryParam("isDigital", false)
                .queryParam("ourClient", false)
                .encode()
                .toUriString();
        ResponseEntity<List<LoanInterestRate>> responseMock = mock(ResponseEntity.class);
        List<LoanInterestRate> expectedExchangeRates = new ArrayList<>();
        when(responseMock.getBody()).thenReturn(expectedExchangeRates);

        when(restTemplate.exchange(anyString(), any(), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseMock);

        clientRetrieve.getAllLoanInterestRatesByLoanProduct(1, false, false);

        verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class));
    }


    private LoanAllParameters getLoanAllParameters() {
        return LoanAllParameters.builder().build();
    }

    @Test
    public void getExchangeRatesTest_throwException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Something went wrong"));

        assertThatCode(() -> clientRetrieve.getLoanProduct("test"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Unexpected error occurred during call rbro-loan-calculator-admin-service - path: /v1/product");

        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LoanProduct.class));
    }
}
