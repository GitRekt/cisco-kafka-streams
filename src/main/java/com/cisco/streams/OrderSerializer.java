package com.cisco.streams;

import org.apache.kafka.common.serialization.Serializer;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class OrderSerializer implements Serializer<CityWiseOrderAggregator.Order> {
    @Override
    public byte[] serialize(String topic, CityWiseOrderAggregator.Order data) {
        if (data == null) return null;

        // Convert Order object to JSON string
        JSONObject json = new JSONObject();
        json.put("city", data.getCity());
        json.put("price", data.getPrice());
        return json.toString().getBytes(StandardCharsets.UTF_8); // Convert to byte array
    }
}