package uk.gov.companieshouse.requestrefund.consumer.apiclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.payment.PrivatePaymentResourceHandler;
import uk.gov.companieshouse.api.handler.payment.request.PaymentRefundRequest;
import uk.gov.companieshouse.api.payment.RequestBodyPost;
import uk.gov.companieshouse.payments.RefundRequest;

class PaymentRefundApiClientTest {

    private Supplier<InternalApiClient> internalApiClientFactory;
    private InternalApiClient internalApiClient;
    private ResponseHandler responseHandler;
    private PaymentRefundApiClient paymentRefundApiClient;
    private RefundRequest refundRequest;
    private PrivatePaymentResourceHandler privatePaymentHandler;
    private PaymentRefundRequest refundsRequestHandler;

    @BeforeEach
    void setUp() {
        internalApiClientFactory = mock(Supplier.class);
        internalApiClient = mock(InternalApiClient.class);
        responseHandler = mock(ResponseHandler.class);
        refundRequest = mock(RefundRequest.class);
        privatePaymentHandler = mock(PrivatePaymentResourceHandler.class);
        refundsRequestHandler = mock(PaymentRefundRequest.class);

        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentHandler);
        when(privatePaymentHandler.createRefundsRequest(anyString(), any(RequestBodyPost.class)))
                .thenReturn(refundsRequestHandler);

        paymentRefundApiClient = new PaymentRefundApiClient(internalApiClientFactory, responseHandler);
    }

    @Test
    void createPaymentRefundRequest_successful() throws Exception {
        when(refundRequest.getRefundAmount()).thenReturn("1.32");
        when((refundRequest.getPaymentId())).thenReturn("pay1234");
        when(refundRequest.getRefundReference()).thenReturn("REF123");

        paymentRefundApiClient.createPaymentRefundRequest(refundRequest);

        ArgumentCaptor<RequestBodyPost> captor = ArgumentCaptor.forClass(RequestBodyPost.class);
        verify(privatePaymentHandler).createRefundsRequest(eq("/payments/pay1234/refunds"), captor.capture());
        RequestBodyPost bodyPost = captor.getValue();
        assertEquals(132, bodyPost.getAmount());
        assertEquals("REF123", bodyPost.getRefundReference());
        verifyNoInteractions(responseHandler);
    }

    @Test
    void createPaymentRefundRequest_handlesURIValidationException() throws Exception {
        when(refundRequest.getRefundAmount()).thenReturn("3.00");
        when(refundRequest.getRefundReference()).thenReturn("REF789");
        doThrow(new URIValidationException("invalid URI")).when(refundsRequestHandler).execute();

        paymentRefundApiClient.createPaymentRefundRequest(refundRequest);

        verify(responseHandler).handle(any(URIValidationException.class));
    }

    @Test
    void convertDecimalAmountToPennies_validAmount() {
        RefundRequest req = mock(RefundRequest.class);
        when(req.getRefundAmount()).thenReturn("12.34");
        // Use reflection to access private method
        int amount = invokeConvertDecimalAmountToPennies(paymentRefundApiClient, req);
        assertEquals(1234, amount);
    }

    @Test
    void convertDecimalAmountToPennies_nullAmount_throwsNullPointerException() {
        RefundRequest req = mock(RefundRequest.class);
        when(req.getRefundAmount()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> invokeConvertDecimalAmountToPennies(paymentRefundApiClient, req));
    }

    @Test
    void convertDecimalAmountToPennies_invalidAmount_throwsNumberFormatException() {
        RefundRequest req = mock(RefundRequest.class);
        when(req.getRefundAmount()).thenReturn("abc");
        assertThrows(NumberFormatException.class, () -> invokeConvertDecimalAmountToPennies(paymentRefundApiClient, req));
    }

    // Helper to invoke private method
    private int invokeConvertDecimalAmountToPennies(PaymentRefundApiClient client, RefundRequest req) {
        try {
            var method = PaymentRefundApiClient.class.getDeclaredMethod("convertDecimalAmountToPennies", RefundRequest.class);
            method.setAccessible(true);
            return (int) method.invoke(client, req);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
            throw new RuntimeException(e);
        }
    }

    // Mocks for handler classes
    interface PrivatePaymentHandler {
        RefundsRequestHandler createRefundsRequest(String uri, RequestBodyPost bodyPost);
    }

    interface RefundsRequestHandler {
        void execute() throws ApiErrorResponseException, URIValidationException;
    }
}