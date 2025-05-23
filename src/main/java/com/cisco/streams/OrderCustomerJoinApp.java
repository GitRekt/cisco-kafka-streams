package com.cisco.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.json.JSONObject;

import java.util.Properties;

public class OrderCustomerJoinApp {

    public static void main(String[] args) {

        final String BOOTSTRAP_SERVERS = "localhost:9092";
        // final String BOOTSTRAP_SERVERS = "broker-1:29092";
        final String APPLICATION_ID = "order-customer-join-application";
        final String INPUT_ORDERS_TOPIC = "cisco.ecommerce.orders";
        final String INPUT_CUSTOMERS_TOPIC = "cisco.ecommerce.customers";
        final String OUTPUT_TOPIC = "cisco.ecommerce.enriched.orders";

        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder builder=new StreamsBuilder();

        // Create a stream from the orders topic
        KStream<String, String> ordersStream = builder.stream(INPUT_ORDERS_TOPIC);

        //Create a Table for customers
        KTable<String, String> customersTable = builder.table(INPUT_CUSTOMERS_TOPIC);

        // Join the orders stream with the customers table
        KStream<String,String> enrichedOrdersStream = ordersStream
                .selectKey((key,orderJsonString)->{
                    // Extract the customer ID from the order JSON string
                    JSONObject order=new JSONObject(orderJsonString);
                    return order.optString("customerId");
                })
                .join(customersTable,(orderString,customerString)->{
                    JSONObject order=new JSONObject(orderString);
                    JSONObject customer=new JSONObject(customerString);
                    // Enrich the order with customer details
                    order.put("name",customer.optString("name"));
                    return order.toString();
                });

        enrichedOrdersStream.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        // Create a Kafka Streams instance
        KafkaStreams streams=new KafkaStreams(builder.build(), properties);
        // Start the Kafka Streams application
        streams.start();
        // Print the topology
        System.out.println(streams.toString());
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
