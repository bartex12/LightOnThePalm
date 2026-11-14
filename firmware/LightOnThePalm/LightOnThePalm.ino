//управление адресной лентой через NodeMCU (ESP8266) с WiFi в режиме STA или AP
//код Андроид: https://github.com/bartex12/LightOnThePalm
//варианты - бег строка, pride, flame, статич цвета, конфетти, радуги
//ESP8266 регистрирует сервис через mDNS, приложение находит его через NSD и узнаёт IP-адрес
//
//ВЕРСИЯ 018 - что изменено относительно 017:
// 1. Новые эффекты: Комета (/comet, w=15), Океан (/ocean, w=16), Мерцающие звёзды
//    (/stars, w=17). Параметры как у остальных динамических эффектов: скорость и яркость.
//    Все три делают один шаг за вызов и не задерживают ответ ленты на команды.
// 2. Пламя стало настраиваемым: /flame принимает яркость, высоту пламени и искры.
//    Высота 5..100 (больше - выше пламя), искры 5..200 (больше - чаще вспышки).
//    Высота и искры запоминаются вместе с эффектом.
//
//ВЕРСИЯ 017 - что изменено относительно 016:
// Перед сменой количества точек лента гасится целиком. Светодиоды WS2812 помнят последний
// цвет, пока есть питание, поэтому при уменьшении количества точек "лишние" продолжали
// гореть тем, что на них было.
//
//ВЕРСИЯ 016 - что изменено относительно 015:
// 1. Количество точек на ленте задаётся из приложения (Настройки) и хранится в памяти.
//    Команда /leds без параметров возвращает "текущее/максимум", например "32/1000";
//    /leds?number=N сохраняет новое количество и перезагружает ленту.
//    Максимум задан константой MAX_LED_NUM - см. пояснение рядом с ней.
// 2. Бегущий огонёк состояния и эффект "Бегущие огни" больше не занимают ленту на весь
//    проход: за один вызов делается один шаг. Лента откликается на команды сразу,
//    что особенно важно на длинной ленте.
//
//ВЕРСИЯ 015 - что изменено относительно 014:
// Лента запоминает последний эффект и его настройки (цвет, яркость, скорость, количество
// точек) и возвращается к нему после включения питания или перезагрузки. При запуске
// 3 секунды бежит огонёк состояния (бирюзовый - домашняя сеть, фиолетовый - локальная),
// затем включается сохранённый эффект. Красный огонёк (сети нет) не заменяется эффектом.
// Запись в память идёт через 5 секунд после последней команды, чтобы не изнашивать флеш.
//
//ВЕРСИЯ 014 - что изменено относительно 013:
// 1. После команды сменить сеть (/home, /hotpoint) лента сохраняет данные, отвечает телефону
//    и ПЕРЕЗАГРУЖАЕТСЯ. Раньше сеть переключалась "на ходу", и после этого веб-сервер и mDNS
//    могли перестать отвечать, а лента - остаться в двух сетях сразу.
// 2. Имя и пароль домашней сети больше не затираются, если подключиться не удалось.
//    Лента временно становится точкой доступа NodeMCU (красный огонёк) и каждые 3 минуты,
//    пока к ней никто не подключен, пробует домашнюю сеть снова. Это нужно, например, когда
//    после отключения света роутер загружается дольше ленты.
// 3. Режим работы хранится отдельно от имени сети:
//      нет данных        - точка доступа NodeMCU, красный огонёк (первое включение)
//      домашняя сеть     - бирюзовый огонёк; не подключилась - красный, см. п.2
//      локальная сеть    - точка доступа NodeMCU по команде пользователя, фиолетовый огонёк
// 4. Пароль сети до 63 символов, имя до 32 (было 32 и 30). Старые данные из EEPROM
//    переносятся в новый формат автоматически при первом запуске.
// 5. mDNS объявляет порт 80 (был 8080, а сервер работает на 80).
// 6. Удалены остатки MQTT вместе с логинами и паролями.
//
//ВАЖНО: после заливки скетча через USB один раз нажмите кнопку RST на плате (или выключите
//и включите питание). Иначе первая программная перезагрузка ESP8266 может зависнуть.

//#define FASTLED_ALLOW_INTERRUPTS 0  ////если только это включено перед #include "FastLED.h" - глючит первый светодиод и остальные тоже
#define FASTLED_INTERRUPT_RETRY_COUNT 0 //если только это включено перед #include "FastLED.h" - работает
//если обе выключены - глючит
#include "FastLED.h"
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <EEPROM.h>
#include <ESP8266mDNS.h>

#define LED_PIN 5 //GPIO5
//Наибольшее количество точек. Под каждую точку нужно 3 байта памяти (плюс 1 байт для
//пламени), а на вывод одной точки уходит 30 микросекунд: 1000 точек - это 4 кБ памяти
//и 30 мс на кадр. Больше ESP8266 на одном выводе надёжно не тянет: не хватает памяти,
//кадры становятся редкими, страдает WiFi. Питание длинной ленты - отдельный вопрос:
//до 60 мА на точку при белом цвете на полной яркости.
#define MAX_LED_NUM 1000
#define DEFAULT_LED_NUM 32
int ledNum = DEFAULT_LED_NUM;  //Количество точек - задаётся из приложения, хранится в EEPROM
#define LED_NUM ledNum         //в эффектах используется текущее количество точек

byte w = 0;//номер варианта свечения ленты 0 -красный (точка доступа) 1 -бирюзовый (домашняя сеть) 2 -фиолетовый (локальная сеть)
String color;  //цвет
String brightness = "50";
String playspeed = "30";
String runNumber = "1"; //количество бегущих светодиодов
String heightFlame = "55"; //высота пламени
String sparkFlame = "120"; //искры
String adapterPositionName = ""; //имя эффекта в списке телефона и адаптере списка

int r = 0;
int g = 0;
int b = 0;
byte counter = 0;                           //счётчик  до 255
int idex = 0;                                //-LED INDEX (0 to LED_COUNT-1
int ihue = 0;                                 //-HUE (0-255)
boolean isHomeNetwork = false;                 //домашняя сеть?

CRGB leds[MAX_LED_NUM];                       //область памяти под наибольшее количество точек
ESP8266WebServer server(80);                  // Объект веб-сервера. Будет прослушивать порт 80 (по умолчанию для HTTP)

const char* ssid = "NodeMCU";                   // Имя WiFi точки доступа - используется для создания AP
const char* pass = "12345678";                  // Пароль от точки доступа - используется для создания AP

String realSsid = "";  //имя сети, в которой работает ESP - его посылаем в телефон для отображения на экране

String nameEeprom = ""; //имя домашней сети из EEPROM
String passEeprom = ""; //пароль домашней сети из EEPROM

/* Настройки IP адреса точки доступа */
IPAddress local_ip(192,168,1,1);
IPAddress gateway(192,168,1,1);
IPAddress subnet(255,255,255,0);

//------------------------ размещение данных в EEPROM ------------------------//
#define EEPROM_SIZE    256
#define ADDR_SSID_LEN  0     //длина имени сети
#define ADDR_SSID      1     //имя сети, до 32 символов (адреса 1..32)
#define ADDR_PASS_LEN  40    //длина пароля
#define ADDR_PASS      41    //пароль, до 63 символов (адреса 41..103)
#define ADDR_MODE      110   //режим работы, см. ниже
#define ADDR_MAGIC     127   //признак того, что данные записаны в этом формате
#define EEPROM_MAGIC   0xA5
#define MAX_SSID_LEN   32
#define MAX_PASS_LEN   63

//последний эффект и его настройки (адреса 130..239)
#define ADDR_FX_MAGIC    130   //признак того, что эффект сохранён
#define FX_MAGIC         0x5A
#define ADDR_FX_W        131   //номер эффекта (3..14)
#define ADDR_FX_R        132   //цвет
#define ADDR_FX_G        133
#define ADDR_FX_B        134
#define ADDR_FX_BRIGHT   135   //яркость
#define ADDR_FX_SPEED_HI 136   //скорость, старший байт
#define ADDR_FX_SPEED_LO 137   //скорость, младший байт
#define ADDR_FX_RUNNUM   138   //количество бегущих точек
#define ADDR_FX_NAME_LEN 139   //длина названия эффекта
#define ADDR_FX_NAME     140   //название эффекта, до 100 байт (адреса 140..239)
#define MAX_FX_NAME_LEN  100
//количество точек на ленте (адреса 240..242)
#define ADDR_LEDS_MAGIC  240   //признак того, что количество точек сохранено
#define LEDS_MAGIC       0x3C
#define ADDR_LEDS_HI     241   //количество точек, старший байт
#define ADDR_LEDS_LO     242   //количество точек, младший байт

//настройки пламени (адреса 243..244)
#define ADDR_FX_HEIGHT   243   //высота пламени
#define ADDR_FX_SPARK    244   //искры

#define FX_FIRST         3     //номера эффектов, которые имеет смысл запоминать
#define FX_LAST          17

//режимы работы (хранятся в EEPROM по адресу ADDR_MODE)
#define MODE_NONE      0     //данных о сети нет - точка доступа, красный огонёк
#define MODE_STA       1     //подключаться к домашней сети
#define MODE_AP_USER   2     //точка доступа по команде пользователя, фиолетовый огонёк

byte wifiMode = MODE_NONE;

//перезагрузка после смены сети: сначала отвечаем телефону, потом перезагружаемся
boolean restartPending = false;
unsigned long restartAt = 0;
#define RESTART_DELAY_MS 1000

//сохранение эффекта: через 5 секунд после последней команды
boolean effectDirty = false;
unsigned long effectChangedAt = 0;
#define FX_SAVE_DELAY_MS 5000

//восстановление эффекта при запуске: сначала 3 секунды огонёк состояния
boolean restorePending = false;
unsigned long restoreAt = 0;
#define FX_RESTORE_DELAY_MS 3000

//домашняя сеть задана, но подключиться не удалось - работаем точкой доступа и пробуем снова
boolean fallbackAp = false;
unsigned long lastRetry = 0;
#define RETRY_INTERVAL_MS 180000UL   //3 минуты
#define CONNECT_ATTEMPTS  30         //30 попыток по 0,5 с = 15 секунд

//---------------для динамических эффектов------------//
void getArgsSpeedBrigthness(){
   playspeed = (server.arg(0));                                   //получаем скорость как строку 
   brightness = server.arg(1);                                    //получаем яркость  как строку
   adapterPositionName = server.arg(2);                           //получаем позицию  как строку
   Serial.print("Скорость = ");  Serial.println(playspeed);
   Serial.print("Яркость = ");  Serial.println(brightness);
   Serial.print("Позиция = ");  Serial.println(adapterPositionName);
  }

//------------для статических цветов---------------//
  void getArgsColorBrigthness(){
   color = server.arg(0);                                   //получаем цвет как строку
   brightness = server.arg(1);                               //получаем яркость  как строку
   adapterPositionName = server.arg(2);                        //получаем позицию  как строку
   Serial.print("Цвет = ");  Serial.println(color);
   Serial.print("Яркость = ");  Serial.println(brightness);
   Serial.print("Позиция = ");  Serial.println(adapterPositionName);
  }

  //--------------для бегущих огней-----------------//
  void getArgsForRunning(){
   playspeed = (server.arg(0)); //получаем скорость как строку 
   brightness = server.arg(1);  //получаем яркость  как строку
   runNumber = server.arg(2); 
   color = server.arg(3); 
   adapterPositionName = server.arg(4); //получаем позицию  как строку
   Serial.print("Скорость = ");  Serial.println(playspeed);
   Serial.print("Яркость = ");  Serial.println(brightness);
   Serial.print("Количество точек = ");  Serial.println(runNumber);
   Serial.print("Цвет  = ");  Serial.println(color);
   Serial.print("Позиция = ");  Serial.println(adapterPositionName);
  }

  //---------------------для пламени----------------------//
  void getArgsFlame(){
   brightness = (server.arg(0)); //получаем скорость как строку 
   heightFlame = server.arg(1);  //получаем яркость  как строку
   sparkFlame = server.arg(2); 
   adapterPositionName = server.arg(3); //получаем позицию  как строку
   Serial.print("Яркость = ");  Serial.println(brightness);
   Serial.print("Высота пламени = ");  Serial.println(heightFlame);
   Serial.print("Искры  = ");  Serial.println(sparkFlame);
   Serial.print("Позиция = ");  Serial.println(adapterPositionName);
}

//-------------------послать сообщение клиенту-----------------//
void sendMessage(){
    //сюда попадаем из обработчика каждого эффекта: настройки изменились
    effectDirty = true;
    effectChangedAt = millis();
    restorePending = false;   //пришла команда - сохранённый эффект уже не восстанавливаем
    String message = adapterPositionName;
     Serial.print("Название эффекта: ");
     Serial.println(message);
     server.send(200, "text/plain", message);                    // ответить на HTTP запрос 
}

//-----послать сообщение клиенту с именем сети, к которой подключена ESP ---//
void sendMessageSsid(){
     Serial.print("Имя сети для ESP: ");
     Serial.println(realSsid);
     server.send(200, "text/plain", realSsid);                    // ответить на HTTP запрос 
}

//-----------------записать строку в EEPROM (без commit) -------------------//
// addrLen - адрес длины строки, addrData - адрес первого символа
void writeString(int addrLen, int addrData, const String &str){
  EEPROM.write(addrLen, str.length());
  for (unsigned int i = 0; i < str.length(); i++){
    EEPROM.write(addrData + i, str[i]);
  }
}

//------------прочитать строку из EEPROM -------------------//
// len - длина строки, addrData - адрес первого символа
String readString(int len, int addrData){
  String data = "";
  for (int i = 0; i < len; i++){
    data += char(EEPROM.read(addrData + i));
  }
  return data;
}

//------------сохранить имя, пароль и режим работы -------------------//
void saveConfig(const String &name, const String &password, byte mode){
  writeString(ADDR_SSID_LEN, ADDR_SSID, name);
  writeString(ADDR_PASS_LEN, ADDR_PASS, password);
  EEPROM.write(ADDR_MODE, mode);
  EEPROM.write(ADDR_MAGIC, EEPROM_MAGIC);
  EEPROM.commit();
}

//------------сохранить только режим работы (имя и пароль домашней сети не трогаем) -------//
void saveMode(byte mode){
  EEPROM.write(ADDR_MODE, mode);
  EEPROM.write(ADDR_MAGIC, EEPROM_MAGIC);
  EEPROM.commit();
}

//------------прочитать настройки; при первом запуске новой версии - перенести старые -------//
void loadConfig(){
  if (EEPROM.read(ADDR_MAGIC) != EEPROM_MAGIC){
    //Данные в старом формате (версия 013): длина имени в 0, имя с 1, длина пароля в 31, пароль с 32
    byte oldNameLen = EEPROM.read(0);
    byte oldPassLen = EEPROM.read(31);
    if (oldNameLen >= 1 && oldNameLen <= 30 && oldPassLen <= 32){
      String oldName = readString(oldNameLen, 1);   //сначала читаем обе строки,
      String oldPass = readString(oldPassLen, 32);  //потом пишем: области перекрываются
      if (oldName == ssid){
        //в старой версии так записывалась и своя точка доступа, и неудача подключения -
        //отличить нельзя, поэтому считаем, что данных о домашней сети нет
        Serial.println("EEPROM: старый формат, была точка доступа NodeMCU");
        saveConfig("", "", MODE_NONE);
      } else {
        Serial.println("EEPROM: переносим имя и пароль сети из старого формата");
        saveConfig(oldName, oldPass, MODE_STA);
      }
    } else {
      Serial.println("EEPROM: первое включение, данных нет");
      saveConfig("", "", MODE_NONE);
    }
  }

  byte nameLen = EEPROM.read(ADDR_SSID_LEN);
  byte passLen = EEPROM.read(ADDR_PASS_LEN);
  if (nameLen > MAX_SSID_LEN) nameLen = 0;
  if (passLen > MAX_PASS_LEN) passLen = 0;
  nameEeprom = readString(nameLen, ADDR_SSID);
  passEeprom = readString(passLen, ADDR_PASS);
  wifiMode = EEPROM.read(ADDR_MODE);
  if (wifiMode > MODE_AP_USER) wifiMode = MODE_NONE;
  if (wifiMode == MODE_STA && nameEeprom.length() == 0) wifiMode = MODE_NONE;

  Serial.print("Режим работы: ");  Serial.println(wifiMode);
  Serial.print("Имя сети в EEPROM: ");  Serial.println(nameEeprom);
}

//------------сохранить текущий эффект и его настройки -------------------//
void saveEffect(){
  if (w < FX_FIRST || w > FX_LAST) return;   //огоньки состояния не запоминаем
  EEPROM.write(ADDR_FX_MAGIC, FX_MAGIC);
  EEPROM.write(ADDR_FX_W, w);
  EEPROM.write(ADDR_FX_R, constrain(r, 0, 255));
  EEPROM.write(ADDR_FX_G, constrain(g, 0, 255));
  EEPROM.write(ADDR_FX_B, constrain(b, 0, 255));
  EEPROM.write(ADDR_FX_BRIGHT, constrain(brightness.toInt(), 0, 255));
  long sp = constrain(playspeed.toInt(), 0, 65535);
  EEPROM.write(ADDR_FX_SPEED_HI, (sp >> 8) & 0xFF);
  EEPROM.write(ADDR_FX_SPEED_LO, sp & 0xFF);
  EEPROM.write(ADDR_FX_RUNNUM, constrain(runNumber.toInt(), 0, 255));
  EEPROM.write(ADDR_FX_HEIGHT, constrain(heightFlame.toInt(), 1, 254));
  EEPROM.write(ADDR_FX_SPARK, constrain(sparkFlame.toInt(), 1, 254));
  String name = adapterPositionName;
  if (name.length() > MAX_FX_NAME_LEN) name = "";  //слишком длинное название не храним
  writeString(ADDR_FX_NAME_LEN, ADDR_FX_NAME, name);
  EEPROM.commit();   //пишет во флеш, только если что-то изменилось
  Serial.println("Эффект сохранён");
}

//------------есть ли сохранённый эффект -------------------//
boolean hasSavedEffect(){
  byte savedW = EEPROM.read(ADDR_FX_W);
  return EEPROM.read(ADDR_FX_MAGIC) == FX_MAGIC && savedW >= FX_FIRST && savedW <= FX_LAST;
}

//------------включить сохранённый эффект -------------------//
void restoreEffect(){
  if (!hasSavedEffect()) return;
  r = EEPROM.read(ADDR_FX_R);
  g = EEPROM.read(ADDR_FX_G);
  b = EEPROM.read(ADDR_FX_B);
  brightness = String(EEPROM.read(ADDR_FX_BRIGHT));
  playspeed = String((EEPROM.read(ADDR_FX_SPEED_HI) << 8) | EEPROM.read(ADDR_FX_SPEED_LO));
  runNumber = String(EEPROM.read(ADDR_FX_RUNNUM));
  //0 и 255 - значение не записано (эффект сохранён прежней версией прошивки)
  byte savedHeight = EEPROM.read(ADDR_FX_HEIGHT);
  byte savedSpark = EEPROM.read(ADDR_FX_SPARK);
  if (savedHeight > 0 && savedHeight < 255) heightFlame = String(savedHeight);
  if (savedSpark > 0 && savedSpark < 255) sparkFlame = String(savedSpark);
  byte nameLen = EEPROM.read(ADDR_FX_NAME_LEN);
  if (nameLen > MAX_FX_NAME_LEN) nameLen = 0;
  adapterPositionName = readString(nameLen, ADDR_FX_NAME);
  FastLED.clear();
  w = EEPROM.read(ADDR_FX_W);
  Serial.print("Восстановлен эффект номер ");  Serial.println(w);
}

//------------прочитать количество точек -------------------//
void loadLedNum(){
  ledNum = DEFAULT_LED_NUM;
  if (EEPROM.read(ADDR_LEDS_MAGIC) == LEDS_MAGIC){
    int saved = (EEPROM.read(ADDR_LEDS_HI) << 8) | EEPROM.read(ADDR_LEDS_LO);
    if (saved >= 1 && saved <= MAX_LED_NUM) ledNum = saved;
  }
  Serial.print("Количество точек: ");  Serial.println(ledNum);
}

//------------сохранить количество точек -------------------//
void saveLedNum(int number){
  EEPROM.write(ADDR_LEDS_MAGIC, LEDS_MAGIC);
  EEPROM.write(ADDR_LEDS_HI, (number >> 8) & 0xFF);
  EEPROM.write(ADDR_LEDS_LO, number & 0xFF);
  EEPROM.commit();
}

//------------запланировать перезагрузку (после ответа телефону) -------------------//
void scheduleRestart(){
  restartPending = true;
  restartAt = millis() + RESTART_DELAY_MS;
}

//==================  варианты от W=1 до W=14     начало  ==========================//

//---------------Бегущий светодиод состояния: один шаг за вызов -----------------//
//За один вызов огонёк сдвигается на одну точку, поэтому лента не "занята" целый проход
//и сразу откликается на команды - это важно на длинной ленте.
void runStatusDot(const CRGB &dotColor){
  static int pos = 0;
  if (pos >= LED_NUM) pos = 0;
  leds[pos] = CRGB::Black;          //гасим точку предыдущего шага
  pos++;
  if (pos >= LED_NUM) pos = 0;
  leds[pos] = dotColor;
  FastLED.delay(20);
}

//---------------Бегущий светодиод красного цвета w=0 -----------------//
void getRed(){
  runStatusDot(CRGB::Red);
}
//---------------Бегущий светодиод синего цвета w=1 -----------------//
void getTurquoise(){
  runStatusDot(CRGB::Turquoise);
}

//---------------Бегущий светодиод малинового цвета w=2 -----------------//
void getMagenta(){
  runStatusDot(CRGB::Magenta);
}

//--------------Плавные переливы w = 3-----------------//
void getPlayOfColours(){
     for (int i = 0; i < LED_NUM; i++){
        leds[i].setHue(counter); 
     }
     counter++;                                              // counter меняется от 0 до 255 (тип данных byte)
     
     FastLED.setBrightness(brightness.toInt());
     FastLED.delay(playspeed.toInt());                           // скорость движения перелива 
}
 
 void  handlePlayOfColours(){ 
   w = 3;
   getArgsSpeedBrigthness();                                      //получаем скорость и яркость из аргументов
   sendMessage();                                                 //посылаем сообщение клиенту
  }

//---------------Установка яркости для статических цветов w = 4--------------//
void getStaticColors() {
  for (int i = 0; i < LED_NUM; i++){
       leds[i].setRGB( r, g, b);
      }
      FastLED.setBrightness(brightness.toInt());
      FastLED.delay(30);
 }

  void handleStatic(){
   w = 4;                                                     // устанавливаем номер варианта свечения ленты 
   getArgsColorBrigthness();                                      //получаем цвет и яркость из аргументов
   
  // Избавьтесь от «0x» и преобразуйте в целое число
  int number = (int) strtol( &color[2], NULL, 16);              //с третьего символа

  // Разделите их на значения r, g, b
  r = number >> 16;
  g = number >> 8 & 0xFF;
  b = number & 0xFF;
  
  Serial.print("r= ");Serial.println(r);
  Serial.print("g= ");Serial.println(g);
  Serial.print("b= ");Serial.println(b);
  
  sendMessage();                                           //отправляем клиенту ответ сервера
 }

 //--------------------------Пламя w = 5------------------//
// COOLING: ОХЛАЖДЕНИЕ: Насколько воздух охлаждается при подъеме?
//Меньшее охлаждение = более высокое пламя. Больше охлаждения = короче пламя.
//По умолчанию 50, рекомендуемый диапазон 20-100   -// heightFlame
#define COOLING  55

// SPARKING: SPARKING: Каков шанс (из 255), что зажжется новая искра?
//Более высокий шанс = больше ревущего огня. Низкий шанс = больше мерцающего огня. 
//По умолчанию 120, рекомендуемый диапазон 50-200. -// sparkFlame
#define SPARKING 120

bool gReverseDirection = false;
void Fire2012()
{
// Array of temperature readings at each simulation cell
  static uint8_t heat[MAX_LED_NUM];

  //Настройки из приложения. Высота пламени 5..100: чем она больше, тем слабее охлаждение
  //(меньше охлаждение = выше пламя). Искры 5..200: шанс новой вспышки из 255.
  int cooling = constrain(105 - heightFlame.toInt(), 20, 100);
  int sparking = constrain(sparkFlame.toInt(), 5, 200);
  int coolMax = constrain(((cooling * 10) / LED_NUM) + 2, 2, 255);

  // Шаг 1. Немного охладите каждую ячейку 
    for( int i = 0; i < LED_NUM; i++) {
      heat[i] = qsub8( heat[i],  random8(0, coolMax));
    }
  
    // Шаг 2. Тепло от каждой ячейки дрейфует «вверх» и немного рассеивается
    for( int k= LED_NUM - 1; k >= 2; k--) {
      heat[k] = (heat[k - 1] + heat[k - 2] + heat[k - 2] ) / 3;
    }
    
    // Шаг 3. Случайным образом зажечь новые «искры» тепла внизу
    if( random8() < sparking ) {
      int y = random8(7);
      heat[y] = qadd8( heat[y], random8(160,255) );
    }

    //Шаг 4. Сопоставление тепловых ячеек с цветами светодиодов
    for( int j = 0; j < LED_NUM; j++) {
      CRGB color = HeatColor( heat[j]);
      int pixelnumber;
      if( gReverseDirection ) {
        pixelnumber = (LED_NUM-1) - j;
      } else {
        pixelnumber = j;
      }
      leds[pixelnumber] = color;
    }
      FastLED.setBrightness(brightness.toInt());
      FastLED.delay(17); 
    }

void handleFlame(){
  w = 5;
  getArgsFlame();   //яркость, высота пламени, искры, название эффекта
  sendMessage();
}

//----------------------радуга с затуханием w = 6--------------------//
void pride() 
{
  static uint16_t sPseudotime = 0;
  static uint16_t sLastMillis = 0;
  static uint16_t sHue16 = 0;
 
  uint8_t sat8 = beatsin88( 87, 220, 250);
  uint8_t brightdepth = beatsin88( 341, 96, 224);
  uint16_t brightnessthetainc16 = beatsin88( 203, (25 * 256), (40 * 256));
  uint8_t msmultiplier = beatsin88(147, 23, 60);

  uint16_t hue16 = sHue16;//gHue * 256;
  uint16_t hueinc16 = beatsin88(113, 1, 3000);
  
  uint16_t ms = millis();
  uint16_t deltams = ms - sLastMillis ;
  sLastMillis  = ms;
  sPseudotime += deltams * msmultiplier;
  sHue16 += deltams * beatsin88( 400, 5,9);
  uint16_t brightnesstheta16 = sPseudotime;
  
  for( uint16_t i = 0 ; i < LED_NUM; i++) {
    hue16 += hueinc16;
    uint8_t hue8 = hue16 / 256;

    brightnesstheta16  += brightnessthetainc16;
    uint16_t b16 = sin16( brightnesstheta16  ) + 32768;

    uint16_t bri16 = (uint32_t)((uint32_t)b16 * (uint32_t)b16) / 65536;
    uint8_t bri8 = (uint32_t)(((uint32_t)bri16) * brightdepth) / 65536;
    bri8 += (255 - brightdepth);
    
    CRGB newcolor = CHSV( hue8, sat8, bri8);
    
    uint16_t pixelnumber = i;
    pixelnumber = (LED_NUM-1) - pixelnumber;
    
    nblend( leds[pixelnumber], newcolor, 64);
  }

  FastLED.setBrightness(brightness.toInt());
  FastLED.delay(playspeed.toInt()); // скорость движения радуги
}

  void handlePride(){
   w = 6;
   getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
   sendMessage();
  }

//------------------------------радуга w = 7--------------------------//
void  getRainbow(){
    for (int i = 0; i < LED_NUM; i++) {
      leds[i].setHue(counter + i * 255 / LED_NUM);  // Hue из HSV, S и V будут 255
      }
      counter++;        // counter меняется от 0 до 255 (тип данных byte)      
      FastLED.setBrightness(brightness.toInt());
      FastLED.delay(playspeed.toInt()); // скорость движения радуги
}

 void handleRainbow(){
  w = 7;
  getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
  sendMessage();
 }

//------------------------------Случайные цвета w = 8--------------------------//
void getRandom(){
  idex = random(0, LED_NUM);
  ihue = random(0, 255);
  leds[idex] = CHSV(ihue, 255, 255);
   FastLED.setBrightness(brightness.toInt());
   FastLED.delay(playspeed.toInt());
}

 void handleRandom(){
   w = 8;
   getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
   sendMessage();
 }

//---------------------Радуга со смещением w = 9--------------------------//
 void getRainbowBias(){
  idex++;
  ihue = ihue + 10;
  if (idex >= LED_NUM) {
    idex = 0;
  }
  if (ihue > 255) {
    ihue = 0;
  }
  leds[idex] = CHSV(ihue, 255, 255); 
     
  FastLED.setBrightness(brightness.toInt());
  FastLED.delay(playspeed.toInt());
 }

 void handleRainbowBias(){
   w = 9;
   getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
   sendMessage();
 }

 //---------------------Случайные цвета с затуханием w = 10--------------------------//
 void getRandomFade(){
    fadeToBlackBy( leds, LED_NUM, 10);
    int pos = random16(LED_NUM);
    leds[pos] += CHSV( random8(255), 200, 255);
    
    FastLED.setBrightness(brightness.toInt());
    FastLED.delay(playspeed.toInt());
 }

void handleRandomFade(){
   w = 10;
   getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
   sendMessage();
 }

//-----------------радуга с блёстками W=11---------------
 void getRainbowFlash(){
   for (int i = 0; i < LED_NUM; i++) {
      leds[i].setHue(counter + i * 255 / LED_NUM);
    }
    if ( random8() < 150) {
      leds[ random16(LED_NUM) ] += CRGB::White;
    }
    counter++;        // counter меняется от 0 до 255 (тип данных byte)
    
    FastLED.setBrightness(brightness.toInt());
    FastLED.delay(playspeed.toInt());
  }

 void handleRainbowFlash(){
   w = 11;
   getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
   sendMessage();
 }

 //------------Переливы цвета с блёстками w = 12--------------
void getPlayOfColoursFlash(){
   for (int i = 0; i < LED_NUM; i++) {
      leds[i].setHue(counter);
    }
    if ( random8() < 150) {
      leds[ random16(LED_NUM)] += CRGB::White;
    }
    counter++;
    
    FastLED.setBrightness(brightness.toInt());
    FastLED.delay(playspeed.toInt());
}
 
  void handlePlayOfColoursFlash(){
   w = 12;
   getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
   sendMessage();
 }

//------------------северное сияние w = 13 ----------------//
 void getNorthernLights(){
  uint8_t octaves = 1;
  uint16_t x = 0;
  int scale = 100;
  uint8_t hue_octaves = 1;
  uint16_t hue_x = 1;
  int hue_scale = 50;
  uint16_t ntime = millis() / 3;
  uint8_t hue_shift = 5;
  fill_noise16 (leds, LED_NUM, octaves, x, scale, hue_octaves, hue_x, hue_scale, ntime, hue_shift);
  
  FastLED.setBrightness(brightness.toInt()); 
  FastLED.delay(playspeed.toInt());
 }

void handleNorthernLights(){
   w = 13;
   getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
   sendMessage();
}

//------------------комета w = 15 ----------------//
//яркая точка с затухающим хвостом бежит по ленте, цвет плавно меняется
void getComet(){
  static int pos = 0;
  static uint8_t hue = 0;
  fadeToBlackBy(leds, LED_NUM, 48);          //хвост: всё, что зажжено раньше, тускнеет
  pos++;
  if (pos >= LED_NUM) pos = 0;
  leds[pos] = CHSV(hue, 255, 255);
  if (pos > 0) leds[pos - 1] += CHSV(hue, 200, 120);  //"голова" из двух точек
  hue++;

  FastLED.setBrightness(brightness.toInt());
  FastLED.delay(playspeed.toInt());
}

void handleComet(){
  w = 15;
  getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
  sendMessage();
}

//------------------океан w = 16 ----------------//
//медленные сине-зелёные волны с редкими светлыми гребнями
void getOcean(){
  static uint16_t t = 0;
  t += 6;                                     //течение времени: сдвиг волн за один кадр
  for (int i = 0; i < LED_NUM; i++) {
    uint8_t wave = inoise8(i * 35, t);                 //крупные волны
    uint8_t ripple = inoise8(i * 90 + 7000, t * 2);    //мелкая рябь поверх
    uint8_t colorIndex = wave + ripple / 3;
    uint8_t bright = qadd8(scale8(wave, 180), 60);     //не темнее 60, чтобы не было провалов
    leds[i] = ColorFromPalette(OceanColors_p, colorIndex, bright, LINEARBLEND);
    if (wave > 205) {                                  //гребень волны - немного белого
      uint8_t foam = (wave - 205) * 3;
      leds[i] += CRGB(foam, foam, foam);
    }
  }

  FastLED.setBrightness(brightness.toInt());
  FastLED.delay(playspeed.toInt());
}

void handleOcean(){
  w = 16;
  getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
  sendMessage();
}

//------------------мерцающие звёзды w = 17 ----------------//
//точки тёплого белого и золотистого цвета загораются в случайных местах и плавно гаснут
void getStars(){
  fadeToBlackBy(leds, LED_NUM, 12);            //все звёзды понемногу гаснут
  //чем длиннее лента, тем больше звёзд зажигается за кадр
  int tries = 1 + LED_NUM / 60;
  for (int k = 0; k < tries; k++) {
    if (random8() < 70) {
      int pos = random16(LED_NUM);
      //оттенок от тёплого оранжевого до жёлтого, насыщенность разная: от золотого до белого
      leds[pos] = CHSV(random8(24, 44), random8(60, 200), 255);
    }
  }

  FastLED.setBrightness(brightness.toInt());
  FastLED.delay(playspeed.toInt());
}

void handleStars(){
  w = 17;
  getArgsSpeedBrigthness();//получаем скорость и яркость из аргументов
  sendMessage();
}

//------------------бегущие огни w = 14 ----------------//
//один шаг за вызов: группа из (runNum + 1) точек сдвигается на одну точку
void getRunningLights(){
  static int pos = 0;
  int runNum = runNumber.toInt();
  if (runNum < 0) runNum = 0;
  if (runNum > LED_NUM - 1) runNum = LED_NUM - 1;
  int lastStart = LED_NUM - runNum - 1;           //крайнее положение начала группы

  //гасим группу в прежнем положении
  if (pos > lastStart) pos = lastStart;
  for (int k = 0; k <= runNum; k++) { leds[pos + k] = CRGB::Black; }
  //сдвигаем
  pos++;
  if (pos > lastStart) pos = 0;
  //зажигаем в новом положении
  for (int k = 0; k <= runNum; k++) { leds[pos + k].setRGB(r, g, b); }

  FastLED.setBrightness(brightness.toInt());
  FastLED.delay(playspeed.toInt());
}

void handleRunningLights(){
   w = 14;
   getArgsForRunning();
   
   // Избавьтесь от «0x» и преобразуйте в целое число
  int number = (int) strtol( &color[2], NULL, 16);  //с третьего символа

  // Разделите их на значения r, g, b
  r = number >> 16;
  g = number >> 8 & 0xFF;
  b = number & 0xFF;
  
  Serial.print("r= ");Serial.println(r);
  Serial.print("g= ");Serial.println(g);
  Serial.print("b= ");Serial.println(b);
  
  sendMessage();
}

//---------привязка к домашней или иной WiFi сети: /home?name=...&password=... ------------//
//сохраняем имя и пароль, отвечаем телефону и перезагружаемся - подключение идёт при запуске
void handleHome(){
  //приложение передаёт параметры с именами name и password; по номеру - для совместимости
  String newSsid = server.hasArg("name") ? server.arg("name") : server.arg(0);
  String newPass = server.hasArg("password") ? server.arg("password") : server.arg(1);
  Serial.print("Имя сети = ");  Serial.println(newSsid);

  if (newSsid.length() == 0 || newSsid.length() > MAX_SSID_LEN || newPass.length() > MAX_PASS_LEN){
    server.send(400, "text/plain", "Wrong name or password length");
    return;
  }
  saveConfig(newSsid, newPass, MODE_STA);
  server.send(200, "text/plain", "OK");
  scheduleRestart();
}

//---------создание собственной WiFi сети - точка доступа: /hotpoint ------------//
//имя и пароль домашней сети остаются в памяти, меняется только режим работы
void handleHotpoint(){
  saveMode(MODE_AP_USER);
  server.send(200, "text/plain", "OK");
  scheduleRestart();
}

//---------количество точек на ленте: /leds или /leds?number=N ------------//
// без параметра - отвечает "текущее/максимум"; с параметром - сохраняет и перезагружается
void handleLeds(){
  if (!server.hasArg("number")){
    server.send(200, "text/plain", String(ledNum) + "/" + String(MAX_LED_NUM));
    return;
  }
  long number = server.arg("number").toInt();
  if (number < 1 || number > MAX_LED_NUM){
    server.send(400, "text/plain", String("Wrong number, max ") + String(MAX_LED_NUM));
    return;
  }
  //Гасим всю ленту в прежней длине. Светодиоды помнят последний цвет, пока есть питание:
  //если точек станет меньше, остальные продолжали бы гореть тем, что на них было.
  if (effectDirty) { saveEffect(); effectDirty = false; }  //текущий эффект вернётся после запуска
  restorePending = false;
  w = 100;            //эффекты до перезагрузки не рисуем, чтобы лента осталась погашенной
  FastLED.clear();
  FastLED.show();

  saveLedNum((int)number);
  server.send(200, "text/plain", "OK");
  scheduleRestart();   //новое количество точек применяется при запуске
}

//------------отправка Названия сети, к которой подключена ESP ------------------//
void handleSsidEsp(){
  sendMessageSsid(); //посылаем сообщение клиенту
}

//------------отправка позиции по запросу для синхронизации нескольких телефонов------------------//
void handlePosition(){
  //отвечаем сами, а не через sendMessage(): это только запрос, настройки не менялись
  server.send(200, "text/plain", adapterPositionName);
}

// -------------подключаемся к WiFi в режиме STA - домашняя сеть -------------//
// возвращает true, если подключились
boolean connectToHomeWiFi(){
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(nameEeprom);

  int countOfPrompts = CONNECT_ATTEMPTS;

  WiFi.mode(WIFI_STA);
  WiFi.begin(nameEeprom.c_str(), passEeprom.c_str());

  while (--countOfPrompts && WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  if (WiFi.status() != WL_CONNECTED){
    Serial.println("Подключиться к сети не удалось");
    return false;
  }
  isHomeNetwork = true;
  realSsid = nameEeprom; //имя сети, к которой подключились
  Serial.println("WiFi connected");
  Serial.println(WiFi.localIP());
  return true;
}

//------------------- создаём точку доступа NodeMCU -------------------//
// сеть NodeMCU с ip адресом 192.168.1.1 и паролем 12345678; в EEPROM ничего не пишем
void startAccessPoint(){
  WiFi.disconnect();                             //не пытаться подключаться к домашней сети
  WiFi.mode(WIFI_AP);                            //только точка доступа
  WiFi.softAPConfig(local_ip, gateway, subnet);  //сначала эта строка, потом softAP
  WiFi.softAP(ssid, pass);
  delay(100);

  isHomeNetwork = false;  //собственная сеть без интернета
  realSsid = ssid;        //имя сети для передачи в телефон

  Serial.println();
  Serial.print("Create Access Point, ssid: ");
  Serial.println(ssid);
}

//=======================Setup====================
void setup() {
  Serial.begin(115200);
  delay(100);
  Serial.println();

  EEPROM.begin(EEPROM_SIZE);
  loadConfig();
  loadLedNum();   //количество точек нужно знать до настройки ленты

  //лента выключена, пока идёт подключение
  FastLED.addLeds<WS2812, LED_PIN, GRB>(leds, LED_NUM).setCorrection(TypicalLEDStrip);
  FastLED.setBrightness(50);
  // Источник питания 2A 5v. Для длинной ленты стоит включить ограничение тока:
  //FastLED.setMaxPowerInVoltsAndMilliamps(5,1800);
  FastLED.clear(); //выключаем всё
  FastLED.show();

  WiFi.persistent(false);   //имя и пароль храним сами в EEPROM, во флеш SDK не пишем

  //выбираем действие в зависимости от режима работы
  if (wifiMode == MODE_STA){
    Serial.println("Заданная WiFi сеть");
    if (connectToHomeWiFi()){
      w = 1;                         //бирюзовый огонёк - в домашней сети
    } else {
      startAccessPoint();            //временно точка доступа; имя и пароль сети сохранены
      fallbackAp = true;
      lastRetry = millis();
      w = 0;                         //красный огонёк
    }
  } else if (wifiMode == MODE_AP_USER){
    Serial.println("Точка доступа NodeMCU по команде пользователя");
    startAccessPoint();
    w = 2;                           //фиолетовый огонёк
  } else {
    Serial.println("Первое включение. Точка доступа NodeMCU");
    startAccessPoint();
    w = 0;                           //красный огонёк
  }

  //в сети (домашней или локальной) через 3 секунды возвращаемся к сохранённому эффекту;
  //красный огонёк оставляем - он сообщает, что сети нет
  if ((w == 1 || w == 2) && hasSavedEffect()){
    restorePending = true;
    restoreAt = millis() + FX_RESTORE_DELAY_MS;
  }

// привязываем  функцию обработчика к URL-пути
  server.on("/static", handleStatic);
  server.on("/play", handlePlayOfColours);
  server.on("/playFlash", handlePlayOfColoursFlash);
  server.on("/flame", handleFlame);
  server.on("/pride", handlePride);
  server.on("/rainbow", handleRainbow);
  server.on("/rainbowBias", handleRainbowBias);
  server.on("/rainbowFlash", handleRainbowFlash);
  server.on("/random", handleRandom);
  server.on("/randomFade", handleRandomFade);
  server.on("/northernLights", handleNorthernLights);
  server.on("/comet", handleComet);
  server.on("/ocean", handleOcean);
  server.on("/stars", handleStars);
  server.on("/runningLights", handleRunningLights);
  server.on("/home", handleHome);
  server.on("/hotpoint", handleHotpoint);
  server.on("/position", handlePosition);
  server.on("/leds", handleLeds);
  server.on("/esp", handleSsidEsp);

  server.begin();
  Serial.println();
  Serial.println("HTTP server started");   // запуск сервера

  //Запускаем ответчик MDNS. Если не запустился - работаем без него:
  //в приложении адрес ленты можно ввести вручную
  if (MDNS.begin("esp8266")) {
    // Добавляем сервис, чтобы его можно было найти по MDNS-SD; порт тот же, что у сервера
    MDNS.addService("http", "tcp", 80);
    Serial.println("mDNS responder started");
  } else {
    Serial.println("Error setting up MDNS responder!");
  }
}

//=======================loop====================
void loop() {
 MDNS.update();
 server.handleClient();    // обработка входящих запросов

 //перезагрузка после смены сети - через секунду после ответа телефону
 if (restartPending && (long)(millis() - restartAt) >= 0) {
   Serial.println("Перезагрузка после смены сети");
   if (effectDirty) saveEffect();   //чтобы после перезагрузки вернуться к текущему эффекту
   delay(100);
   ESP.restart();
 }

 //возвращаемся к сохранённому эффекту через 3 секунды после запуска
 if (restorePending && (long)(millis() - restoreAt) >= 0) {
   restorePending = false;
   restoreEffect();
 }

 //запоминаем эффект через 5 секунд после последней команды
 if (effectDirty && millis() - effectChangedAt > FX_SAVE_DELAY_MS) {
   effectDirty = false;
   saveEffect();
 }

 //Домашняя сеть задана, но при запуске была недоступна. Раз в 3 минуты, если к нашей точке
 //доступа никто не подключен, перезагружаемся и пробуем подключиться снова.
 if (fallbackAp && millis() - lastRetry > RETRY_INTERVAL_MS) {
   lastRetry = millis();
   if (WiFi.softAPgetStationNum() == 0) {
     Serial.println("Повторная попытка подключения к домашней сети");
     delay(100);
     ESP.restart();
   }
 }

 switch(w){
    case  0: getRed(); break;           //точка доступа: первое включение или домашняя сеть недоступна
    case  1: getTurquoise(); break;      //ESP привязано к какой то сети
    case  2: getMagenta(); break;         //ESP переведено пользователем в режим точки доступа
    case  3: getPlayOfColours(); break;
    case  4: getStaticColors(); break;
    case  5: Fire2012(); break;
    case  6: pride(); break;
    case  7: getRainbow(); break;
    case  8: getRandom(); break;
    case  9: getRainbowBias(); break;
    case  10: getRandomFade(); break;
    case  11: getRainbowFlash(); break;
    case  12: getPlayOfColoursFlash(); break;
    case  13: getNorthernLights(); break;
    case  14: getRunningLights(); break;
    case  15: getComet(); break;
    case  16: getOcean(); break;
    case  17: getStars(); break;
 }
 FastLED.show();
}
