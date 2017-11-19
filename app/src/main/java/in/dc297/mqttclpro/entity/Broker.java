package in.dc297.mqttclpro.entity;

import android.databinding.Bindable;
import android.os.Parcelable;


import android.databinding.Observable;

import io.requery.CascadeAction;
import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.OneToMany;
import io.requery.Persistable;
import io.requery.query.MutableResult;

/**
 * Created by Deepesh on 10/15/2017.
 */
@Entity
public interface Broker extends Observable, Parcelable, Persistable{

    @Key
    @Generated
    long getId();

    @Bindable
    boolean getSSLEnabled();

    void setSSLEnabled(boolean sslEnabled);

    @Bindable
    boolean getWSEnabled();

    void setWSEnabled(boolean wsEnabled);

    @Bindable
    @Column(nullable = false)
    String getHost();

    void setHost(String host);

    @Bindable
    @Column(nullable = false)
    String getPort();

    void setPort(String port);

    @Bindable
    String getUsername();

    void setUsername(String username);

    @Bindable
    String getPassword();

    void setPassword(String password);

    @Bindable
    String getKeepAlive();

    void setKeepAlive(String keepAlive);

    @Bindable
    @Column(nullable = false)
    String getClientId();

    void setClientId(String clientId);

    @Bindable
    boolean getCleanSession();

    void setCleanSession(boolean cleanSession);

    @Bindable
    String getCACrt();

    void setCACrt(String caCrt);

    @Bindable
    String getClientCrt();

    void setClientCrt(String clientCrt);

    @Bindable
    String getClientKey();

    void setClientKey(String clientKey);

    @Bindable
    String getClientP12Crt();

    void setClientP12Crt(String clientP12Crt);

    @Bindable
    String getClientKeyPwd();

    void setClientKeyPwd(String clientKeyPwd);

    @Bindable
    String getLastWillTopic();

    void setLastWillTopic(String lastWillTopic);

    @Bindable
    String getLastWillMessage();

    void setLastWillMessage(String lastWillMessage);

    @Bindable
    @Column(value = "0")
    String getLastWillQOS();

    void setLastWillQOS(String lastWillQOS);

    @Bindable
    boolean getLastWillRetained();

    void setLastWillRetained(boolean lastWillRetained);

    @Bindable
    boolean getEnabled();

    void setEnabled(boolean enabled);

    @Bindable
    @Column(nullable = false)
    String getNickName();

    void setNickName(String nickName);


    @OneToMany
    MutableResult<Topic> getTopics();

    @Bindable
    @Column(value = "Disabled")
    String getStatus();

    @Bindable
    int getTaskerPassThroughId();

    @Bindable
    @Column(value = "false")
    boolean getv31();
}
