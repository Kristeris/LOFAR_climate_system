#!/usr/bin/env python3

from datetime import datetime
import requests
import json
import random

# Get current date and time
current_datetime = datetime.now().strftime("%a %b %d %H:%M:%S %Y")

# Generate random sensor data
temperature = round(random.uniform(2.0, 40.0), 2)
humidity = round(random.uniform(5.0, 80.0), 2)

# Output the sensor data string with current datetime and random values
output = f"date = {current_datetime} temperature = {temperature} humidity = {humidity} heater state = OFF power 48V state = ON power LCU state = ON lightning state = N.A."

# Print to console
print(output)

# Also send to Spring Boot application
try:
    url = "http://localhost:8080/sensors/receive"
    headers = {"Content-Type": "application/json"}
    data = {"rawOutput": output}
    
    response = requests.post(url, json=data, headers=headers, timeout=5)
    
    if response.status_code == 200:
        print("Data successfully sent to database")
        print(f"Temperature: {temperature}°C, Humidity: {humidity}%")
    else:
        print(f"Failed to send data: {response.status_code}")
except Exception as e:
    print(f"Error sending data: {e}")
