package uk.gov.companieshouse.requestrefund.consumer.kafka;

import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_MESSAGE;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_PARTITION;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_TOPIC;
import static uk.gov.companieshouse.requestrefund.consumer.Application.NAMESPACE;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.requestrefund.consumer.logging.DataMapHolder;

public class InvalidMessageRouter implements ProducerInterceptor<String, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private MessageFlags messageFlags;
    private String invalidTopic;

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> producerRecord) {
        if (messageFlags.isRetryable()) {
            messageFlags.destroy();
            return producerRecord;
        } else {

            BigInteger OFFSET_UNAVAILABLE = BigInteger.valueOf(-1);
            String UNKNOWN_ERROR = "unknown";
            Headers headers = producerRecord.headers();

            String originalTopic = getRequiredHeader(headers, ORIGINAL_TOPIC)
                                        .map(header -> new String(header.value()))
                                        .orElse(producerRecord.topic());

            BigInteger partition = getRequiredHeader(headers, ORIGINAL_PARTITION)
                                        .map(header -> new BigInteger(header.value()))
                                        .orElse(OFFSET_UNAVAILABLE);

            BigInteger offset = getRequiredHeader(headers, ORIGINAL_OFFSET)
                                        .map(header -> new BigInteger(header.value()))
                                        .orElse(OFFSET_UNAVAILABLE);

            String exception = getRequiredHeader(headers, EXCEPTION_MESSAGE)
                                        .map(header -> new String(header.value()))
                                        .orElse(UNKNOWN_ERROR);

            LOGGER.error("""
                    Republishing record to topic: [%s] \
                    From: original topic: [%s], partition: [%s], offset: [%s], exception: [%s]\
                    """.formatted(invalidTopic, originalTopic, partition, offset, exception),
                    DataMapHolder.getLogMap());

            return new ProducerRecord<>(invalidTopic, producerRecord.key(), producerRecord.value());
        }
    }

    private Optional<Header> getRequiredHeader(Headers headers, String headerKey) {
        return Optional.ofNullable(headers.lastHeader(headerKey));
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // intentionally left blank

    }

    @Override
    public void close() {
        // intentionally left blank
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.messageFlags = (MessageFlags) configs.get("message-flags");
        this.invalidTopic = (String) configs.get("invalid-topic");
    }
}
