package in.dc297.mqttclpro.activity;

import android.app.Application;
import android.os.StrictMode;

import in.dc297.mqttclpro.BuildConfig;
import in.dc297.mqttclpro.entity.Models;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveSupport;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;

/**
 * Created by Deepesh on 10/16/2017.
 */

public class MQTTClientApplication extends Application {
    private ReactiveEntityStore<Persistable> dataStore;

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.enableDefaults();
    }

    public ReactiveEntityStore<Persistable> getData() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT,1);
            if (BuildConfig.DEBUG) {
                // use this in development mode to drop and recreate the tables on every upgrade
                source.setTableCreationMode(TableCreationMode.DROP_CREATE);
            }
            Configuration configuration = source.getConfiguration();
            dataStore = ReactiveSupport.toReactiveStore(
                    new EntityDataStore<Persistable>(configuration));
        }
        return dataStore;
    }
}
