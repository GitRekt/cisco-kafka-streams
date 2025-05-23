package com.cisco.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;

public class WindowedCityOrderCountApp {
    public static void main(String[] args) {
        final String BOOTSTRAP_SERVERS = "localhost:9092";
        // final String BOOTSTRAP_SERVERS = "broker-1:29092";
        final String APPLICATION_ID = "windowed-city-order-count-application";
        final String INPUT_TOPIC = "cisco.orders";
        final String OUTPUT_TOPIC = "cisco.city.windowed.count";

        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> orderStream = builder.stream(INPUT_TOPIC);

        KTable<Windowed<String>,Long> cityOrderCount=orderStream
                .map((key,value)->{
                    JSONObject order=new JSONObject(value);
                    String city=order.optString("city","UNKNOWN");
                    return KeyValue.pair(city,order.toString());
                })
                .groupByKey(Grouped.with(Serdes.String(),Serdes.String()))
                .windowedBy((TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(2))))
                .count(Materialized.<String,Long, WindowStore<Bytes,byte[]>>as("windowed-city-order-count-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()));



        cityOrderCount
                .toStream()
                .map((windowedKey,count)->{
                    String city=windowedKey.key();
                    // Format the start time
                    String startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(windowedKey.window().start()));
                    // Format the end time
                    String endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(windowedKey.window().end()));

                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("city",city);
                    jsonObject.put("count",count);
                    jsonObject.put("startTime",startTime);
                    jsonObject.put("endTime",endTime);
                    return KeyValue.pair(city,jsonObject.toString());
                })
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

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
