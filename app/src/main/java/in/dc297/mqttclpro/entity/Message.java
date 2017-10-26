package in.dc297.mqttclpro.entity;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import java.sql.Timestamp;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Persistable;
import io.requery.Transient;

/**
 * Created by Deepesh on 10/15/2017.
 */
@Entity
public interface Message extends Observable, Parcelable, Persistable {

    @Key
    @Generated
    int getId();

    @Bindable
    @ManyToOne
    Topic getTopic();

    @Bindable
    String getPayload();

    @Bindable
    int getQOS();

    @Bindable
    String getDisplayTopic();

    @Column(value = "CURRENT_TIMESTAMP", name="timeStamp")
    @Bindable
    Timestamp getTimeStamp();

    /**
     * 0 means unread
     * 1 means read
     * @return
     */
    @Column(value = "0", name="read")
    @Bindable
    int getRead();

    @Bindable
    boolean getRetained();

    @Bindable
    int getTaskerId();
}
