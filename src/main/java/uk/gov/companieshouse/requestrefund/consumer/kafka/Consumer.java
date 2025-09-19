package uk.gov.companieshouse.requestrefund.consumer.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import uk.gov.companieshouse.payments.RefundRequest;
import uk.gov.companieshouse.requestrefund.consumer.exception.RetryableException;
import uk.gov.companieshouse.requestrefund.consumer.service.RefundRequestServiceRouter;

@Component
public class Consumer {

    private final RefundRequestServiceRouter router;
    private final MessageFlags messageFlags;

    public Consumer(RefundRequestServiceRouter router, MessageFlags messageFlags) {
        this.router = router;
        this.messageFlags = messageFlags;
    }


    @KafkaListener(
            id = "${consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            topics = {"${consumer.topic}"},
            groupId = "${consumer.group-id}"
    )
    public void consume(Message<RefundRequest> message) throws InterruptedException {
        try {
            router.route(message.getPayload());
        } catch (RetryableException ex) {
            messageFlags.setRetryable(true);
            System.err.println("Retryable exception occurred: " + message.getPayload().toString());
            throw ex;
        }
    }
}
