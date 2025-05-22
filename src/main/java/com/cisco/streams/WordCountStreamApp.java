package com.cisco.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Properties;

public class WordCountStreamApp {

    public static void main(String[] args) {
        final String BOOTSTRAP_SERVERS = "localhost:9092";
        // final String BOOTSTRAP_SERVERS = "broker-1:29092";
        final String APPLICATION_ID = "word-count-application";
        final String INPUT_TOPIC = "cisco.wordcount.input";
        final String OUTPUT_TOPIC = "cisco.wordcount.output";

        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        KStream<String,String> wordCountInputStream=streamsBuilder.stream(INPUT_TOPIC);


        // Example: Input line - "Twinkle Twinkle Little Star"
        KTable<String, Long> wordCountOutput = wordCountInputStream
                // Map the text to lower case: "twinkle twinkle little star"
                .mapValues(textLine -> textLine.toLowerCase())
                // Split the text into words using space: ["twinkle", "twinkle", "little", "star"]
                .flatMapValues(textLine -> Arrays.asList(textLine.split(" ")))
                // Select a Key - each word becomes the key: ("twinkle", "twinkle", "little", "star")
                .selectKey((nullKey, word) -> word)
                // Group by key - group words by their value: {twinkle=[twinkle, twinkle], little=[little], star=[star]}
                .groupByKey()
                // Count the number of words: {twinkle=2, little=1, star=1}
                .count();


        wordCountOutput.toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        // Create a Kafka Streams instance
        KafkaStreams streams=new KafkaStreams(streamsBuilder.build(), properties);
        // Start the Kafka Streams application
        streams.start();

        //Print the topology
        System.out.println(streams.toString());

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
