package com.cisco.streams;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class WikimediaBotOrNotBotStream {

    public static void main(String[] args) {
        Logger logger= LoggerFactory.getLogger(WikimediaBotOrNotBotStream.class);

        final String BOOTSTRAP_SERVERS = "localhost:9092";
        // final String BOOTSTRAP_SERVERS = "broker-1:29092";
        final String INPUT_TOPIC_NAME = "cisco.wikimedia.message";
        final String OUTPUT_TOPIC_NAME = "cisco.wikimedia.statistics";
        final String BOT_COUNT_STORE= "bot-count-store";
        final String APPLICATION_ID="wikimedia-bot-or-not-bot-stream";

        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        StreamsBuilder streamsBuilder = new StreamsBuilder();
        // Create a stream from the input topic
        KStream<String, String> inputStream = streamsBuilder.stream(INPUT_TOPIC_NAME);

        // Process the stream to count the number of bot and non-bot edits
        // Create a KTable to count the number of bot and non-bot edits from the input stream
        KTable<String, Long> intermediateTable = inputStream
                // Map the values of the input stream to classify each message as "bot", "not bot", or "error"
                .mapValues(json -> {
                    try {
                        // Parse the JSON string into a JSONObject
                        JSONObject jsonObject = new JSONObject(json);
                        // Check if the "bot" field is true and return "bot" if true, otherwise return "not bot"
                        if (jsonObject.optBoolean("bot")) {
                            return "bot";
                        } else {
                            return "not bot";
                        }
                    } catch (Exception e) {
                        // Log an error if JSON parsing fails and return "error"
                        logger.error("Error parsing JSON: " + e.getMessage());
                        return "error";
                    }
                })
                // Group the stream by the classification ("bot", "not bot", or "error")
                .groupBy((nullKey, botOrNotBot) -> botOrNotBot)
                // Count the occurrences of each classification and materialize the results into a state store
                .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(BOT_COUNT_STORE)
                        // Specify the key and value serdes for the state store
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long())
                );

        intermediateTable
                .toStream()
                .mapValues((key,value)->{
                    JSONObject result=new JSONObject();
                    result.put(key,value);
                    logger.info("Key: " + key + ", Value: " + value);
                    return result.toString();
                })
                .to(OUTPUT_TOPIC_NAME);


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
