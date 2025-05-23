package com.cisco.streams;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class TransactionDataGenerator {

    private static final String[] CITIES = {"Mumbai", "Pune", "Delhi", "Chennai", "Bangalore", "Hyderabad"};
    private static final String[] CHANNELS = {"ATM", "ONLINE", "BRANCH", "MOBILE"};
    private static final String TOPIC = "raw.transactions";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    public static void main(String[] args) throws InterruptedException {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        Random random = new Random();

        for (int i = 1; i <= 1000; i++) {
            String transactionId = UUID.randomUUID().toString();
            String customerId = "CUST" + (1000 + random.nextInt(500));

            JSONObject txn = new JSONObject();
            txn.put("transactionId", transactionId);
            txn.put("customerId", customerId);
            txn.put("amount", 100 + random.nextInt(9900));
            txn.put("channel", CHANNELS[random.nextInt(CHANNELS.length)]);
            txn.put("city", CITIES[random.nextInt(CITIES.length)]);
            txn.put("timestamp", Instant.now().toString());

            producer.send(new ProducerRecord<>(TOPIC, customerId, txn.toString()));
            System.out.println("Sent transaction: " + txn);

            Thread.sleep(1000); // 1 second delay
        }

        producer.close();
    }
}