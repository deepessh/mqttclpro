package in.dc297.mqttclpro.tasker.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;

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
import tasker.TaskerPlugin;

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
    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public void startActionPublish(final Intent intent) {
        Bundle taskerBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null) {
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
                    Iterator<TopicEntity> topicEntityIterator = topicEntities.iterator();

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
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Toast.makeText(getApplicationContext(), "Unknown error occurred!", Toast.LENGTH_SHORT).show();
                                    throwable.printStackTrace();
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
                        data.update(brokerEntity);
                        mqttClients = MQTTClients.getInstance((MQTTClientApplication) getApplication());
                        mqttClients.doConnect(brokerEntity);
                    }
                });
            }
        }
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
}
