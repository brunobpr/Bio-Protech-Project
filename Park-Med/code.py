#importing required libraries
import time
import board
import digitalio
import busio
import adafruit_lis3dh
import pulseio
import math
from adafruit_ble import BLERadio
from adafruit_ble.advertising.standard import ProvideServicesAdvertisement
from adafruit_ble.services.nordic import UARTService



# Defining a new bluetooth low energy service
ble = BLERadio()
# Setting the advertisement name to Park Med
ble.name = "Park Med"

#defining a new UART protocol
#UART is a bluetooth protocol that allows the transmission of short messages
uart_service = UARTService()

#bluetooth stream message global
streamMessage = None

#advertising the bluetooth device with uart_service
advertisement = ProvideServicesAdvertisement(uart_service)



#small red LED
led = digitalio.DigitalInOut(board.D13)
led.direction = digitalio.Direction.OUTPUT

#global duty cycle for the motors
duty = 36695
motor1 = None
motor2 = None
motor3 = None
motor4 = None
motor5 = None


def start():
    # Loop until there is a new message from the Bluetooth
    streamMessage = None
    data_string = ""
    while len(data_string) == 0 and ble.connected:
        # Define the intensity of the vibration using pwm duty_cycle
        motor1.duty_cycle = duty
        motor2.duty_cycle = duty
        motor3.duty_cycle = duty
        motor4.duty_cycle = duty
        motor5.duty_cycle = duty
        # Check if there is new message from the Bluetooth
        streamMessage = uart_service.readline()
        data_string = ''.join([chr(b) for b in streamMessage])
        if len(data_string) > 0:
            return data_string
    stop()

# initialising the accelerometer sensor
i2c = busio.I2C(board.ACCELEROMETER_SCL, board.ACCELEROMETER_SDA)
lis3dh = adafruit_lis3dh.LIS3DH_I2C(i2c, address=0x19)
lis3dh.range = adafruit_lis3dh.RANGE_8_G


def graph():
    while ble.connected:
        # read the 3-axes
        x, y, z = lis3dh.acceleration
        # calculate the net acceleration
        tremor = math.sqrt(((x * x) + (y * y) + (z * z)))
        time.sleep(0.1)
        # send it to the app via bluetooth
        uart_service.write(str(tremor))


# denit method frees the ports, so the motors stop
def stop():
    motor1.deinit()
    motor2.deinit()
    motor3.deinit()
    motor4.deinit()
    motor5.deinit()

while True:
    # start to advertise the bluetooth
    ble.start_advertising(advertisement)

    #if the bluetooth is not connected the red light will be ON
    #it is also a good way to check if the program is running
    led.value = True

    while not ble.connected:
        led.value = True

    # Now we're connected

    while ble.connected:
            #if the bluetooth is connected the LED will be OFF
            led.value = False
            try:
                streamMessage = uart_service.readline()
                data_string = ''.join([chr(b) for b in streamMessage])
                if data_string == "START":
                    print('START')
                    # Initialise all the motors with the lowest PWM
                    motor1 = pulseio.PWMOut(board.A1, duty_cycle= 65534, frequency=50000)
                    motor2 = pulseio.PWMOut(board.A2, duty_cycle= 65534, frequency=50000)
                    motor3 = pulseio.PWMOut(board.A3, duty_cycle= 65534, frequency=50000)
                    motor4 = pulseio.PWMOut(board.A4, duty_cycle= 65534, frequency=50000)
                    motor5 = pulseio.PWMOut(board.A6, duty_cycle= 65534, frequency=50000)
                    # Call the start method, if a new message from the bluetooth is captured
                    # inside of the start method, it will be returned
                    data_string = start()
                if data_string == "STOP":
                    print('STOP')
                    stop()
                if data_string == "G":
                    print('GRAPH')
                    data_string = graph()
                if data_string.find("F") > 0:
                    print('NEW FREQUENCY')
                    f, f1 = data_string.split("F")
                    frequency = int(f)
                    duty = int(65535 - 358 * frequency)
                    data_string = start()
            except ValueError:
                continue    # or pass.
	
