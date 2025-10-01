package uk.gov.companieshouse.requestrefund.consumer.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.common.serialization.Serializer;

import payments.refund_request;
import uk.gov.companieshouse.requestrefund.consumer.exception.NonRetryableException;

public class RefundRequestSerializer implements Serializer<refund_request> {

    @Override
    public byte[] serialize(String topic, refund_request data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<refund_request> writer = getDatumWriter();
        try {
            writer.write(data, encoder);
        } catch (IOException ex) {
            throw new NonRetryableException("Error serialising refund request", ex);
        }
        return outputStream.toByteArray();
    }

    public DatumWriter<refund_request> getDatumWriter() {
        return new ReflectDatumWriter<>(refund_request.class);
    }
}
