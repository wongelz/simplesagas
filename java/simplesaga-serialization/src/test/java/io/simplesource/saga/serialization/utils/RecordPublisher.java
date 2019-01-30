package io.simplesource.saga.serialization.utils;

import org.apache.avro.specific.SpecificRecord;

public interface RecordPublisher<K extends SpecificRecord, V extends SpecificRecord> {
    void publish(K key, V value);
}
