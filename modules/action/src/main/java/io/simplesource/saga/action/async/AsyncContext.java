package io.simplesource.saga.action.async;
import io.simplesource.saga.model.specs.ActionProcessorSpec;
import io.simplesource.saga.shared.topics.TopicNamer;
import lombok.Value;


/**
  * @param <A> - common representation form for all action commands (typically Json / or GenericRecord for Avro)
  * @param <I> - input to async function
  * @param <K> - key for the output topic
  * @param <O> - output returned by async function
  * @param <R> - final result type that ends up in output topic
  */


@Value
public final class AsyncContext<A, I, K, O, R> {
    public final ActionProcessorSpec<A> actionSpec;
    public final TopicNamer actionTopicNamer;
    public final AsyncSpec<A, I, K, O, R> asyncSpec;
}

//
//final case class AsyncContext[A, I, K, O, R](actionSpec: ActionProcessorSpec[A],
//                                             actionTopicNamer: TopicNamer,
//                                             asyncSpec: AsyncSpec[A, I, K, O, R]) {
//  val outputSerdes = asyncSpec.outputSpec.map(_.serdes)
//  val actionSerdes = actionSpec.serdes
//}
