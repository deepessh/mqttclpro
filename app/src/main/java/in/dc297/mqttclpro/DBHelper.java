package in.dc297.mqttclpro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by deepesh on 28/3/16.
 */
public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, context.getPackageName(), null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS topics(_id INTEGER PRIMARY KEY AUTOINCREMENT,topic TEXT,show_noti INTEGER DEFAULT 0,topic_type INTEGER DEFAULT 0, qos INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS messages(_id INTEGER PRIMARY KEY AUTOINCREMENT,topic_id INTEGER,message VARCHAR,timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,read INTEGER DEFAULT 0,topic TEXT,display_topic TEXT,status INTEGER DEFAULT 1,qos INTEGER DEFAULT 0)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS topics");
        db.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(db);

    }

    public int addTopic(String topic,int topic_type,int qos){
        SQLiteDatabase db = getWritableDatabase();
        try{
            MqttTopic.validate(topic,true);
        }
        catch(IllegalArgumentException ile){
            ile.printStackTrace();
            return 3;
        }
        catch(IllegalStateException ise){
            ise.printStackTrace();
            return 3;
        }
        topic = DatabaseUtils.sqlEscapeString(topic);
        try {
            if (db.rawQuery("SELECT * FROM topics where topic=" + topic + " and topic_type=" + topic_type, null).getCount() == 0) {
                db.execSQL("INSERT INTO topics (topic,topic_type,qos) VALUES (" + topic + "," + topic_type + ","+ qos +")");
                return 0;
            } else return 1;
        } catch (SQLException se) {

            se.printStackTrace();
            return 2;

        }
    }

    public Cursor getTopics(int topic_type){
        SQLiteDatabase db = getReadableDatabase();
        Cursor topicCursor = db.rawQuery("SELECT topics._id as _id,topics.qos as qos, topics.topic as topic,(SELECT COUNT(message) FROM messages WHERE messages.topic_id = topics._id AND messages.read=0) AS count,messages.message as message,datetime(messages.timestamp, 'localtime') as timest FROM topics LEFT JOIN messages ON messages.topic_id = topics._id AND messages._id=(SELECT messages._id FROM messages WHERE messages.topic_id = topics._id ORDER BY timestamp DESC LIMIT 1) WHERE topics.topic_type="+topic_type+" ORDER BY messages.timestamp DESC", null);

        return topicCursor;//.moveToFirst();
        /*while(!topicCursor.isAfterLast()){
            String topic = topicCursor.getString(0);
            int count = topicCursor.getInt(1);
            String message = topicCursor.getString(2);
            String time = topicCursor.getString(3);
            if(message==null) message = "No message received";

            TopicsListViewModel tlvm = new TopicsListViewModel(topic,count,message,time);
            topics.add(tlvm);
            Log.i("mytopic",topic);
            topicCursor.moveToNext();
        }
        Log.i("mytopic",String.valueOf(topics.size()));*/
        //return topics;
    }

    public long addMessage(String topic,String message,int topic_type,int qos){
        if(topic_type==0){
            return addMessageReceived(topic,message,qos);
        }
        SQLiteDatabase db = getWritableDatabase();
        String topic_esc = DatabaseUtils.sqlEscapeString(topic);
        try{
            ContentValues values = new ContentValues();
            Cursor topicCursor = db.rawQuery("SELECT _id from topics where topic="+topic_esc+" and topic_type="+topic_type,null);
            topicCursor.moveToFirst();
            long topic_id =topicCursor.getLong(0);
            values.put("topic_id",topic_id);
            values.put("message", message);
            values.put("topic",topic);
            values.put("read", topic_type);
            values.put("qos",qos);
            values.put("display_topic",topic);
            if(topic_type==1) values.put("status",0);
            Log.i("db", "putting message");
            long mid = db.insertOrThrow("messages",null,values);
            //db
            return mid;
        }
        catch(SQLException se){
            Log.e("db","Failed to add message",se);
            return 0;
        }
    }

    public long addMessageReceived(String topic,String message, int qos){
        SQLiteDatabase db = getWritableDatabase();
        Util myUtil = new Util();
        try {
            db.beginTransaction();
            Cursor topicCursor = db.rawQuery("SELECT _id,topic from topics where topic_type=0",null);
            topicCursor.moveToFirst();
            while(!topicCursor.isAfterLast()){
                long topic_id = topicCursor.getLong(0);
                String topicInDb = topicCursor.getString(1);
                if(myUtil.mosquitto_topic_matches_sub(topicInDb,topic)){
                    ContentValues values = new ContentValues();
                    values.put("topic_id",topic_id);
                    values.put("message", message);
                    values.put("topic",topicInDb);
                    values.put("read", 0);
                    values.put("qos",qos);
                    values.put("display_topic",topic);
                    Log.i("db", "putting received message for topic"+topicInDb);
                    db.insertOrThrow("messages",null,values);
                }
                topicCursor.moveToNext();
            }
            db.setTransactionSuccessful();
            return 1;
        }
        catch(Exception e){
            e.printStackTrace();
            return 0;
        }
        finally {
            db.endTransaction();
        }
    }

    public Cursor getMessages(String topic,int topic_type){
        SQLiteDatabase db = getReadableDatabase();
        topic = DatabaseUtils.sqlEscapeString(topic);
        try{
            Cursor messagesCursor = db.rawQuery("SELECT _id,message,status,timestamp,display_topic from messages where topic_id = (SELECT _id from topics where topic="+topic+" and topic_type="+topic_type+") ORDER BY timestamp DESC",null);
            return messagesCursor;
        }
        catch(SQLException se){
            se.printStackTrace();
        }
        return null;
    }


    public void setMessagesRead(String topic,int topicType){
        SQLiteDatabase db = getWritableDatabase();
        topic = DatabaseUtils.sqlEscapeString(topic);
        try{
            db.execSQL("UPDATE messages set read=1 where topic_id = (SELECT _id from topics where topic="+topic+" and topic_type="+topicType+")");
        }
        catch(SQLException se){
            se.printStackTrace();
        }
    }

    public void setMessagePublished(long message_id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status",1);
        db.update("messages",cv,"_id="+message_id,null);
    }

    public int deleteTopic(String topic, int topicType){
        SQLiteDatabase db = getWritableDatabase();
        try{
            String[] values = new String[1];
            String topic_esc = DatabaseUtils.sqlEscapeString(topic);
            Cursor topicCursor = db.rawQuery("SELECT _id from topics where topic="+topic_esc+" and topic_type="+topicType,null);
            topicCursor.moveToFirst();
            String topic_id =String.valueOf(topicCursor.getLong(0));
            values[0] = topic_id;
            db.delete("messages","topic_id=?",values);
            db.delete("topics","_id=?",values);

            Log.i("db","deleted message");
            //db
            return 1;
        }
        catch(SQLException se){
            Log.e("db","Failed to delete topic",se);
            return 0;
        }

    }

    public void deleteMessages(String topic,int topic_type){
        SQLiteDatabase db = getWritableDatabase();
        String topic_esc = DatabaseUtils.sqlEscapeString(topic);

        try{
            db.execSQL("DELETE FROM messages where topic_id in (SELECT _id from topics where topic = "+topic_esc+" and topic_type = "+topic_type+");");
            if(topic_type==1){
                db.execSQL("DELETE from topics where topic = "+topic_esc+" and topic_type = "+topic_type+";");
            }
        }
        catch(SQLException se){
            Log.e("db","Failed to delete messages",se);
        }

    }
}
