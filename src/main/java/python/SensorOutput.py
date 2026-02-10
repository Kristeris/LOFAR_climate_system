#!/usr/bin/env python3

from datetime import datetime
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
