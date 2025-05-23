package com.cisco.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

public class RealTimeTransactionMonitoring {

    public static void main(String[] args) {

        // Kafka Streams Configuration
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "real-time-transaction-monitoring");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);

        StreamsBuilder builder = new StreamsBuilder();

        // Read from the input topic
        KStream<String, String> transactionStream = builder.stream("raw.transactions");

        // STEP 1: Branch Transactions by Amount
        Map<String, KStream<String, String>> branches = transactionStream.split(Named.as("Branch-"))
                .branch((key, value) -> parseAmount(value) >= 5000, Branched.as("High"))
                .branch((key, value) -> parseAmount(value) >= 1000, Branched.as("Medium"))
                .defaultBranch(Branched.as("Low"));

        // High-value transactions
        branches.get("Branch-High").to("transactions.high", Produced.with(Serdes.String(), Serdes.String()));

        // Medium-value transactions
        branches.get("Branch-Medium").to("transactions.medium", Produced.with(Serdes.String(), Serdes.String()));

        // STEP 2: Aggregate Transactions per City
        transactionStream.map((key, value) -> {
                    JSONObject json = new JSONObject(value);
                    return KeyValue.pair(json.getString("city"), value);
                })
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .aggregate(
                        () -> new JSONObject().put("orderCount", 0).put("totalAmount", 0).toString(),
                        (key, newValue, aggValue) -> {
                            JSONObject newJson = new JSONObject(newValue);
                            JSONObject aggJson = new JSONObject(aggValue);
                            aggJson.put("orderCount", aggJson.getInt("orderCount") + 1);
                            aggJson.put("totalAmount", aggJson.getDouble("totalAmount") + newJson.getDouble("amount"));
                            return aggJson.toString();
                        },
                        Materialized.with(Serdes.String(), Serdes.String())
                )
                .toStream()
                .to("city.transaction.stats", Produced.with(Serdes.String(), Serdes.String()));

        // STEP 3: Windowed City-Level Analysis (5-Minute Tumbling Windows)
        transactionStream.map((key, value) -> {
                    JSONObject json = new JSONObject(value);
                    return KeyValue.pair(json.getString("city"), value);
                })
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
                .count(Materialized.as("city-transaction-windowed"))
                .toStream()
                .map((key, value) -> KeyValue.pair(key.key(), "WindowedCount: " + value))
                .to("city.transaction.windowed", Produced.with(Serdes.String(), Serdes.String()));

        // STEP 4: Scrutinize High-Risk Transactions
        transactionStream.filter((key, value) -> {
                    JSONObject json = new JSONObject(value);
                    return json.getDouble("amount") >= 8000 &&
                            (json.getString("channel").equalsIgnoreCase("ONLINE") ||
                                    json.getString("channel").equalsIgnoreCase("MOBILE"));
                })
                .mapValues(value -> {
                    JSONObject json = new JSONObject(value);
                    json.put("HIGH_RISK", true);
                    json.put("reason", "High-value transaction on ONLINE or MOBILE channel");
                    return json.toString();
                })
                .to("transactions.scrutiny", Produced.with(Serdes.String(), Serdes.String()));

        // STEP 5: Enrich Transactions with Risk Level
        transactionStream.mapValues(value -> {
            JSONObject json = new JSONObject(value);
            double amount = json.getDouble("amount");
            if (amount < 1000) {
                json.put("riskLevel", "LOW");
            } else if (amount < 5000) {
                json.put("riskLevel", "MEDIUM");
            } else {
                json.put("riskLevel", "HIGH");
            }
            return json.toString();
        }).to("transactions.enriched", Produced.with(Serdes.String(), Serdes.String()));

        // STEP 6: Mask Customer ID in Logs
        transactionStream.mapValues(value -> {
                    JSONObject json = new JSONObject(value);
                    String customerId = json.getString("customerId").replaceAll("\\d", "*");
                    json.put("customerId", customerId);
                    return json.toString();
                })
                .foreach((key, value) -> System.out.println("Masked Transaction: " + value));

        // STEP 7: Re-key by Channel
        transactionStream.selectKey((key, value) -> {
                    JSONObject json = new JSONObject(value);
                    return json.getString("channel");
                })
                .to("transactions.by.channel", Produced.with(Serdes.String(), Serdes.String()));

        // Start the Kafka Streams Application
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    // Helper Method to Parse Amount
    private static double parseAmount(String value) {
        JSONObject json = new JSONObject(value);
        return json.getDouble("amount");
    }
}