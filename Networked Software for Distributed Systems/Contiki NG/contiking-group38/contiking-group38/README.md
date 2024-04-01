# Evaluation lab - Contiki-NG

## Group number: 38

## Group members

- Fatih Temiz
- Hessam Hashemizadeh 10900041
- Mehmet Emre Akbulut

## Solution description
Serverside:
    Initializes DAG root. In serverside we have a new reciever function that stored incoming receivers until the max receivers to check if reached the threshold. Then we read new readings with UDP and the compute the average of the last MAX_READINGS.
Clientside: 
    On the client side we have queue for our readings that is equal size to MAX_READINGS. We will try to batch the inputs in the queue and based on the number of retries we calculate the average when we are sending. if the retry exceeds the MAX_READINGS, we replace the corresponding value by calculating the retries % MAX_READINGS. 

