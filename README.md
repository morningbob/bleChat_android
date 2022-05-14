# BLE Chat

&nbsp;

## BLE Chat Android Mobile App

&nbsp;
&nbsp;

### BLE Chat is an app that uses Bluetooth Low Energy technology to send and receive messages.  The user can scan for devices nearby, which also has the app installed and running.  The app also let the user send file via Bluetooth using Android's internal bluetooth sharing capabilities.  

&nbsp;

### The app only allows the user to have one chat at a time.  Bluetooth performs the best when connected to one device and communicate with it.  When the user is in a chat, the device automatically shuts down the server to stop listening to incoming connection.  When the chat is over, the user can choose to start the server again.

&nbsp;

### The app only use Bluetooth Low Energy to communicate.  It doesn't use Bluetooth normal.  So, it only available for device with BLE in Google Play.  

&nbsp;

### The app provides peer to peer chats.  The communication is peer to peer.  There is no central server involved.  

&nbsp;

### The app keeps all the chat records in the Room database.  The messages are recorded according the the peer device record.  The user can choose a device and read the records.  

&nbsp;

<img src=".\images\01_BLEChat_android.jpg" alt="application home screenshot" style="width:250px; margin-left: auto; margin-right: auto; display: block;" />

&nbsp;
<center> Home screen </center>
&nbsp;
&nbsp;

<img src=".\images\02_BLEChat_android.jpg" alt="chat screenshot" style="width:250px; margin-left: auto; margin-right: auto; display: block;" />

&nbsp;
<center> Chat screen, the user can choose to turn on notification to receive info when the peer message the user.  The user can tap the notification and be redirected to the chat view.  </center>

&nbsp;
<center> The blue area shows the info about the peer and the connection status. </center>

&nbsp;
<center> The app verifies if the message is successfully sent.  If the message is verified, there will be a logo besides it. </center>
&nbsp;
&nbsp;

<img src=".\images\03_BLEChat_android.jpg" alt="chat screenshot" style="width:250px; margin-left: auto; margin-right: auto; display: block;" />

&nbsp;
<center> The user can disconnect the peer device anytime during the chat.  "Disconnected" will be shown in the blue area.  </center>

&nbsp;
<center> The user can also send any kind of file via bluetooth.  </center>

&nbsp;
&nbsp;

<img src=".\images\05_BLEChat_android.jpg" alt="chat screenshot" style="width:250px; margin-left: auto; margin-right: auto; display: block;" />

&nbsp;
<center> Chat records  </center>

&nbsp;
&nbsp;

## Programming Style

&nbsp;

1. The app is written in Kotlin.  It follows the best practice of Android App architecture.

2. All the layouts use data binding.

3. The devices list and the messages list are built by using recyclerview.  

4. I use devices view model and messages view model to hold all the information of devices and messages to persist across views.  

5. I built Room database to hold devices entity and messages entity.  Every message has a device address field which is the id of the device entity.  So, I can search all the messages that has the same device address.

6. I built notification channel and notification to show the user a notification with the peer message received.  The user can tap on the message and be redirected to the chat view of the app.  But I also allow the user to turn off the notification service.

7. I also built a service and registered that service in Android Manifest to handle the operation of the notification I mentioned in point 6.  

8. When the user sends a message to the peer, I attach a cofirmation code at the end of the message.  The peer's app will extract the confirmation code and send it back to the user's app.  So, the user's app can confirm the delivery of the message is successful or not.  A "sent" logo will appear if the message is confirmed.

9. I didn't add build the layouts for different screen sizes yet.  I will add them soon.  Thank you very much!

&nbsp;
## All right reserved
