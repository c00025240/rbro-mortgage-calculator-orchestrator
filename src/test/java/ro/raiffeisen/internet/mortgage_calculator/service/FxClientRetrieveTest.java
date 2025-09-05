package ro.raiffeisen.internet.mortgage_calculator.service;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ro.raiffeisen.internet.mortgage_calculator.exception.InternalServerException;
import ro.raiffeisen.internet.mortgage_calculator.model.client.ExchangeRate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FxClientRetrieveTest {
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final FxClientRetrieve clientRetrieve = new FxClientRetrieve(restTemplate);

    @Test
    public void getExchangeRatesTest() {
        clientRetrieve.setUrl("http://localhost:8090/rice/fx/rates");

        ResponseEntity<List<ExchangeRate>> responseMock = mock(ResponseEntity.class);
        List<ExchangeRate> expectedExchangeRates = new ArrayList<>();
        when(responseMock.getBody()).thenReturn(expectedExchangeRates);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseMock);

        List<ExchangeRate> actualExchangeRates = clientRetrieve.getExchangeRates("EUR");

        Assertions.assertEquals(expectedExchangeRates, actualExchangeRates);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));

    }

    @Test
    public void getExchangeRatesTest_throwException() {
        clientRetrieve.setUrl("http://localhost:8090/rice/fx/rates");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Something went wrong"));

        assertThatCode(() -> clientRetrieve.getExchangeRates("EUR"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Unexpected error occurred during call fx-rates");

        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }
}
