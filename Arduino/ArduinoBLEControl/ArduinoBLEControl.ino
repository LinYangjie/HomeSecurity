/** UW EE P 523. SPRING 2020
    Example of simple interaction beteween Adafruit Circuit Playground
    and Android App. Communication with BLE - uart
*********************************************************************/
#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"
#include <Adafruit_CircuitPlayground.h>

#include "BluefruitConfig.h"

#if SOFTWARE_SERIAL_AVAILABLE
#include <SoftwareSerial.h>
#endif


// Strings to compare incoming BLE messages
String cancel = "cancel";
String readtemp = "readtemp";
String stp = "stop";
String deactivate = "deactivate";
String activate = "activate";

// Accelometer and motionDetection
float X, Y, startAccelMag = 0;
int sensorTemp = 0;
int counter = 0;
const long interval = 500;
float threshold = 0.15;

// some Flags to control the message be sent and for the states recover
bool alertActivate = false;
bool alertCounter = false;
bool shakeFlag = false;
bool alertSent = false;
bool alertCancel = false;
bool colorSwitch = false;
bool leftButtonPressed = false;
bool rightButtonPressed = false;
unsigned long lastShakeTime = 0;
unsigned long shakeInterval = 3000;
unsigned long previousMillis = 0; 
unsigned long counterStartTime = 0;


/*=========================================================================
    APPLICATION SETTINGS
    -----------------------------------------------------------------------*/
#define FACTORYRESET_ENABLE         0
#define MINIMUM_FIRMWARE_VERSION    "0.6.6"
#define MODE_LED_BEHAVIOUR          "MODE"
/*=========================================================================*/

// Create the bluefruit object, either software serial...uncomment these lines

Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);

/* ...hardware SPI, using SCK/MOSI/MISO hardware SPI pins and then user selected CS/IRQ/RST */
// Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

/* ...software SPI, using SCK/MOSI/MISO user-defined SPI pins and then user selected CS/IRQ/RST */
//Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_SCK, BLUEFRUIT_SPI_MISO,
//                             BLUEFRUIT_SPI_MOSI, BLUEFRUIT_SPI_CS,
//                             BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);


// A small helper to show errors on the serial monitor
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}


// function
float getAccelMagnitude() {
  float X = 0;
  float Y = 0;

  float dX = 0;
  float dY = 0;
  //float dZ = 0;

  for (int i = 0; i < 10; ++i) {
    dX = CircuitPlayground.motionX();
    dY = CircuitPlayground.motionY();

    X += dX;
    Y += dY;
  }

  X /= 10;
  Y /= 10;

  return sqrt(X * X + Y * Y);
}

boolean isShake() {
  float currentAccelMag = getAccelMagnitude();
  if ((currentAccelMag - startAccelMag) < threshold) {
    return false;
  }

  unsigned long now = millis();
  if ((now - lastShakeTime) < shakeInterval) {
    // don't give shake gestures too frequently
    return false;
  }

  Serial.println("Shake detected");
  lastShakeTime = now;
  return true;
}


void Blink(int a, int b, int c) {
    unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= interval) {
      previousMillis = currentMillis;
      if (!colorSwitch) {
        for (int i = 0; i < 11; i++) {
          CircuitPlayground.setPixelColor(i, a, b, c);
        }
        colorSwitch = true;
      } else {
        CircuitPlayground.clearPixels();
        colorSwitch = false;
      }

    }
}



void setup(void)
{
  CircuitPlayground.begin();
  Serial.begin(115200);

  /* Initialise the module */
  Serial.print(F("Initialising the Bluefruit LE module: "));

  if ( !ble.begin(VERBOSE_MODE) )
  {
    error(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?"));
  }
  Serial.println( F("OK!") );

  if ( FACTORYRESET_ENABLE )
  {
    /* Perform a factory reset to make sure everything is in a known state */
    Serial.println(F("Performing a factory reset: "));
    if ( ! ble.factoryReset() ) {
      error(F("Couldn't factory reset"));
    }
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);

  Serial.println("Requesting Bluefruit info:");
  /* Print Bluefruit information */
  ble.info();

  Serial.println(F("Please use Adafruit Bluefruit LE app to connect in UART mode"));
  Serial.println(F("Then Enter characters to send to Bluefruit"));
  Serial.println();

  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while (! ble.isConnected()) {
    delay(500);
  }
  
  for (int i = 0; i < 11; i++) {
    CircuitPlayground.setPixelColor(i, 0, 0, 255);
  }
  Serial.println("CONNECTED:");
  Serial.println(F("******************************"));


  // LED Activity command is only supported from 0.6.6
  if ( ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION) )
  {
    // Change Mode LED Activity
    Serial.println(F("Change LED activity to " MODE_LED_BEHAVIOUR));
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
  }

  // Set module to DATA mode
  Serial.println( F("Switching to DATA mode!") );
  ble.setMode(BLUEFRUIT_MODE_DATA);

  Serial.println(F("******************************"));

  CircuitPlayground.setPixelColor(20, 20, 20, 20);

  delay(100);
  // get the initial accelMagnitude
  startAccelMag = getAccelMagnitude();
   
  
}
/**************************************************************************/
/*!
   Constantly poll for new command or response data
*/
/**************************************************************************/
void loop(void)
{

  if (isShake() && alertActivate && !shakeFlag) {
    Serial.println("detect motion");
    shakeFlag = true;
    alertCounter = true;
    alertCancel = false;
    ble.print("door open!!");
    unsigned long timer = millis();
  }
  
  if (alertCounter) {
       
    if (counter > 2500) {
       Blink(255, 0, 0);
       CircuitPlayground.playTone(500,100);
       delay(100);
       if (!alertSent) {
          Serial.println("times up!");
          ble.print("times up!");
          alertSent = true;
       }
    }
    else {
      Blink(255, 255, 0);
      Serial.println(counter);
      counter++;
    }
  }



  // Save received data to string
  String received = "";
  while ( ble.available() )
  {
    int c = ble.read();
    received += (char)c;
    delay(50);
  }


   if (cancel == received) {
    Serial.println("ALERT CANCEL");
    for (int i = 0; i < 11; i++) {
      CircuitPlayground.setPixelColor(i, 0, 255, 0);
    }
    alertCounter = false; 
    alertActivate = false; 
    counter = 0;
    
  }

  else if (activate == received) {
    Serial.println("ALERT ACTIVATE");
    for (int i = 0; i < 11; i++) {
        CircuitPlayground.setPixelColor(i, 0, 255, 0);
    }
    shakeFlag = false;
    alertActivate = true;
  }

  else if (deactivate == received) {
    Serial.println("DEACTIVATE");

    for (int i = 0; i < 11; i++) {
        CircuitPlayground.setPixelColor(i, 255, 255, 255);
    }
    alertActivate = false;
  }


  else if (readtemp == received) {

    sensorTemp = CircuitPlayground.temperature(); // returns a floating point number in Centigrade
    Serial.println("Read temperature sensor");
    delay(10);

    //Send data to Android Device
    char output[8];
    String data = "";
    data += sensorTemp;
    Serial.println(data);
    data.toCharArray(output, 8);
    ble.print(data);
    delay(100);

    // if too cold
    if (sensorTemp < 26) {
      ble.print("too cold!");
    }
  }

  else if (stp == received) {
    CircuitPlayground.clearPixels();

  }
}
