package uk.gov.companieshouse.requestrefund.consumer.service;

import org.springframework.stereotype.Component;

import payments.refund_request;
import uk.gov.companieshouse.requestrefund.consumer.apiclient.PaymentsApiClient;

@Component
public class RefundRequestServiceRouter {

    PaymentsApiClient paymentsApiClient;

    public RefundRequestServiceRouter(PaymentsApiClient paymentsApiClient) {
            this.paymentsApiClient = paymentsApiClient;
    }

    public void route(refund_request refundRequest) {
        paymentsApiClient.createPaymentRefundRequest(refundRequest);
    }
}
