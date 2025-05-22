package com.cisco.streams;

import org.apache.kafka.common.serialization.Serializer;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class CityStatsSerializer implements Serializer<CityWiseOrderAggregator.CityStats> {
    @Override
    public byte[] serialize(String topic, CityWiseOrderAggregator.CityStats data) {
        if (data == null) return null;

        // Convert CityStats object to JSON string
        JSONObject json = new JSONObject();
        json.put("orderCount", data.getOrderCount());
        json.put("totalAmount", data.getTotalAmount());
        return json.toString().getBytes(StandardCharsets.UTF_8); // Convert to byte array
    }
}