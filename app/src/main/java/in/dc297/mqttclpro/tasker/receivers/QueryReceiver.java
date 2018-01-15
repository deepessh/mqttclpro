package in.dc297.mqttclpro.tasker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.MQTTClientApplication;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.entity.MessageEntity;
import in.dc297.mqttclpro.helpers.ComparatorHelper;
import in.dc297.mqttclpro.mqtt.internal.Util;
import in.dc297.mqttclpro.tasker.Constants;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;
import tasker.TaskerPlugin;

import static in.dc297.mqttclpro.tasker.Constants.LOG_TAG;
import static in.dc297.mqttclpro.tasker.activity.Intent.CONNECTION_LOST;
import static in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BUNDLE;
import static in.dc297.mqttclpro.tasker.activity.Intent.MESSAGE_ARRIVED;
import static in.dc297.mqttclpro.tasker.activity.Intent.RECONNECTED;

/**
 * Created by Deepesh on 10/26/2017.
 */

public class QueryReceiver extends BroadcastReceiver {

    ReactiveEntityStore<Persistable> data = null;
    @Override
    public void onReceive(Context context, Intent intent) {

        if (!in.dc297.mqttclpro.tasker.activity.Intent.ACTION_QUERY_CONDITION.equals(intent.getAction()))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(LOG_TAG,
                        String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        data = ((MQTTClientApplication)context.getApplicationContext()).getData();
        if(data==null){
            setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
            return;
        }

        String intentOperation = intent.getBundleExtra(EXTRA_BUNDLE)!=null?intent.getBundleExtra(EXTRA_BUNDLE).getString(in.dc297.mqttclpro.tasker.activity.Intent.QUERY_OPERATION):"";

        if(intentOperation==null){
            setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
            return;
        }

        Log.i(QueryReceiver.class.getName(),"Received query with operation: "+intentOperation);

        switch(intentOperation){
            case MESSAGE_ARRIVED:
                checkReceivedMessage(intent, context);
                break;
            case CONNECTION_LOST:
                checkConnectionLost(intent, context);
                break;
            case RECONNECTED:
                checkReconnected(intent, context);
                break;
            default:
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                break;
        }
    }

    private void checkConnectionLost(Intent intent, Context context){
        Bundle taskerBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null){
            Long brokerId = taskerBundle.getLong(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID, 0);
            int taskerPassThroughId = TaskerPlugin.Event.retrievePassThroughMessageID(intent);

            Log.i(QueryReceiver.class.getName(),"Our broker id is " + brokerId + " and our tasker pass thru id is " + taskerPassThroughId);

            if(taskerPassThroughId<=0){
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                return;
            }
            if(brokerId>0){
                List<BrokerEntity> brokerEntityList = data.select(BrokerEntity.class).where(BrokerEntity.TASKER_PASS_THROUGH_ID.eq(taskerPassThroughId)).get().toList();
                if(brokerEntityList.size()<=0){
                    Log.i(QueryReceiver.class.getName(),"No such brokers found with matching tasker id");
                    setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                    return;
                }

                if(brokerEntityList.get(0).getId()!=brokerId){
                    Log.i(QueryReceiver.class.getName(),"Broker ids dont match");
                    setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                    return;
                }
                Log.i(QueryReceiver.class.getName(),"Query success");
                data.update(BrokerEntity.class)
                        .set(BrokerEntity.TASKER_PASS_THROUGH_ID,0)
                        .where(BrokerEntity.TASKER_PASS_THROUGH_ID.eq(taskerPassThroughId))
                        .get()
                        .single()
                        .subscribeOn(Schedulers.single())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();;
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_SATISFIED);
            }
        }
    }

    private void checkReconnected(Intent intent, Context context){
        Bundle taskerBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null){
            Long brokerId = taskerBundle.getLong(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID, 0);
            int taskerPassThroughId = TaskerPlugin.Event.retrievePassThroughMessageID(intent);

            Log.i(QueryReceiver.class.getName(),"Our broker id is " + brokerId + " and our tasker pass thru id is " + taskerPassThroughId);

            if(taskerPassThroughId<=0){
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                return;
            }
            if(brokerId>0){
                List<BrokerEntity> brokerEntityList = data.select(BrokerEntity.class).where(BrokerEntity.TASKER_PASS_THROUGH_ID.eq(taskerPassThroughId)).get().toList();
                if(brokerEntityList.size()<=0){
                    Log.i(QueryReceiver.class.getName(),"No such brokers found with matching tasker id");
                    setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                    return;
                }

                if(brokerEntityList.get(0).getId()!=brokerId){
                    Log.i(QueryReceiver.class.getName(),"Broker ids dont match");
                    setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                    return;
                }
                Log.i(QueryReceiver.class.getName(),"Query success");
                data.update(BrokerEntity.class)
                        .set(BrokerEntity.TASKER_PASS_THROUGH_ID,0)
                        .where(BrokerEntity.TASKER_PASS_THROUGH_ID.eq(taskerPassThroughId))
                        .get()
                        .single()
                        .subscribeOn(Schedulers.single())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();;
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_SATISFIED);
            }
        }
    }

    private void checkReceivedMessage(Intent intent, Context context){

        String topic = "";
        String message = "";
        String topicVar = "";
        long brokerId = 0;
        int taskerMessageId = 0;
        int topicComparator = 0;
        int messageComparator = 0;
        String topicToCompare = "";
        String messageToCompare = "";

        Bundle taskerBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if(taskerBundle!=null) {
            topic = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC);
            message = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE);
            topicVar = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC_VAR);
            brokerId = taskerBundle.getLong(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_BROKER_ID);
            topicComparator = taskerBundle.getInt(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC_COMPARATOR);
            messageComparator = taskerBundle.getInt(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE_COMPARATOR);
            topicToCompare = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_TOPIC_COMPARE_TO);
            messageToCompare = taskerBundle.getString(in.dc297.mqttclpro.tasker.activity.Intent.EXTRA_MESSAGE_COMPARE_TO);
        }

        if(TextUtils.isEmpty(topic) || TextUtils.isEmpty(message) || TextUtils.isEmpty(topicVar) || brokerId==0) {//if any of them is still null, we return failure
            setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
            return;
        }

        taskerMessageId = TaskerPlugin.Event.retrievePassThroughMessageID(intent);

        if(taskerMessageId<=0){
            setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
            return;
        }

        Log.i(QueryReceiver.class.getName(),"Received query with message ID" + taskerMessageId);

        try{
            List<MessageEntity> messageEntityList =  data.select(MessageEntity.class).where(MessageEntity.TASKER_ID.eq(taskerMessageId)).get().toList();
            if(messageEntityList ==null || messageEntityList.size()!=1){
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                return;
            }

            MessageEntity messageEntity = messageEntityList.get(0);
            if(messageEntity==null){
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNKNOWN);
                return;
            }
            String publishedMessage = messageEntity.getPayload();
            String publishedTopic = messageEntity.getDisplayTopic();


            if(!Util.mosquitto_topic_matches_sub(topic, publishedTopic)){
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNSATISFIED);
                return;
            }

            long publishedBrokerId = messageEntity.getTopic().getBroker().getId();

            if(publishedBrokerId!=brokerId){
                setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNSATISFIED);
                return;
            }

            String[] comparators = context.getResources().getStringArray(R.array.comparators_array_method);

            if(!TextUtils.isEmpty(topicToCompare)){
                if(!(Boolean)getCustomValue(comparators[topicComparator],publishedTopic,topicToCompare)){
                    setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNSATISFIED);
                    return;
                }
            }

            if(!TextUtils.isEmpty(messageToCompare)){
                if(!(Boolean)getCustomValue(comparators[messageComparator],publishedMessage,messageToCompare)){
                    setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_UNSATISFIED);
                    return;
                }
            }

            setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_SATISFIED);
            if (TaskerPlugin.Setting.hostSupportsVariableReturn(intent.getExtras())) {
                Bundle vars = new Bundle();
                vars.putString("%" + message, publishedMessage);
                vars.putString("%" + topicVar, publishedTopic);
                Log.i("Query success", "Returning var name " + message + " with value " + publishedMessage);
                TaskerPlugin.addVariableBundle(getResultExtras(true), vars);
            } else {
                Log.i("Query success", "Seems like host doesnt support variable setting");
            }

            setResultCode(in.dc297.mqttclpro.tasker.activity.Intent.RESULT_CONDITION_SATISFIED);

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private Object getCustomValue(String methodName, String a, String b){
        Method[] methods = ComparatorHelper.class.getMethods();

        Object value = null;
        for(Method method:methods){
            if(method.getName().equals(methodName)){
                try {
                    value = method.invoke(new ComparatorHelper(), a, b);
                    break;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
}
