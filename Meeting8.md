# 8th Meeting 10 MAR 2014Spring

# Introduction #
  1. Revise map, pointer implementation
  1. Revise WiFi-localization logic

# Details #
  * Application Server logic revisions:
    * Manipulate rssi data according to SSID/ frequency
    * Adjust threshold value for closest match of RSSI data
    * Set threshold distance when looking for closest match location on server (assume the local location would not be too inaccurate)

  * Mobile app revision:
    * New map including concourse
    * Map-moving implementation