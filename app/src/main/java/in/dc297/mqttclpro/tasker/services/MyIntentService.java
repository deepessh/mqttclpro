package in.dc297.mqttclpro.tasker.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.sql.Timestamp;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.activity.PublishActivity;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.entity.MessageEntity;
import in.dc297.mqttclpro.entity.TopicEntity;
import in.dc297.mqttclpro.mqtt.internal.MQTTClients;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.util.CloseableIterator;

//import android.support.v4.app.JobIntentService;

import static in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BUNDLE;
import static in.dc297.mqttclpro.tasker.activity.Intent.MQTT_CONNECT_ACTION;
import static in.dc297.mqttclpro.tasker.activity.Intent.MQTT_PUBLISH_ACTION;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class MyIntentService extends IntentService {

    public MyIntentService() {
        super("MyIntentService");
    }

    private ReactiveEntityStore<Persistable> data = null;
    private MQTTClients mqttClients = null;

    public static final String CHANNEL_ID = "tasker_channel";
    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public void startActionPublish(final Intent intent) {
        Bundle taskerBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if (taskerBundle != null) {
            final String topic = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC);
            final String message = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE);
            final String qos = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_QOS);
            final boolean retained = taskerBundle.getBoolean(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_RETAINED);
            final Long brokerId = taskerBundle.getLong(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID);

            if (TextUtils.isEmpty(topic) || TextUtils.isEmpty(qos) || message == null || brokerId <= 0) {
                return;
            }
            try {
                MqttTopic.validate(topic, false);
            } catch (IllegalStateException ise) {
                ise.printStackTrace();
                return;
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
                return;
            }

            try {
                MqttMessage.validateQos(Integer.parseInt(qos));
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
                return;
            }
            data = ((MQTTClientApplication) getApplication()).getData();
            mqttClients = MQTTClients.getInstance((MQTTClientApplication) getApplication());
            data.findByKey(BrokerEntity.class, brokerId).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<BrokerEntity>() {
                @Override
                public void accept(final BrokerEntity brokerEntity) throws Exception {
                    Result<TopicEntity> topicEntities = data.select(TopicEntity.class).where(TopicEntity.NAME.eq(topic).and(TopicEntity.TYPE.eq(1).and(TopicEntity.BROKER.eq(brokerEntity)))).get();
                    final CloseableIterator<TopicEntity> topicEntityIterator = topicEntities.iterator();
                    MessageEntity messageEntity = new MessageEntity();
                    messageEntity.setDisplayTopic(topic);
                    messageEntity.setQOS(Integer.valueOf(qos));
                    messageEntity.setPayload(message);
                    messageEntity.setTimeStamp(new Timestamp(System.currentTimeMillis()));
                    messageEntity.setRetained(retained);
                    if (topicEntityIterator.hasNext()) {
                        messageEntity.setTopic(topicEntityIterator.next());
                    } else {
                        TopicEntity topicEntity = new TopicEntity();
                        topicEntity.setBroker(brokerEntity);
                        topicEntity.setQOS(0);//setting to 0 as in case of published message, qos will be set on message level
                        topicEntity.setType(1);
                        topicEntity.setName(topic);
                        messageEntity.setTopic(topicEntity);
                    }
                    data.insert(messageEntity)
                            .subscribeOn(Schedulers.single())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<MessageEntity>() {
                                @Override
                                public void accept(MessageEntity messageEntity) throws Exception {
                                    Log.i(PublishActivity.class.getName(), "Sending " + messageEntity.getId());
                                    mqttClients.publishMessage(brokerEntity, topic, message, Integer.parseInt(qos), retained, messageEntity.getId());
                                    if(topicEntityIterator!=null) topicEntityIterator.close();
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Toast.makeText(getApplicationContext(), "Unknown error occurred!", Toast.LENGTH_SHORT).show();
                                    throwable.printStackTrace();
                                    if(topicEntityIterator!=null) topicEntityIterator.close();
                                }
                            });
                }
            });
        }
    }

    private void startConnectAction(Intent intent){
        Bundle taskerBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null){
            Long brokerId = taskerBundle.getLong(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID, 0);

            Log.i(MyIntentService.class.getName(),"Our broker id is " + brokerId);
            if(brokerId>0){
                data = ((MQTTClientApplication)getApplication()).getData();
                data.select(BrokerEntity.class).where(BrokerEntity.ID.eq(brokerId)).get().each(new io.requery.util.function.Consumer<BrokerEntity>() {
                    @Override
                    public void accept(BrokerEntity brokerEntity) {
                        brokerEntity.setEnabled(true);
                        try{
                            data.update(brokerEntity);
                            mqttClients = MQTTClients.getInstance((MQTTClientApplication) getApplication());
                            mqttClients.doConnect(brokerEntity);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(2000,showNotification());
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (MQTT_PUBLISH_ACTION.equals(action)) {
                startActionPublish(intent);
            }
            else if(MQTT_CONNECT_ACTION.equals(action)){
                startConnectAction(intent);
            }
        }
    }

    private Notification showNotification(){
        createNotificationChannel();
        Notification.Builder builder = null;
        int resourceId = R.drawable.ic_notifications_black_24dp;

        if(Build.VERSION.SDK_INT<=19) resourceId = R.mipmap.ic_launcher;//fix for kitkat

        builder = new Notification.Builder(getApplicationContext());

        builder.setSmallIcon(resourceId)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setAutoCancel(false)
                .setContentText("Handling tasker action");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        Notification notification = builder.getNotification();

        notification.flags |= Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONGOING_EVENT;
        //notification.priority = getNotificationPriority();

        return notification;

    }

    private void removeNotification(){
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        notificationManager.cancel(2000);
        stopForeground(true);
    }

    @Override
    public void onDestroy(){
        removeNotification();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name_mqtt_tasker);
            String description = getString(R.string.channel_description_mqtt_tasker);
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
