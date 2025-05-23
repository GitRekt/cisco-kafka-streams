package com.cisco.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.json.JSONObject;

import java.util.Map;
import java.util.Properties;

public class OrderBranchingStream {

    public static void main(String[] args) {
        final String BOOTSTRAP_SERVERS = "localhost:9092";
        // final String BOOTSTRAP_SERVERS = "broker-1:29092";
        final String APPLICATION_ID = "order-branching-application";
        final String INPUT_TOPIC = "cisco.orders";
        final String OUTPUT_HIGH_TOPIC = "cisco.orders.high";
        final String OUTPUT_LOW_TOPIC = "cisco.orders.low";

        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> orderStream = builder.stream(INPUT_TOPIC);

        //Spilt into two branches: high-value and low-value orders

        Map<String,KStream<String,String>> branches=
                orderStream
                        .split(Named.as("order-"))
                        .branch((orderKey, orderValue) -> {
                            JSONObject order = new JSONObject(orderValue);
                            return order.optInt("price", 0) > 1000;
                        },Branched.as("high"))
                        .defaultBranch(Branched.as("low"));

        KStream<String,String> highValueOrders=branches.get("order-high");
        KStream<String,String> lowValueOrders=branches.get("order-low");

        highValueOrders.to(OUTPUT_HIGH_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
        lowValueOrders.to(OUTPUT_LOW_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        // Create a Kafka Streams instance
        KafkaStreams streams = new KafkaStreams(builder.build(), properties);
        // Start the Kafka Streams application
        streams.start();
        // Print the topology
        System.out.println(streams.toString());
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));





    }
}
