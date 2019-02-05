# mqttclpro 

https://play.google.com/store/apps/details?id=in.dc297.mqttclpro

MQTT Client for android with tasker integration

Connect to any MQTT v3.1 broker and subscribe to topics. Perform Tasker actions on message publish or publish messages on tasker events. The app still might be rough around the edges.

Tasker Integration works as follows:

-To publish messages from tasker simply create a action in tasker. The configuration is pretty simple. You may use tasker variables in topic and message.

-To perform action whenever a message is received. Create a event in tasker from the plugin. Currently if any message is published on the topic configured, the event will be fired. need feedback on this as to how you guys want this to be implemented.

-Add/Remove topics easily!

-Messages are automatically saved!

-SSL support (Experimental)

-Publish Messages

-No notifications as of now. Coming soon!

Planned features: Full SSL support

Notifications

Better tasker integration according to the feedback from you guys

Feedback is highly appreciated!

If you found the application helpful. Please consider buying me a beer.

Special thanks to: Dale Lane - http://dalelane.co.uk/blog/?p=1599 (This was my inspiration)

StackOverflow Community :D

FilePicker - https://github.com/Angads25/android-filepicker

SSL Support - https://gist.github.com/sharonbn/4104301

## Interfacing with other apps

You may use the below code in your app to send actions to the app.

**Connecting to a broker**

`String innerAction = "in.dc297.mqttclpro.tasker.activity.Intent.MQTT_CONNECT_ACTION";`

`//the following numeric value can be read in the interface of the app next to`

`//the broker nickname and host name`

`long brokerId = 1;`

`Bundle b = new Bundle();`

`b.putLong("in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID",brokerId);`

`b.putString("in.dc297.mqttclpro.tasker.activity.intent.ACTION_OPERATION",innerAction);`

`Intent intent = new Intent("com.twofortyfouram.locale.intent.action.FIRE_SETTING");`

`intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE",b);`

`intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);`

`context.sendBroadcast(intent);`

**Publishing a message**

`String innerAction = "in.dc297.mqttclpro.tasker.activity.Intent.MQTT_PUBLISH_ACTION";`

`//the following numeric value can be read in the interface of the app next to`

`//the broker nickname and host name`

`long brokerId = 1;`

`String topic = "sample/topic";`

`String message = "this is a message";`

`boolean retained = false;`

`//qos as String`

`String qos = "0";`

`Bundle b = new Bundle();`

`b.putLong("in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID",brokerId);`

`b.putString("in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC",topic);`

`b.putString("in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE",message);`

`b.putString("in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_QOS",qos);`

`b.putBoolean("in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_RETAINED",retained);`

`b.putString("in.dc297.mqttclpro.tasker.activity.intent.ACTION_OPERATION",innerAction);`

`Intent intent = new Intent("com.twofortyfouram.locale.intent.action.FIRE_SETTING");`

`intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE",b);`

`intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);`

`context.sendBroadcast(intent);`
