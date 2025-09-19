package uk.gov.companieshouse.requestrefund.consumer.serdes;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import uk.gov.companieshouse.payments.RefundRequest;
import uk.gov.companieshouse.requestrefund .consumer.exception.InvalidPayloadException;

class ChsDeltaDeserialiserTest {

    @Test
    void testShouldSuccessfullyDeserialiseChsDelta() throws IOException {
        // given
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setAttempt(0);
        refundRequest.setPaymentId("qwerty");
        refundRequest.setRefundAmount("1.32");
        refundRequest.setRefundReference("INVALID_TOPIC");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<RefundRequest> writer = new ReflectDatumWriter<>(RefundRequest.class);
        writer.write(refundRequest, encoder);
        RefundRequestDeserialiser deserialiser = new RefundRequestDeserialiser();

        // when
        RefundRequest actual = deserialiser.deserialize("topic", outputStream.toByteArray());

        // then
        assertThat(actual, is(equalTo(refundRequest)));
    }

    @Test
    void testDeserialiseDataThrowsInvalidPayloadExceptionIfIOExceptionEncountered() throws IOException {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new SpecificDatumWriter<>(String.class);
        writer.write("hello", encoder);
        RefundRequestDeserialiser deserialiser = new RefundRequestDeserialiser();

        // when
        Executable actual = () -> deserialiser.deserialize("topic", outputStream.toByteArray());

        // then
        InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
        // Note the '\n' is the length prefix of the invalid data sent to the deserialiser
        assertThat(exception.getMessage(), is(equalTo("Invalid payload: [\nhello]")));
        assertThat(exception.getCause(), is(CoreMatchers.instanceOf(IOException.class)));
    }
}
