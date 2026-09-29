package uk.gov.companieshouse.requestrefund.consumer.serdes;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.util.ClassSecurityValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import payments.refund_request;
import uk.gov.companieshouse.requestrefund.consumer.exception.NonRetryableException;

@ExtendWith(MockitoExtension.class)
class RefundRequestSerialiserTest {

    @Mock
    private DatumWriter<refund_request> writer;

    @BeforeAll
    static void setupAvroSecurity() {
        // Avro 1.12+ requires explicit class trust for serialization.
        // This accepts all classes. Long term, we could consider using the schema registry to avoid
        // runtime class validation entirely. However, that would make integration testing more complex.
        ClassSecurityValidator.setGlobal((clazz -> true));
    }

    @Test
    void testSerialiseRefundRequest() {
        // given
        refund_request refundRequest = new refund_request();
        refundRequest.setAttempt(0);
        refundRequest.setPaymentId("qwerty");
        refundRequest.setRefundAmount("1.32");
        refundRequest.setRefundReference("INVALID_TOPIC");

        try (RefundRequestSerializer serialiser = new RefundRequestSerializer()) {
            // when
            byte[] actual = serialiser.serialize("topic", refundRequest);

            // then
            assertThat(actual, is(notNullValue()));
        }
    }

    @Test
    void testThrowNonRetryableExceptionIfIOExceptionThrown() throws IOException {
        // given
        refund_request refundRequest = new refund_request();
        refundRequest.setAttempt(0);
        refundRequest.setPaymentId("qwerty");
        refundRequest.setRefundAmount("1.32");
        refundRequest.setRefundReference("INVALID_TOPIC");

        RefundRequestSerializer serialiser = spy(new RefundRequestSerializer());
        when(serialiser.getDatumWriter()).thenReturn(writer);
        doThrow(IOException.class).when(writer).write(any(), any());

        // when
        Executable actual = () -> serialiser.serialize("topic", refundRequest);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, actual);
        assertThat(exception.getMessage(), is(equalTo("Error serialising refund request")));
        assertThat(exception.getCause(), is(instanceOf(IOException.class)));
    }
}
