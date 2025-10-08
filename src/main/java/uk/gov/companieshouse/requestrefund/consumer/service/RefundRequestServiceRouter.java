package uk.gov.companieshouse.requestrefund.consumer.service;

import org.springframework.stereotype.Component;

import payments.refund_request;
import uk.gov.companieshouse.requestrefund.consumer.apiclient.PaymentRefundApiClient;

@Component
public class RefundRequestServiceRouter {

    PaymentRefundApiClient paymentRefundApiClient;

    public RefundRequestServiceRouter(PaymentRefundApiClient paymentRefundApiClient) {
            this.paymentRefundApiClient = paymentRefundApiClient;
    }

    public void route(refund_request refundRequest) {
        paymentRefundApiClient.createPaymentRefundRequest(refundRequest);
    }
}
