package uk.gov.companieshouse.requestrefund.consumer.apiclient;

import static uk.gov.companieshouse.requestrefund.consumer.Application.NAMESPACE;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.requestrefund.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.requestrefund.consumer.exception.RetryableException;
import uk.gov.companieshouse.requestrefund.consumer.logging.DataMapHolder;

@Component
public class ResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String API_INFO_RESPONSE_MESSAGE = "Call to API failed, status code: %d. %s";
    private static final String API_ERROR_RESPONSE_MESSAGE = "Call to API failed, status code: %d";
    private static final String URI_VALIDATION_EXCEPTION_MESSAGE = "Invalid URI";

    public void handle(ApiErrorResponseException ex) {
        final int statusCode = ex.getStatusCode();
        final HttpStatus httpStatus = HttpStatus.valueOf(ex.getStatusCode());

        if (HttpStatus.CONFLICT.equals(httpStatus) || HttpStatus.BAD_REQUEST.equals(httpStatus)) {
            LOGGER.error(API_ERROR_RESPONSE_MESSAGE.formatted(statusCode), ex, DataMapHolder.getLogMap());
            throw new NonRetryableException(API_ERROR_RESPONSE_MESSAGE.formatted(statusCode), ex);
        } else {
            LOGGER.info(API_INFO_RESPONSE_MESSAGE.formatted(statusCode, Arrays.toString(ex.getStackTrace())), DataMapHolder.getLogMap());
            throw new RetryableException(API_ERROR_RESPONSE_MESSAGE.formatted(statusCode), ex);
        }
    }

    public void handle(URIValidationException ex) {
        LOGGER.error(ex.getMessage());
        LOGGER.error(URI_VALIDATION_EXCEPTION_MESSAGE, DataMapHolder.getLogMap());
        throw new NonRetryableException(URI_VALIDATION_EXCEPTION_MESSAGE, ex);
    }
}
