package com.cisco.streams;

import org.apache.kafka.common.serialization.Deserializer;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class CityStatsDeserializer implements Deserializer<CityWiseOrderAggregator.CityStats> {
    @Override
    public CityWiseOrderAggregator.CityStats deserialize(String topic, byte[] data) {
        if (data == null) return null;

        String jsonString = new String(data, StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(jsonString);
        long orderCount = json.getLong("orderCount");
        double totalAmount = json.getDouble("totalAmount");
        return new CityWiseOrderAggregator.CityStats(orderCount, totalAmount);
    }
}