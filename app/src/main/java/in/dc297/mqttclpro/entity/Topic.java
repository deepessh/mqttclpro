package in.dc297.mqttclpro.entity;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.os.Parcelable;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Index;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Persistable;
import io.requery.Table;
import io.requery.Transient;
import io.requery.query.MutableResult;

/**
 * Created by Deepesh on 10/15/2017.
 */

@Entity
@Table(name="Topic", uniqueIndexes = {"my_unq_indx"})
public interface Topic extends Observable, Parcelable, Persistable {

    @Key
    @Generated
    long getId();

    @Bindable
    @Index("my_unq_indx")
    String getName();

    /**
     * 0 means sub
     * 1 means pub
     * @return
     */
    @Bindable
    @Index("my_unq_indx")
    int getType();

    @Bindable
    int getQOS();

    @ManyToOne
    @Index("my_unq_indx")
    Broker getBroker();

    @OneToMany
    MutableResult<Message> getMessages();

    @Transient
    int getUnreadCount();

    @Transient
    Message getLatestMessage();

    @Transient
    int getCountVisibility();
}
