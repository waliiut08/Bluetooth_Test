# Communication between Wireless Devices.


Introduction:
An android based application to communicate between wireless devices through Bluetooth and WiFi. This app is focused to create a wireless network where cellular network is absent or destroyed(in natural disaster). 
So far, I have done the Bluetooth peer-to-peer messaging. Yet to go far away.

---------------------------------------------------------------------------------------------------------------------------------
Technologies and Tools Used:
  - Android Studio
  - Java
  - Concepts of Networking
  - Bluetooth and Wi-Fi Devices
  
---------------------------------------------------------------------------------------------------------------------------------  
Accomplished Tasks:
1) Bluetooth messaging:
        - A discoverable switch-button is used to switch on/off visibility by other bluetooth devices.
        - Find Bluetooth Devices.
        - Create Pairing.
        - Create Network Connection with paired device to send and receive data. Socket programming is applied for these.
        - Multithreading technique is used.
        - Can send and receive text message with the connected device.
        - At the startup the app automatically turns on the Bluetooth and start scaning to find Bluetooth Devices.
        - At the shut down the app automatically turns off Bluetooth and other Bluetooth related acitivities.
        
        
2) Topology Establishment:
        - Can start scanning (search for other bluetooth devices) autonomously periodically.
        - A switch-button is added to switch on/off the auto-scan process.
        
        
