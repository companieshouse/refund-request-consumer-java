package uk.gov.companieshouse.requestrefund.consumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.payments.RefundRequest;
import uk.gov.companieshouse.requestrefund.consumer.apiclient.PaymentRefundApiClient;
import static org.mockito.Mockito.*;

class RefundRequestServiceRouterTest {

    private PaymentRefundApiClient paymentRefundApiClient;
    private RefundRequestServiceRouter refundRequestServiceRouter;

    @BeforeEach
    void setUp() {
        paymentRefundApiClient = mock(PaymentRefundApiClient.class);
        refundRequestServiceRouter = new RefundRequestServiceRouter(paymentRefundApiClient);
    }

    @Test
    void route_shouldCallCreatePaymentRefundRequest() {
        RefundRequest refundRequest = mock(RefundRequest.class);

        refundRequestServiceRouter.route(refundRequest);

        verify(paymentRefundApiClient, times(1)).createPaymentRefundRequest(refundRequest);
    }
}