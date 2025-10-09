package uk.gov.companieshouse.requestrefund.consumer.serdes;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import payments.refund_request;
import uk.gov.companieshouse.requestrefund .consumer.exception.InvalidPayloadException;

class RefundRequestDeserialiserTest {

    @Test
    void testShouldSuccessfullyDeserialiseRefundRequest() throws IOException {
        // given
        refund_request refundRequest = new refund_request();
        refundRequest.setAttempt(0);
        refundRequest.setPaymentId("qwerty");
        refundRequest.setRefundAmount("1.32");
        refundRequest.setRefundReference("INVALID_TOPIC");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<refund_request> writer = new ReflectDatumWriter<>(refund_request.class);
        writer.write(refundRequest, encoder);
        try (RefundRequestDeserialiser deserialiser = new RefundRequestDeserialiser()) {
      // when
      refund_request actual = deserialiser.deserialize("topic", outputStream.toByteArray());

      // then
      assertThat(actual, is(equalTo(refundRequest)));
    }
    }

    @Test
    void testDeserialiseDataThrowsInvalidPayloadExceptionIfIOExceptionEncountered() throws IOException {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new SpecificDatumWriter<>(String.class);
        writer.write("hello", encoder);
        try (RefundRequestDeserialiser deserialiser = new RefundRequestDeserialiser()) {
      // when
      Executable actual = () -> deserialiser.deserialize("topic", outputStream.toByteArray());

      // then
      InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
      // Note the '\n' is the length prefix of the invalid data sent to the deserialiser
      assertThat(exception.getMessage(), is(equalTo("Invalid payload: [\nhello]")));
      assertThat(exception.getCause(), is(CoreMatchers.instanceOf(IOException.class)));
    }
    }
}
