package in.dc297.mqttclpro;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

        db.execSQL("CREATE TABLE IF NOT EXISTS topics(_id INTEGER PRIMARY KEY AUTOINCREMENT,topic TEXT,show_noti INTEGER DEFAULT 0,topic_type INTEGER DEFAULT 0,qos INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS messages(_id INTEGER PRIMARY KEY AUTOINCREMENT,topic_id INTEGER,message VARCHAR,timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,read INTEGER DEFAULT 0,topic TEXT)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS topics");
        db.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(db);

    }

    public int addTopic(String topic,int topic_type){
        SQLiteDatabase db = getWritableDatabase();
        topic = DatabaseUtils.sqlEscapeString(topic);
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
            try {
                if (db.rawQuery("SELECT * FROM topics where topic=" + topic + " and topic_type=" + topic_type, null).getCount() == 0) {
                    db.execSQL("INSERT INTO topics (topic,topic_type) VALUES (" + topic + "," + topic_type + ")");
                    return 0;
                } else return 1;
            } catch (SQLException se) {

                se.printStackTrace();
                return 2;

            }
    }

    public Cursor getTopics(int topic_type){
        ArrayList<TopicsListViewModel> topics = new ArrayList<TopicsListViewModel>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor topicCursor = db.rawQuery("SELECT topics._id as _id,topics.qos as qos, topics.topic as topic,(SELECT COUNT(message) FROM messages WHERE messages.topic_id = topics._id AND messages.read=0) AS count,messages.message as message,datetime(messages.timestamp, 'localtime') as timest FROM topics LEFT JOIN messages ON messages.topic_id = topics._id AND messages._id=(SELECT messages._id FROM messages WHERE messages.topic_id = topics._id and topics.topic_type="+topic_type+" ORDER BY timestamp DESC LIMIT 1) ORDER BY messages.timestamp DESC", null);

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

    public boolean addMessage(String topic,String message){
        SQLiteDatabase db = getWritableDatabase();
        topic = DatabaseUtils.sqlEscapeString(topic);
        message = DatabaseUtils.sqlEscapeString(message);
        try{
            db.execSQL("INSERT into messages (topic_id,message,topic) VALUES ((SELECT _id from topics where topic="+topic+"),"+message+","+topic+")");
        }
        catch(SQLException se){
            Log.e("db","Failed to add message",se);
            return false;
        }
        return true;
    }
}
