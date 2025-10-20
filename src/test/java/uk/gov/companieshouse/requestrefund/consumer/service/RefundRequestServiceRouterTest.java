package uk.gov.companieshouse.requestrefund.consumer.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import payments.refund_request;
import uk.gov.companieshouse.requestrefund.consumer.apiclient.PaymentsApiClient;

class RefundRequestServiceRouterTest {

    private PaymentsApiClient paymentRefundApiClient;
    private RefundRequestServiceRouter refundRequestServiceRouter;

    @BeforeEach
    void setUp() {
        paymentRefundApiClient = mock(PaymentsApiClient.class);
        refundRequestServiceRouter = new RefundRequestServiceRouter(paymentRefundApiClient);
    }

    @Test
    void route_shouldCallCreatePaymentRefundRequest() {
        refund_request mockRefundRequest = mock(refund_request.class);

        refundRequestServiceRouter.route(mockRefundRequest);

        verify(paymentRefundApiClient, times(1)).createPaymentRefundRequest(mockRefundRequest);
    }
}