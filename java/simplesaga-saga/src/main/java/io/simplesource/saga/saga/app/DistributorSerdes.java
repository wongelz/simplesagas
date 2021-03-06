package io.simplesource.saga.saga.app;

import lombok.Value;
import org.apache.kafka.common.serialization.Serde;

import java.util.UUID;

@Value
public final class DistributorSerdes<V> {
    public final Serde<UUID> uuid;
    public final Serde<V> value;

    public DistributorSerdes(Serde<UUID> uuid, Serde<V> value) {
        this.uuid = uuid;
        this.value = value;
    }
}
