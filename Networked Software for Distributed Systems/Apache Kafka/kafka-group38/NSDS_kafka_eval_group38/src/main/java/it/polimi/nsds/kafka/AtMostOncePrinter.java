package it.polimi.nsds.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;

public class AtMostOncePrinter {
    private static final String topic = "inputTopic";
    private static final String serverAddr = "localhost:9092";
    private static final int threshold = 500;

    public static void main(String[] args) {
        String groupId = args[0];

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        // implement at most one semantics, so we just commit as soon as we read manually
        // by the help of this, we can commit messages we got, independent of their processing (it can crash after we got)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(false));
        // we want to read only committed messages, does not matter also because of the assumptions we made in the assignment
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        // order does not matter here because of the nature of the problem
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        while(true){
            final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
            // we want broker to know the offset of the last record we have read, so wait for it
            consumer.commitSync();
            for (final ConsumerRecord<String, Integer> record : records) {
                if(record.value() > threshold){
                    System.out.println("Partition: " + record.partition() +
                            ", Key: " + record.key() +
                            ", Value: " + record.value());
                }
            }
        }

    }
}
