# Evaluation lab - Node-RED

## Group number: 38

## Group members

- Student 1 : Fatih Temiz
- Student 2 : Mehmet Emre Akbulut
- Student 3 : Hessam Hashemizadeh 

## Description of message flows

Description of functions:
Message Validator: We check received telegram message if the format of the message is appropriate and filter acceptable questions. If message format is not true, it sends possible commands. 
Message Parser: We get telegram message from validator if it valid message and parse the message according to city, wind/temp_forecast, two-days/tomorrow and send to different OpenWeather map nodes if it is Milan or Rome. We are passing variable called as "increment" which is "1" if tomorrow and "2" if two days.
Milan:OpenWeather node to get data from Milan
Rome:OpenWeather node to get data from Milan
Closest Time Slot Finder: It gets current time from the data and loop through the forecast array and according to tomorrow or two day (with increment variable). We select the closest timestamp as forecasted which has min difference for estimated time.
Response Creator:We are getting global response variables and set global response as sender message, we are also checking and formatting message if it is changed according to last asked time or not. 
Text Node:{{payload.day}} {{payload.query}} for {{payload.city}} is {{payload.response}}. {{payload.extra_cont}  Extra_cont stands for whether question asked before or not. 
Flow initialize trigger and Flow variable initializer: We are initializing our object in nested object. City(Milan or Rome), query(forecast or wind), date(Tomorrow or Two-Days). 
File Log Trigger & Log Function & "/data/WeatherLog.txt": Which triggers every minutes.  It overrides to the text file in docker path; counter of wind_speed and weather_forecast variables in last minute. 


## Extensions 

-none 

## Bot URL 
Telegram Bot URL: https://t.me/NSDSVerySmartBot
##Bot Token
6749579494:AAF74gmYsoebdQ60tIV1mzoZfKGf5KxWf7M
##OpenWeather API key
c80d5546f3ee4243b9e487d3fc76a4b1