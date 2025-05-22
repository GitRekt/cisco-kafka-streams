package com.cisco.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.json.JSONObject;

import java.util.Properties;

public class CityWiseOrderAggregator {
    public static void main(String[] args) {
        // Kafka Streams Configuration
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "city-order-aggregation-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        // Read from the input topic
        KStream<String, String> ordersStream = builder.stream("cisco.orders");

        // Process the stream: Parse, aggregate, and produce results
        KTable<String, CityStats> cityStatsTable = ordersStream
                // Parse JSON string to extract fields
                .mapValues(value -> {
                    JSONObject orderJson = new JSONObject(value);
                    String city = orderJson.getString("city");
                    double price = orderJson.getDouble("price");
                    return new Order(city, price); // Convert JSON to Order object
                })
                .selectKey((key, order) -> order.getCity()) // Use city as the key
                .groupByKey(Grouped.with(Serdes.String(), Serdes.serdeFrom(new OrderSerializer(), new OrderDeserializer())))
                .aggregate(
                        // Initialize aggregation
                        () -> new CityStats(0, 0.0),
                        // Aggregate logic
                        (city, order, agg) -> {
                            agg.setOrderCount(agg.getOrderCount() + 1);
                            agg.setTotalAmount(agg.getTotalAmount() + order.getPrice());
                            return agg;
                        },
                        Materialized.with(Serdes.String(), Serdes.serdeFrom(new CityStatsSerializer(), new CityStatsDeserializer()))
                );

        // Write the aggregated results to the output topic
        cityStatsTable
                .toStream()
                .mapValues((key, stats) -> {
                    JSONObject result = new JSONObject();
                    result.put("city", key);
                    result.put("orderCount", stats.getOrderCount());
                    result.put("totalAmount", stats.getTotalAmount());
                    return result.toString(); // Convert CityStats to JSON string
                })
                .to("cisco.city.orders.stats", Produced.with(Serdes.String(), Serdes.String()));

        // Start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    // Helper class to represent an Order
    static class Order {
        private final String city;
        private final double price;

        public Order(String city, double price) {
            this.city = city;
            this.price = price;
        }

        public String getCity() {
            return city;
        }

        public double getPrice() {
            return price;
        }
    }

    // Helper class to represent aggregated city statistics
    static class CityStats {
        private long orderCount;
        private double totalAmount;

        public CityStats(long orderCount, double totalAmount) {
            this.orderCount = orderCount;
            this.totalAmount = totalAmount;
        }

        public long getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(long orderCount) {
            this.orderCount = orderCount;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }
    }
}