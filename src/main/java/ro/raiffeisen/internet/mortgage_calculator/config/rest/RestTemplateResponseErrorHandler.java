package ro.raiffeisen.internet.mortgage_calculator.config.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
@ComponentScan
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
        if (statusCode == null) {
            throw new UnknownHttpStatusCodeException(
                    response.getStatusCode().value(),
                    response.getStatusText(),
                    response.getHeaders(),
                    getResponseBody(response),
                    getCharset(response));
        }
        handleError(response, statusCode);
    }


    private void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
        String statusText = response.getStatusText();
        HttpHeaders headers = response.getHeaders();
        byte[] body = getResponseBody(response);
        Charset charset = getCharset(response);

        switch (statusCode.series()) {
            case CLIENT_ERROR -> throw new HttpClientErrorException(statusCode, statusText, headers, body, charset);
            case SERVER_ERROR -> throw new HttpServerErrorException(statusCode, statusText, headers, body, charset);
            default -> throw new UnknownHttpStatusCodeException(statusCode.value(), statusText, headers, body, charset);
        }
    }

    protected byte[] getResponseBody(ClientHttpResponse response) {
        try {
            return FileCopyUtils.copyToByteArray(response.getBody());
        } catch (IOException ex) {
            // do  nothing - ignore
        }
        return new byte[0];
    }

    protected Charset getCharset(ClientHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        MediaType contentType = headers.getContentType();
        return (contentType != null ? contentType.getCharset() : null);
    }
}
