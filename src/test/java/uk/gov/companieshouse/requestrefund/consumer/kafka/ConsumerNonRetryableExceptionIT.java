package uk.gov.companieshouse.requestrefund.consumer.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.requestrefund.consumer.kafka.KafkaUtils.ERROR_TOPIC;
import static uk.gov.companieshouse.requestrefund.consumer.kafka.KafkaUtils.INVALID_TOPIC;
import static uk.gov.companieshouse.requestrefund.consumer.kafka.KafkaUtils.MAIN_TOPIC;
import static uk.gov.companieshouse.requestrefund.consumer.kafka.KafkaUtils.RETRY_TOPIC;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import payments.refund_request;
import uk.gov.companieshouse.requestrefund.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.requestrefund.consumer.service.RefundRequestServiceRouter;

@SpringBootTest
class ConsumerNonRetryableExceptionIT extends AbstractKafkaIT {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    private KafkaProducer<String, byte[]> testProducer;

    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @MockitoBean
    private RefundRequestServiceRouter refundRequestServiceRouter;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void drainKafkaTopics() {
        if (!kafka.isRunning()) {
            throw new IllegalStateException("Kafka container is not running!");
        }
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void testRepublishToRefundRequestInvalidMessageTopicIfNonRetryableExceptionThrown() throws Exception {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<refund_request> writer = new ReflectDatumWriter<>(refund_request.class);

        refund_request refundRequest = new refund_request();
        refundRequest.setAttempt(0);
        refundRequest.setPaymentId("qwerty");
        refundRequest.setRefundAmount("1.32");
        refundRequest.setRefundReference("INVALID_TOPIC");
        writer.write(refundRequest, encoder);
        doThrow(NonRetryableException.class).when(refundRequestServiceRouter).route(any());

        // when
        testProducer.send(new ProducerRecord<>(MAIN_TOPIC, 0, System.currentTimeMillis(),"key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        // then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, MAIN_TOPIC)).isOne();
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, RETRY_TOPIC)).isZero();
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, ERROR_TOPIC)).isZero();
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, INVALID_TOPIC)).isOne();
        verify(refundRequestServiceRouter).route(any());
    }
}
