package io.simplesource.saga.serialization.avro;

import io.simplesource.data.Result;
import io.simplesource.data.Sequence;
import io.simplesource.saga.model.messages.SagaRequest;
import io.simplesource.saga.model.messages.SagaResponse;
import io.simplesource.saga.model.saga.Saga;
import io.simplesource.saga.model.saga.SagaError;
import io.simplesource.saga.model.serdes.SagaClientSerdes;
import io.simplesource.saga.model.serdes.SagaSerdes;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SagaClientSerdesTest {

    private static String SCHEMA_URL = "http://localhost:8081/";
    private static String FAKE_TOPIC = "topic";

    @Test
    void uuidTest() {
        SagaClientSerdes<?> serdes = AvroSerdes.sagaClientSerdes(SCHEMA_URL, true);
        UUID original = UUID.randomUUID();
        byte[] serialized = serdes.uuid().serializer().serialize(FAKE_TOPIC, original);
        UUID deserialized = serdes.uuid().deserializer().deserialize(FAKE_TOPIC, serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void responseTestSuccess() {
        SagaClientSerdes<?> serdes = AvroSerdes.sagaClientSerdes(SCHEMA_URL, true);
        SagaResponse original = new SagaResponse(UUID.randomUUID(), Result.success(Sequence.first().next().next()));
        byte[] serialized = serdes.response().serializer().serialize(FAKE_TOPIC, original);
        SagaResponse deserialized = serdes.response().deserializer().deserialize(FAKE_TOPIC, serialized);
        assertThat(deserialized.toString()).isEqualTo(original.toString());
        assertThat(deserialized).isEqualToIgnoringGivenFields(original, "result");
        assertThat(deserialized.result.isSuccess()).isTrue();
    }

    @Test
    void responseTestFailure() {
        SagaClientSerdes<?> serdes = AvroSerdes.sagaClientSerdes(SCHEMA_URL, true);
        SagaError sagaError1 = SagaError.of(SagaError.Reason.InternalError, "There was an error");
        SagaError sagaError2 = SagaError.of(SagaError.Reason.CommandError, "Invalid command");
        SagaResponse original = new SagaResponse(UUID.randomUUID(), Result.failure(sagaError1, sagaError2));
        byte[] serialized = serdes.response().serializer().serialize(FAKE_TOPIC, original);
        SagaResponse deserialized = serdes.response().deserializer().deserialize(FAKE_TOPIC, serialized);
        assertThat(deserialized).isEqualToIgnoringGivenFields(original, "result");
        assertThat(deserialized.result.isFailure()).isTrue();
        deserialized.result.failureReasons().ifPresent(nel -> {
            List<SagaError> el = nel.toList();
            assertThat(el).hasSize(2);
            assertThat(el.get(0)).isEqualToComparingFieldByField(sagaError1);
            assertThat(el.get(1)).isEqualToComparingFieldByField(sagaError2);
        });
    }

    @Test
    void sagaRequestTest() {
        SagaClientSerdes<GenericRecord> serdes = AvroSerdes.sagaClientSerdes(SCHEMA_URL, true);

        Saga<GenericRecord> saga = SagaTestUtils.getTestSaga();

        // SagaRequest<GenericRecord> original = new SagaRequest<>(UUID.randomUUID(), saga);
        SagaRequest<GenericRecord> original = new SagaRequest<>(saga.sagaId(), saga);

        byte[] serialized = serdes.request().serializer().serialize(FAKE_TOPIC, original);
        SagaRequest<GenericRecord> deserialized = serdes.request().deserializer().deserialize(FAKE_TOPIC, serialized);

        String originalAsString = original.toString();

        assertThat(deserialized.toString()).hasSameSizeAs(originalAsString);
    }

}

