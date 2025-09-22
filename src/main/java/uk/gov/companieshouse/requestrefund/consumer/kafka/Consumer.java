package uk.gov.companieshouse.requestrefund.consumer.kafka;

import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;
import static org.springframework.kafka.support.KafkaHeaders.OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_PARTITION;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;
import static uk.gov.companieshouse.requestrefund.consumer.Application.NAMESPACE;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.payments.RefundRequest;
import uk.gov.companieshouse.requestrefund.consumer.Util;
import uk.gov.companieshouse.requestrefund.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.requestrefund.consumer.exception.RetryableException;
import uk.gov.companieshouse.requestrefund.consumer.logging.DataMapHolder;
import uk.gov.companieshouse.requestrefund.consumer.service.RefundRequestServiceRouter;
@Component
public class Consumer {

    private final RefundRequestServiceRouter router;
    private final MessageFlags messageFlags;
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final int maxAttempts;

    public Consumer(RefundRequestServiceRouter router, MessageFlags messageFlags, @Value("${consumer.max-attempts}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.router = router;
        this.messageFlags = messageFlags;
    }


    @KafkaListener(
            id = "${consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            topics = {"${consumer.topic}"},
            groupId = "${consumer.group-id}")
    public void consume(Message<RefundRequest> message) throws InterruptedException {
        try {
            router.route(message.getPayload());
        } catch (RetryableException ex) {
            messageFlags.setRetryable(true);
            logIfMaxAttemptsReached(message, ex);
            throw ex;
        }
        finally {
         DataMapHolder.clear();
        }
    }

    private void logIfMaxAttemptsReached(Message<RefundRequest> message, RetryableException ex) {
        MessageHeaders headers = message.getHeaders();

        int retryCount = Util.getRetryCount(headers);

        RefundRequest refundRequest = Util.extractRefundRequest(message.getPayload());

        DataMapHolder.initialise(Optional.ofNullable(refundRequest.getRefundReference())
                .orElse(UUID.randomUUID().toString()));

        DataMapHolder.get()
                .retryCount(retryCount)
                .topic((String) headers.get(RECEIVED_TOPIC))
                .partition((Integer) headers.get(RECEIVED_PARTITION))
                .offset((Long) headers.get(OFFSET));

        if (retryCount >= maxAttempts - 1) {
            LOGGER.error("Max retry attempts reached", ex, DataMapHolder.getLogMap());
        }
    }
}
