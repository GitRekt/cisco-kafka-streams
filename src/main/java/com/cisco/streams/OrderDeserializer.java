package com.cisco.streams;

import org.apache.kafka.common.serialization.Deserializer;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class OrderDeserializer implements Deserializer<CityWiseOrderAggregator.Order> {
    @Override
    public CityWiseOrderAggregator.Order deserialize(String topic, byte[] data) {
        if (data == null) return null;

        // Convert byte array back to JSON string, then to Order object
        String jsonString = new String(data, StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(jsonString);
        String city = json.getString("city");
        double price = json.getDouble("price");
        return new CityWiseOrderAggregator.Order(city, price);
    }
}