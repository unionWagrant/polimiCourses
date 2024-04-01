# Evaluation lab - Apache Kafka

## Group number: 38

## Group members

- Fatih Temiz
- Hessam Hashemizadeh
- Mehmet Emre Akbulut

## Exercise 1

- Number of partitions allowed for inputTopic (1, N)
- Number of consumers allowed (1, N)
    - Consumer 1: [ConsumerGroup1]
    - Consumer 2: [ConsumerGroup1]
    - ...
    - Consumer n: [ConsumerGroup1]
    #### We can have as many consumers(logically less than or equal to number of partitions) as we want in a consumer group. But we have only 1 one consumer group to avoid duplication(at most once).
## Exercise 2

- Number of partitions allowed for inputTopic (1, N)
- Number of consumers allowed (1, 1)
    - Consumer 1: [ConsumerGroupX1]
    - Consumer 2: [ConsumerGroupX2]
    - ...
    - Consumer n: [ConsumerGroupXN]
    #### We can have as many consumer groups as we want but they should have 1 and only 1 consumer in them. Since the HashMap can not be shared between consumer inside each groups, we can not have more than 1 consumer in a consumer group.
    #### To run both programs simultaneously we should use different consumer groups. Otherwise, the second consumer will not be able to read the messages from the topic.
    #### Each consumer group individually counts the top value 'received', so it is normal to assume values of different consumer groups will be different.
    #### To run simultaneously we have added letter 'X' to ConsumerGroup argument in Exercise 2.