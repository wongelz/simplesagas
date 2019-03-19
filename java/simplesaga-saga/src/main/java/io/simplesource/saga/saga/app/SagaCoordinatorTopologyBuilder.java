package io.simplesource.saga.saga.app;

import io.simplesource.saga.model.messages.SagaRequest;
import io.simplesource.saga.model.messages.SagaResponse;
import io.simplesource.saga.model.messages.SagaStateTransition;
import io.simplesource.saga.model.saga.Saga;
import io.simplesource.saga.model.specs.SagaSpec;
import io.simplesource.saga.shared.topics.TopicConfig;
import io.simplesource.saga.shared.topics.TopicNamer;
import io.simplesource.saga.shared.topics.TopicTypes;
import lombok.Value;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SagaCoordinatorTopologyBuilder<A> {

    private final SagaSpec<A> sagaSpec;
    private final TopicConfig sagaTopicConfig;
    private final List<Consumer<SagaTopologyContext<A>>> onBuildConsumers = new ArrayList<>();

    @Value
    public static final class SagaTopologyContext<A> {
        public final StreamsBuilder builder;
        public final KStream<UUID, SagaRequest<A>> sagaRequest;
        public final KStream<UUID, Saga<A>> sagaState;
        public final KStream<UUID, SagaStateTransition> sagaStateTransition;
    }

    public SagaCoordinatorTopologyBuilder(SagaSpec<A> sagaSpec, TopicConfig sagaTopicConfig) {
        this.sagaSpec = sagaSpec;
        this.sagaTopicConfig = sagaTopicConfig;
    }

    /**
     * Register a consumer to be called when the topology is built, ie. to allow sub-topologies to be added.
     * @param consumer to register.
     */
    public void onBuildTopology(Consumer<SagaTopologyContext<A>> consumer) {
        onBuildConsumers.add(consumer);
    }

    public Topology build() {
        StreamsBuilder builder = new StreamsBuilder();
        // get input topic streams
        TopicNamer topicNamer = sagaTopicConfig.namer;
        KStream<UUID, SagaRequest<A>> sagaRequest = SagaConsumer.sagaRequest(sagaSpec, topicNamer, builder);
        KStream<UUID, Saga<A>> sagaState = SagaConsumer.state(sagaSpec, topicNamer, builder);
        KStream<UUID, SagaStateTransition> sagaStateTransition = SagaConsumer.stateTransition(sagaSpec, topicNamer, builder);
        SagaTopologyContext<A> topologyContext = new SagaTopologyContext<>(builder, sagaRequest, sagaState, sagaStateTransition);
        onBuildConsumers.forEach(p -> p.accept(topologyContext));

        DistributorContext<SagaResponse> distCtx = new DistributorContext<>(
                new DistributorSerdes<>(sagaSpec.serdes().uuid(), sagaSpec.serdes().response()),
                sagaTopicConfig.namer.apply(TopicTypes.SagaTopic.responseTopicMap),
                sagaSpec.responseWindow,
                response -> response.sagaId);

        KStream<UUID, String> topicNames = ResultDistributor.resultTopicMapStream(distCtx, builder);
        KStream<UUID, SagaResponse> sagaResponse = SagaConsumer.sagaResponse(sagaSpec, topicNamer, builder);
        ResultDistributor.distribute(distCtx, sagaResponse, topicNames);
        return builder.build();
    }
}
