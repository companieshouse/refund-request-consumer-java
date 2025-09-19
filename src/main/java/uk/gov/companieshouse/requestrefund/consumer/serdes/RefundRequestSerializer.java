package uk.gov.companieshouse.requestrefund.consumer.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.common.serialization.Serializer;

import uk.gov.companieshouse.payments.RefundRequest;
import uk.gov.companieshouse.requestrefund.consumer.exception.NonRetryableException;

public class RefundRequestSerializer implements Serializer<RefundRequest> {

    @Override
    public byte[] serialize(String topic, RefundRequest data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<RefundRequest> writer = getDatumWriter();
        try {
            writer.write(data, encoder);
        } catch (IOException ex) {
            throw new NonRetryableException("Error serialising refund request", ex);
        }
        return outputStream.toByteArray();
    }

    public DatumWriter<RefundRequest> getDatumWriter() {
        return new ReflectDatumWriter<>(RefundRequest.class);
    }
}
