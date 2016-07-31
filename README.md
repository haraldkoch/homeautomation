[![Build Status](https://travis-ci.org/haraldkoch/homeautomation.svg?branch=master)](https://travis-ci.org/haraldkoch/homeautomation)
[![Dependencies Status](http://jarkeeper.com/haraldkoch/homeautomation/status.png)](http://jarkeeper.com/haraldkoch/homeautomation)
[![Coverage Status](https://coveralls.io/repos/haraldkoch/homeautomation/badge.svg?branch=master&service=github)](https://coveralls.io/github/haraldkoch/homeautomation?branch=master)

# homeautomation

Powered by [Luminus](http://www.luminusweb.net/).

This is a place for me to play with home automation technologies using
Clojure. There are many exciting technologies floating around right now;
[MQTT][mqtt], [homA][homa], [openHAB][openhab], [Node-Red][nodered],
[Raspberry Pis][rpi], and so many more.

Right now I'm experimenting with mobile-based presence. On my home network
is an [ELK][elk] stack collecting log data, including the logs from my
[OpenWRT][openwrt]-based network access point. The AP logs contain status
updates for WiFi clients. I use [logstash][logstash] to extract relevant
information from those log messages and publish them to an MQTT topic.

This application then receives those status updates, tracks device
presence, and turns device presence into user presence events, also
distributed via MQTT. Back in the network, there is a process that
receives events over MQTT and publishes them to my phone using
[Instapush][instapush] and/or [Pushbullet][pushbullet].

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run

## License

Copyright © 2015-2016 C. Harald Koch.

Distributed under the [MIT License](http://opensource.org/licenses/MIT).

[mqtt]: <http://mqtt.org/>
[homa]: <https://github.com/binarybucks/homA>
[openhab]: <http://www.openhab.org/>
[nodered]: <http://nodered.org/>
[rpi]: <https://www.raspberrypi.org/>
[elk]: <https://www.elastic.co/products>
[openwrt]: <https://openwrt.org/>
[logstash]: <https://www.elastic.co/products/logstash>
[instapush]: <https://instapush.im/>
[pushbullet]: <https://www.pushbullet.com>
