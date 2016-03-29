package in.dc297.mqttclpro;

/**
 * Created by deepesh on 29/3/16.
 */
public class TopicsListViewModel {

    public int count;
    public String topic;
    public String message;
    public String time;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object tplvm){
        return (this.topic == ((TopicsListViewModel) tplvm).getTopic());
    }

    public TopicsListViewModel(String topic,int count, String message,String time){
        this.setCount(count);
        this.setMessage(message);
        this.setTime(time);
        this.setTopic(topic);
    }

}
