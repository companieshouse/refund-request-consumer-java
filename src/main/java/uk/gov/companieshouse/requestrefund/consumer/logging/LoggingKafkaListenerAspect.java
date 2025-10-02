package uk.gov.companieshouse.requestrefund.consumer.logging;

import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;
import static org.springframework.kafka.support.KafkaHeaders.OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_PARTITION;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;
import static uk.gov.companieshouse.requestrefund.consumer.Application.NAMESPACE;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import payments.refund_request;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.requestrefund.consumer.Util;

@Component
@Aspect
class LoggingKafkaListenerAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String LOG_MESSAGE_RECEIVED = "Processing Refund Request";
    private static final String LOG_MESSAGE_PROCESSED = "Processed Refund Request";

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object manageStructuredLogging(ProceedingJoinPoint joinPoint) throws Throwable {

        int retryCount = 0;
        try {
            Message<?> message = (Message<?>) joinPoint.getArgs()[0];
            MessageHeaders headers = message.getHeaders();

            retryCount = Optional.ofNullable(headers.get(DEFAULT_HEADER_ATTEMPTS))
                    .map(attempts -> ByteBuffer.wrap((byte[]) attempts).getInt())
                    .orElse(1) - 1;
            refund_request refundRequest = Util.extractRefundRequest(message.getPayload());

            DataMapHolder.initialise(Optional.ofNullable(refundRequest.getRefundReference())
                    .orElse(UUID.randomUUID().toString()));

            DataMapHolder.get()
                    .retryCount(retryCount)
                    .topic((String) headers.get(RECEIVED_TOPIC))
                    .partition((Integer) headers.get(RECEIVED_PARTITION))
                    .offset((Long) headers.get(OFFSET));

            LOGGER.info( LOG_MESSAGE_RECEIVED, DataMapHolder.getLogMap());

            Object result = joinPoint.proceed();

            LOGGER.info(LOG_MESSAGE_PROCESSED, DataMapHolder.getLogMap());

            return result;
        } catch (Exception ex) {
            LOGGER.error("Exception thrown", ex, DataMapHolder.getLogMap());
            throw ex;
        } finally {
            DataMapHolder.clear();
        }
    }
}