# Technical information about obtaining GPS reference point at an outdoor position

# Introduction #
As a start of the indoor-positioning system project, we decided to implement the indoor navigation by using motion sensor on a outdoor reference location.

This outdoor reference location would be obtained by using GPS on the Android device.

# Details #
Firstly we will need to have one pre-measured outdoor reference location just near the indoor area (for example: the entrance). As we cannot expect all users will enter the indoor area from that pre-measured outdoor reference location, we need to create a real-time calculated reference position based on the information we already have for the pre-measured location.

Let P<sub>ref</sub> be the pre-measured location, P<sub>user</sub> be the real-time calculated outdoor position for the user.

Using the latitude, longitude values obtained from GPS, we can calculate the distance and bearing between P<sub>ref</sub> and P<sub>user</sub>.
See: http://www.movable-type.co.uk/scripts/latlong.html


# Notes #
  1. In our implementation, GPS (latitude, longitude) values are used in calculation ONLY IF the accuracy of the measurement is within 5 meters radius in a 68% confidence level.