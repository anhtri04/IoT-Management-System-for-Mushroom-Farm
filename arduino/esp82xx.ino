#include <ESP8266WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <Stepper.h>
#include <Servo.h>
#include <ArduinoJson.h>
#include <time.h>

// ===== Wi-Fi Credentials =====
const char *ssid = "International University";
const char *password = "";

// ===== AWS IoT MQTT Broker =====
const char *mqttServer = "a1xubhmogrk0r9-ats.iot.ap-southeast-1.amazonaws.com"; // Replace this
const int mqttPort = 8883;

// ===== AWS IoT Certificates =====
static const char *ca_cert = R"EOF(
-----BEGIN CERTIFICATE-----
MIIDQTCCAimgAwIBAgITBmyfz5m/jAo54vB4ikPmljZbyjANBgkqhkiG9w0BAQsF
ADA5MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6
b24gUm9vdCBDQSAxMB4XDTE1MDUyNjAwMDAwMFoXDTM4MDExNzAwMDAwMFowOTEL
MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEZMBcGA1UEAxMQQW1hem9uIFJv
b3QgQ0EgMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALJ4gHHKeNXj
ca9HgFB0fW7Y14h29Jlo91ghYPl0hAEvrAIthtOgQ3pOsqTQNroBvo3bSMgHFzZM
9O6II8c+6zf1tRn4SWiw3te5djgdYZ6k/oI2peVKVuRF4fn9tBb6dNqcmzU5L/qw
IFAGbHrQgLKm+a/sRxmPUDgH3KKHOVj4utWp+UhnMJbulHheb4mjUcAwhmahRWa6
VOujw5H5SNz/0egwLX0tdHA114gk957EWW67c4cX8jJGKLhD+rcdqsq08p8kDi1L
93FcXmn/6pUCyziKrlA4b9v7LWIbxcceVOF34GfID5yHI9Y/QCB/IIDEgEw+OyQm
jgSubJrIqg0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMC
AYYwHQYDVR0OBBYEFIQYzIU07LwMlJQuCFmcx7IQTgoIMA0GCSqGSIb3DQEBCwUA
A4IBAQCY8jdaQZChGsV2USggNiMOruYou6r4lK5IpDB/G/wkjUu0yKGX9rbxenDI
U5PMCCjjmCXPI6T53iHTfIUJrU6adTrCC2qJeHZERxhlbI1Bjjt/msv0tadQ1wUs
N+gDS63pYaACbvXy8MWy7Vu33PqUXHeeE6V/Uq2V8viTO96LXFvKWlJbYK8U90vv
o/ufQJVtMVT8QtPHRh8jrdkPSHCa2XV4cdFyQzR1bldZwgJcJmApzyMZFo6IQ6XU
5MsI+yMRQ+hDKXJioaldXgjUkK642M4UwtBV8ob2xJNDd2ZhwLnoQdeXeGADbkpy
rqXRfboQnoZsG4q5WTP468SQvvG5
-----END CERTIFICATE-----
)EOF";

static const char *client_cert = R"KEY(
-----BEGIN CERTIFICATE-----
MIIDWTCCAkGgAwIBAgIUe5DRmWuXCp+9FUd1Kdu5dIc8vHMwDQYJKoZIhvcNAQEL
BQAwTTFLMEkGA1UECwxCQW1hem9uIFdlYiBTZXJ2aWNlcyBPPUFtYXpvbi5jb20g
SW5jLiBMPVNlYXR0bGUgU1Q9V2FzaGluZ3RvbiBDPVVTMB4XDTI1MDcyOTA3MjE1
MFoXDTQ5MTIzMTIzNTk1OVowHjEcMBoGA1UEAwwTQVdTIElvVCBDZXJ0aWZpY2F0
ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKSYZZvoSmZuPKuEBRj+
iAM73SHVZW4x/4e0t0STntg2FS/vgkGp5V83D5Be0DZ4E3QJcCNAJipOGiZOs8PV
G2Ly9pCTLmSRXJicoO+xbZiXtT6N0xo6avEv4REMfJk2UdkjMdp1LQn7HQ0AIkCF
I9ta/ppnovV+djnf1TJ6DfxRBs6W0KcM1tnH+Nva4h24V/2RrbalIlE8Icu/82gF
UsKwZuYB8A96+MOWVd6MhnbpgPU2R23BzKiB+T8FZVIbCKhEiHySGNEq8hnyFfYK
iO2dvCt6yZmmrqNOTXlFh+OXxy6wS4OMJQCEiJpzvd8+nvdYHqrcDKBsWH4JT5Kf
/kUCAwEAAaNgMF4wHwYDVR0jBBgwFoAUAMxvhTbnee7MqisRTnUpUt/DPr0wHQYD
VR0OBBYEFB2QgxmO61YJZhHgLTEZaW3W/6LfMAwGA1UdEwEB/wQCMAAwDgYDVR0P
AQH/BAQDAgeAMA0GCSqGSIb3DQEBCwUAA4IBAQCPhNLn7k/godSaUlWUWenFuv8N
2TW0BAtXLlGATSbWD6JjrowHBLEW/9dS22nWl+tEvnICQPRda6H+RkIvIqavK01C
EzcdMfMAWOK1AD1ftXVIC7su4o1EPlmMl9Q+osmtMNHdai7nLoxZ59TWMdftRvwy
HxWOh0fb34BQFg9VHKCZWrUEcVsTPZbk1N0xGGX2E4M07mtknDnP/gDjjcXhgV/i
X9Uvje825kYJKfZleZ1/dC6kSYnyaZO2L4ZqVLpWur8wH8RWpNHZER7FgOadecJ6
mLXrhxWoJDsc447LJvMwTlWFLgjNCZJdyb8M9Mb/sjSKPRXyhPCC1XU4MHcj
-----END CERTIFICATE-----
)KEY";

static const char *private_key = R"KEY(
-----BEGIN RSA PRIVATE KEY-----
MIIEogIBAAKCAQEApJhlm+hKZm48q4QFGP6IAzvdIdVlbjH/h7S3RJOe2DYVL++C
QanlXzcPkF7QNngTdAlwI0AmKk4aJk6zw9UbYvL2kJMuZJFcmJyg77FtmJe1Po3T
Gjpq8S/hEQx8mTZR2SMx2nUtCfsdDQAiQIUj21r+mmei9X52Od/VMnoN/FEGzpbQ
pwzW2cf429riHbhX/ZGttqUiUTwhy7/zaAVSwrBm5gHwD3r4w5ZV3oyGdumA9TZH
bcHMqIH5PwVlUhsIqESIfJIY0SryGfIV9gqI7Z28K3rJmaauo05NeUWH45fHLrBL
g4wlAISImnO93z6e91geqtwMoGxYfglPkp/+RQIDAQABAoIBAHsNhEl+7MCPgcQo
uzboc/8W7kexJlewAX4PWURnqMW0CTsBMLyuq9D3dSdV+wv90W1a8P8pol7WbS5e
gH7T/9mGE1ga8QA2vKdL0fXhDDKcmN/fYAenzPPr+7OnRi+1MJPJgCf2mmlv11Q0
2WarIxeHe/krqeUnWnQCJ33JSlNrfV23oj3Q8UIfD4lInamMylwqvDDeLVD5dX1v
iwwSvyE4Amtbxo+4ufifKlCc39qSANKVFOz/JjiDMgH2O0KmRc5+y5/0/JeGFfUM
nMMjrJ907SfF9ff7JWW99TAESKiTUy9/7+RdCQkPU6dv/h2HtxkqbziRHtfKzyZB
/7CTrAECgYEAzzexl6qzPtlrlfbWHMq/tFYl1+fsR20MWFlR8Z0umThkciOiwXlW
4mut+LgQwuMeQjAb52RYTd+VPuChmLkyw6TIdXdTAi5a0UDRXvF5ATiLRqNlTKWk
97bsUKluxHAKpxUSObUh9s0nQgmnxGIHV4ugfDIK0JvqCHlia9/HqoUCgYEAy1gA
yhr+PwP5kGQxOto98WqpAbLpxQ4+QesesLQh4lYoEsDrEwW/t96vBU9ow4R8VMuN
Ezg6GVGZ47PDg2btnhV/PW6z9NwFtRK2voBQKD3/pW4Rh2nqsmItOLMy3WIkVfGZ
ZCZAT6ZqRYGVaxd82cbKQRWwPKkl4P5a6LN5sMECgYBs8pJL9LLjU3ruUOZIIw1h
6n1hJA4li58XAHdF+mdapbyiCjxzTrGwv/6rK1Ocs77f+91f7nFDGE+mzIIKAy0+
ke/XWtJo7ihPpq2uJnWBf9IyZIxJHTo8B4/95OdZHrNrH3gCoUpnMXv3i44KgaPs
knvhcIohHiiRJgjqQLEA5QKBgBbaA/SyheIFC1XLvLCVeCKaTvXu6lCH4j08NA29
oJU0A3rgDUrs+XerYdXh768RQ3uwCGGhjv3rarTpLc6lkYyTc+Iuf7DDzSxlNv4/
o0JyfTlVHkkugP6dA+H8WXoBnn2+EfPsTIvm8shu4KDRWemLao1X559ZjvUzAHpo
OQPBAoGAOn4hMGkHRP4S5v7RwxZEUhRtU4LD0Fgc5OmXCIm8srSWcd5i8yCM2JR1
ImGv7st1LkIR4Fzdy+5nE2u6dj0ToTQK4vocuPfWu1G5o97bSu2QaQtJu/6CfhZ2
xCfqFkH3PxOXRQU03An/1vMtT0vMKc6o9MK1q8Sb92IJKfjZtqU=
-----END RSA PRIVATE KEY-----
)KEY";

// ===== DHT11 Setup =====
#define DHTPIN 4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// ===== Pin Definitions =====
const int relayPin = 16;
const int lightPin = A0;
const int motorPin1 = 0;
const int motorPin2 = 2;
const int fanRelayPin = 5;
const int stepperPins[] = {14, 12, 13, 15};
const int servoPin = 3;

// ===== Stepper Setup =====
#define STEPS_PER_REVOLUTION 2048
Stepper myStepper(STEPS_PER_REVOLUTION, stepperPins[0], stepperPins[1], stepperPins[2], stepperPins[3]);

// ===== Servo Setup =====
Servo myServo;

// ===== MQTT Setup =====
WiFiClientSecure net;
PubSubClient client(net);

// ===== Device Control Logic =====
void controlDCMotor(String direction)
{
  if (direction == "FORWARD")
  {
    digitalWrite(motorPin1, HIGH);
    digitalWrite(motorPin2, LOW);
  }
  else if (direction == "BACKWARD")
  {
    digitalWrite(motorPin1, LOW);
    digitalWrite(motorPin2, HIGH);
  }
  else
  {
    digitalWrite(motorPin1, LOW);
    digitalWrite(motorPin2, LOW);
  }
}

void controlStepper(String direction)
{
  if (direction == "LEFT")
  {
    myStepper.step(512);
  }
  else if (direction == "RIGHT")
  {
    myStepper.step(-512);
  }
}

void controlServo(String position)
{
  int angle = position.toInt();
  if (angle >= 0 && angle <= 180)
  {
    myServo.write(angle);
  }
}

// ===== Setup Function =====
void setup()
{
  Serial.begin(115200);
  dht.begin();

  pinMode(relayPin, OUTPUT);
  digitalWrite(relayPin, LOW);
  pinMode(fanRelayPin, OUTPUT);
  digitalWrite(fanRelayPin, LOW);
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  controlDCMotor("STOP");

  for (int i = 0; i < 4; i++)
    pinMode(stepperPins[i], OUTPUT);
  myStepper.setSpeed(15);
  myServo.attach(servoPin);
  myServo.write(90);

  // Connect to WiFi
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");

  // Setup TLS/SSL certs
  BearSSL::X509List ca(ca_cert);
  BearSSL::X509List client_crt(client_cert);
  BearSSL::PrivateKey key(private_key);
  net.setTrustAnchors(&ca);
  net.setClientRSACert(&client_crt, &key);

  configTime(0, 0, "pool.ntp.org", "time.nist.gov");
  Serial.print("Waiting for NTP time sync");
  time_t now = time(nullptr);
  while (now < 8 * 3600 * 2)
  {
    delay(500);
    Serial.print(".");
    now = time(nullptr);
  }
  Serial.println(" done.");

  client.setServer(mqttServer, mqttPort);
  client.setCallback(mqttCallback);

  connectMQTT();
}

// ===== Reconnect MQTT =====
void connectMQTT()
{
  while (!client.connected())
  {
    Serial.print("Connecting to AWS IoT... ");
    if (client.connect("ESP8266Client"))
    {
      Serial.println("connected");
      client.subscribe("home/control/light1");
      client.subscribe("home/control/fan1");
      client.subscribe("home/control/servo");
      client.subscribe("home/control/stepper");
      client.subscribe("home/control/dc_motor");
    }
    else
    {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" retrying in 5s");
      delay(5000);
    }
  }
}

// ===== Publish State =====
void publishDeviceState(const char *key, const String &value)
{
  StaticJsonDocument<128> doc;
  doc[key] = value;
  char buffer[128];
  serializeJson(doc, buffer);
  client.publish("home/device_states", buffer);
}

void publishDeviceState(const char *key, bool value)
{
  StaticJsonDocument<128> doc;
  doc[key] = value;
  char buffer[128];
  serializeJson(doc, buffer);
  client.publish("home/device_states", buffer);
}

void publishDeviceState(const char *key, int value)
{
  StaticJsonDocument<128> doc;
  doc[key] = value;
  char buffer[128];
  serializeJson(doc, buffer);
  client.publish("home/device_states", buffer);
}

// ===== Main Loop =====
void loop()
{
  if (!client.connected())
    connectMQTT();
  client.loop();

  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  int lightLevel = analogRead(lightPin);

  if (!isnan(humidity) && !isnan(temperature))
  {
    StaticJsonDocument<128> doc;
    doc["temperature"] = temperature;
    doc["humidity"] = humidity;
    doc["light"] = lightLevel;
    char buffer[128];
    serializeJson(doc, buffer);
    client.publish("home/sensors", buffer);
  }

  delay(5000);
}

// ===== MQTT Callback =====
void mqttCallback(char *topic, byte *message, unsigned int length)
{
  String msg;
  for (unsigned int i = 0; i < length; i++)
    msg += (char)message[i];
  String topicStr = String(topic);

  if (topicStr == "home/control/light1")
  {
    digitalWrite(relayPin, msg == "on" ? HIGH : LOW);
    publishDeviceState("light1", msg == "on");
  }
  else if (topicStr == "home/control/fan1")
  {
    digitalWrite(fanRelayPin, msg == "on" ? HIGH : LOW);
    publishDeviceState("fan1", msg == "on");
  }
  else if (topicStr == "home/control/servo")
  {
    controlServo(msg);
    publishDeviceState("servo_angle", (int)msg.toInt());
  }
  else if (topicStr == "home/control/stepper")
  {
    controlStepper(msg);
    publishDeviceState("stepper", msg);
  }
  else if (topicStr == "home/control/dc_motor")
  {
    controlDCMotor(msg);
    publishDeviceState("dc_motor", msg);
  }
}
