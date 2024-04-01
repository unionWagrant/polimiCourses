package it.polimi.nsds.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PopularTopicConsumer {
    private static final String topic = "inputTopic";
    private static final String serverAddr = "localhost:9092";

    public static void main(String[] args) {
        String groupId = args[0];

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());

        // auto commit is enabled and not guarantee anything. default is commiting async
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(true));

        // order does not matter here because of the nature of the problem
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        //initialize the map
        Map<String, Integer> keyCountMap = new HashMap<>();

        while(true){
            final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));

            for(final ConsumerRecord<String, Integer> record : records){
                String key = record.key();
                Integer value = record.value();
                incrementOrAdd(keyCountMap, key);
            }

            Map.Entry<String, Integer> topEntry = findEntryWithMaxValue(keyCountMap);
            if(topEntry == null){
                System.out.println("No top entry found");
                continue;
            }
            String topKey = topEntry.getKey();
            Integer topValue = topEntry.getValue();
            System.out.println("Top key: " + topKey + ", Top value: " + topValue);
        }

    }

    private static void incrementOrAdd(Map<String, Integer> dictionary, String key) {
        // Check if the key already exists in the dictionary
        if (dictionary.containsKey(key)) {
            // If the key exists, increment its value by 1
            int count = dictionary.get(key);
            dictionary.put(key, count + 1);
        } else {
            // If the key does not exist, add it to the dictionary with a value of 1
            dictionary.put(key, 1);
        }
    }

    private static Map.Entry<String, Integer> findEntryWithMaxValue(Map<String, Integer> dictionary) {
        if (dictionary.isEmpty()) {
            // Handle the case when the dictionary is empty
            return null;
        }

        // Initialize variables to track the maximum value and corresponding entry
        int maxValue = Integer.MIN_VALUE;
        Map.Entry<String, Integer> entryWithMaxValue = null;

        // Iterate through the entries in the dictionary
        for (Map.Entry<String, Integer> entry : dictionary.entrySet()) {
            int value = entry.getValue();

            // Update entryWithMaxValue if the current value is greater than the current maxValue
            if (value > maxValue) {
                maxValue = value;
                entryWithMaxValue = entry;
            }
        }

        return entryWithMaxValue;
    }
}
