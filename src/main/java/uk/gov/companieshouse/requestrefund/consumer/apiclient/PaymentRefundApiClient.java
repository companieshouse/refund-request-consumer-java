package uk.gov.companieshouse.requestrefund.consumer.apiclient;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.payment.RequestBodyPost;
import uk.gov.companieshouse.payments.RefundRequest;

@Component
public class PaymentRefundApiClient {

    private static final String POST_REQUEST_URI = "/payments/%s/refunds";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    public PaymentRefundApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public void createPaymentRefundRequest(RefundRequest refundRequest) {
        InternalApiClient client = internalApiClientFactory.get();

        RequestBodyPost bodyPost = new RequestBodyPost();

        int amount = convertDecimalAmountToPennies(refundRequest);

        bodyPost.setAmount(amount);

        bodyPost.setRefundReference(refundRequest.getRefundReference());
        try {
            client.privatePayment().createRefundsRequest(POST_REQUEST_URI.formatted(refundRequest.getRefundReference()), bodyPost).execute();

        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(ex);
        }
    }

    private int convertDecimalAmountToPennies(RefundRequest refundRequest) {
        //Converting the refund amount from String to Integer as the payments API expects an Integer value
        //representing the amount in pence.
        //e.g. "1.32" becomes 132
        //If the conversion fails this will throw a NumberFormatException which will be caught by the
        //RetryableException handler in the Kafka listener and the message will be retried.
        //If the amount is null this will throw a NullPointerException which will be caught by  the
        //NonRetryableException handler in the Kafka listener and the message will be discarded.
        if (refundRequest.getRefundAmount() == null) {
            throw new NullPointerException("Refund amount is null");
        }
        String amountString = refundRequest.getRefundAmount().replace(".", "");
        if (!amountString.matches("\\d+")) {
            throw new NumberFormatException("Refund amount is not a valid number");
        }
        int amount = Integer.parseInt(amountString);
        return amount;
    }
}
