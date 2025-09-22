package uk.gov.companieshouse.requestrefund.consumer;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.payments.RefundRequest;
import uk.gov.companieshouse.requestrefund.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.requestrefund.consumer.logging.DataMapHolder;
import static uk.gov.companieshouse.requestrefund.consumer.Application.NAMESPACE;
import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.springframework.messaging.MessageHeaders;

public class Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    public static RefundRequest extractRefundRequest(Object payload) {
        if (payload instanceof RefundRequest refundRequest) {
            return refundRequest;
        }
        String errorMessage = "Invalid payload type, payload: [%s]".formatted(payload.toString());
        LOGGER.error(errorMessage, DataMapHolder.getLogMap());
        throw new NonRetryableException(errorMessage);
    }

    public static int getRetryCount(MessageHeaders headers) {
        return Optional.ofNullable(headers.get(DEFAULT_HEADER_ATTEMPTS))
                .map(attempts -> ByteBuffer.wrap((byte[]) attempts).getInt())
                .orElse(1) - 1;
    }
}
