package uk.gov.companieshouse.requestrefund.consumer.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

final class KafkaUtils {

    static final String MAIN_TOPIC = "refund-request";
    static final String RETRY_TOPIC = "refund-request-refund-request-consumer-retry";
    static final String ERROR_TOPIC = "refund-request-refund-request-consumer-error";
    static final String INVALID_TOPIC = "refund-request-refund-request-consumer-invalid";

    private KafkaUtils() {
    }

    static int noOfRecordsForTopic(ConsumerRecords<?, ?> records, String topic) {
        int count = 0;
        for (@SuppressWarnings("unused") ConsumerRecord<?, ?> ignored : records.records(topic)) {
            count++;
        }
        return count;
    }
}
