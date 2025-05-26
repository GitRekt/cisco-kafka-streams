# 🚀 Kafka Streams Projects

This repository contains two comprehensive Kafka Streams projects demonstrating real-time data processing capabilities with Apache Kafka. Both projects showcase different aspects of stream processing, aggregation, and real-time analytics.

## 📋 Project Overview

### Project 1: Real-Time Transaction Monitoring System
A comprehensive financial transaction processing pipeline that handles real-time data enrichment, categorization, and risk analysis.

### Project 2: City-wise Order Aggregation
A streamlined order processing system that aggregates order statistics by city in real-time.

---

## 🧪 Project 1: Real-Time Transaction Monitoring

### 🎯 Objective
Design and implement a Kafka Streams pipeline to process financial transactions in real-time with enrichment, categorization, aggregation, and monitoring capabilities.

### 📥 Input Data Format
**Topic:** `raw.transactions`
```json
{
  "transactionId": "abc-123",
  "customerId": "CUST1001",
  "amount": 5400,
  "channel": "ONLINE",
  "city": "Mumbai",
  "timestamp": "2025-05-21T14:42:17.312Z"
}
```

### ✅ Implemented Features

#### 1. Transaction Stream Branching by Amount
- **High-value transactions** (≥ 5000) → `transactions.high`
- **Medium-value transactions** (1000-4999) → `transactions.medium`
- **Low-value transactions** (< 1000) → logged to console

#### 2. City-based Aggregation
- Real-time counting of transactions per city
- Total amount calculation per city
- Results published to `city.transaction.stats`

#### 3. Windowed Analysis
- 5-minute tumbling window analysis per city
- Transaction count tracking within time windows
- Output to `city.transaction.windowed`

#### 4. High-Risk Transaction Detection
- Automatic flagging of transactions ≥ 8000 via ONLINE/MOBILE channels
- Enhanced with `flag: "HIGH_RISK"` and reason field
- Scrutinized transactions sent to `transactions.scrutiny`

#### 5. Risk Level Enrichment
- Dynamic risk level assignment:
  - `LOW`: amount < 1000
  - `MEDIUM`: 1000 ≤ amount < 5000
  - `HIGH`: amount ≥ 5000

#### 6. Customer ID Masking
- Privacy protection through ID masking (e.g., `CUST1234` → `CUST****`)
- Applied to all log outputs

#### 7. Channel-based Re-keying
- Stream re-keying by channel type for downstream processing
- Enables channel-specific aggregations

### 📤 Output Topics
| Topic Name | Purpose |
|------------|---------|
| `transactions.high` | High-value transactions (≥ 5000) |
| `transactions.medium` | Medium-value transactions (1000-4999) |
| `transactions.scrutiny` | High-risk flagged transactions |
| `city.transaction.stats` | Real-time city statistics |
| `city.transaction.windowed` | Windowed city analysis |

---

## 🧪 Project 2: City-wise Order Aggregation

### 🎯 Objective
Create a Kafka Streams application for real-time order aggregation by city, providing both count and total amount statistics.

### 📥 Input Data Format
**Topic:** `cisco.orders`
```json
{
  "orderId": "fd12-xyz...",
  "customerId": 1005,
  "name": "Sneha",
  "productId": 2450,
  "price": 2200,
  "city": "Pune"
}
```

### 📤 Output Format
**Topic:** `cisco.city.orders.stats`
```json
{
  "city": "Pune",
  "orderCount": 154,
  "totalAmount": 232000
}
```

### ✅ Implemented Features
- Real-time JSON parsing and processing
- City-based stream grouping
- Concurrent order counting and amount aggregation
- Continuous statistics updates

---

## 🛠️ Technical Stack

- **Apache Kafka** - Event streaming platform
- **Kafka Streams** - Stream processing library
- **Java** - Primary development language
- **JSON** - Data serialization format
- **Maven/Gradle** - Build management (depending on setup)

## 🔧 Key Kafka Streams Concepts Demonstrated

| Concept | Description | Used In |
|---------|-------------|---------|
| Stream Processing | Real-time data transformation | Both Projects |
| Stream Branching | Conditional routing based on criteria | Project 1 |
| Grouping & Aggregation | Data grouping and statistical calculations | Both Projects |
| Windowed Operations | Time-based data analysis | Project 1 |
| Stream Enrichment | Adding computed fields to records | Project 1 |
| Key Transformation | Re-keying streams for processing optimization | Project 1 |
| JSON Processing | Parsing and manipulating JSON data | Both Projects |

## 🚀 Getting Started

### Prerequisites
- Java 8 or higher
- Apache Kafka 2.8+
- Maven or Gradle

### Running the Projects

1. **Start Kafka Services**
   ```bash
   # Start Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties
   
   # Start Kafka Server
   bin/kafka-server-start.sh config/server.properties
   ```

2. **Create Required Topics**
   ```bash
   # For Project 1
   bin/kafka-topics.sh --create --topic raw.transactions --bootstrap-server localhost:9092
   bin/kafka-topics.sh --create --topic transactions.high --bootstrap-server localhost:9092
   bin/kafka-topics.sh --create --topic transactions.medium --bootstrap-server localhost:9092
   bin/kafka-topics.sh --create --topic transactions.scrutiny --bootstrap-server localhost:9092
   bin/kafka-topics.sh --create --topic city.transaction.stats --bootstrap-server localhost:9092
   bin/kafka-topics.sh --create --topic city.transaction.windowed --bootstrap-server localhost:9092
   
   # For Project 2
   bin/kafka-topics.sh --create --topic cisco.orders --bootstrap-server localhost:9092
   bin/kafka-topics.sh --create --topic cisco.city.orders.stats --bootstrap-server localhost:9092
   ```

3. **Generate Test Data**
   - Use the provided `TransactionDataGenerator.java` for Project 1
   - Create sample order data for Project 2

4. **Run the Applications**
   - Compile and execute the Kafka Streams applications
   - Monitor the output topics using Kafka console consumers

## 📊 Monitoring and Verification

### Console Consumer Commands
```bash
# Monitor high-value transactions
bin/kafka-console-consumer.sh --topic transactions.high --from-beginning --bootstrap-server localhost:9092

# Monitor city statistics
bin/kafka-console-consumer.sh --topic city.transaction.stats --from-beginning --bootstrap-server localhost:9092

# Monitor order statistics
bin/kafka-console-consumer.sh --topic cisco.city.orders.stats --from-beginning --bootstrap-server localhost:9092
```

## 🎯 Learning Outcomes

Through these projects, you'll gain hands-on experience with:
- Real-time stream processing architecture
- Complex event processing patterns
- Data enrichment and transformation techniques
- Windowed aggregations and time-based analytics
- Risk detection and monitoring systems
- Scalable data pipeline design

## 📝 Project Structure

```
├── src/
│   ├── main/
│   │   └── java/
│   │       ├── TransactionStreamProcessor.java
│   │       ├── OrderAggregationProcessor.java
│   │       └── TransactionDataGenerator.java
│   └── test/
├── README.md
└── pom.xml / build.gradle
```

## 🤝 Contributing

Feel free to fork this repository and submit pull requests for improvements or additional features. All contributions are welcome!

