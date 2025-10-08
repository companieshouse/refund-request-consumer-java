package uk.gov.companieshouse.requestrefund.consumer.kafka;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
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

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import payments.refund_request;

@SpringBootTest(properties = {
    "api.api-url=http://localhost:8889",
    "consumer.max-attempts=2",
    "consumer.backoff-delay=50"
})
@WireMockTest(httpPort = 8889)
class ConsumerRetryableException500ErrorIT extends AbstractKafkaIT {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;
    @Autowired
    private KafkaProducer<String, byte[]> testProducer;
    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void shouldConsumeRefundRequestAndProcessSuccessfully() throws Exception {

        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<refund_request> writer = new ReflectDatumWriter<>(refund_request.class);
        writer.write(new refund_request(1,"ref1234","1","2"), encoder);

        stubFor(post(urlEqualTo("/payments/ref1234/refunds"))
                .willReturn(aResponse()
                        .withStatus(500)));

        // when
        testProducer.send(new ProducerRecord<>(MAIN_TOPIC, 0, System.currentTimeMillis(),
                "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(2, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        // then
        verify(postRequestedFor(urlEqualTo("/payments/ref1234/refunds")));

        int totalCount = 0;
        long timeout = System.currentTimeMillis() + 2000; // 2 seconds
        while (System.currentTimeMillis() < timeout) {
            ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(200), 1);
            totalCount += KafkaUtils.noOfRecordsForTopic(records, RETRY_TOPIC);
            if (totalCount >= 1) break;
        }
        assertThat(totalCount).isEqualTo(1);
    }
}
