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

    private static final String POST_REQUEST_URI = "/payments/%s/refund";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    public PaymentRefundApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public void createPaymentRefundRequest(RefundRequest refundRequest) {
        InternalApiClient client = internalApiClientFactory.get();

        RequestBodyPost bodyPost = new RequestBodyPost();
        bodyPost.setAmount(refundRequest.getRefundAmount());
        bodyPost.setRefundReference(refundRequest.getRefundReference());

        try {
            client.privatePayment().createRefundsRequest(POST_REQUEST_URI.formatted(refundRequest.getRefundReference()), bodyPost).execute();

        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(ex);
        }
    }
}
