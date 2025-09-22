package uk.gov.companieshouse.requestrefund.consumer.service;

import org.springframework.stereotype.Component;

import uk.gov.companieshouse.payments.RefundRequest;
import uk.gov.companieshouse.requestrefund.consumer.apiclient.PaymentRefundApiClient;

@Component
public class RefundRequestServiceRouter {

    PaymentRefundApiClient paymentRefundApiClient;

    public RefundRequestServiceRouter(PaymentRefundApiClient paymentRefundApiClient) {
        this.paymentRefundApiClient = paymentRefundApiClient;
    }

    public void route(RefundRequest json) {
        paymentRefundApiClient.createPaymentRefundRequest(json);
    }
}
