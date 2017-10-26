package in.dc297.mqttclpro.preferences;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import in.dc297.mqttclpro.entity.BrokerEntity;

/**
 * Created by Deepesh on 10/19/2017.
 */

public class MyBrokerPreferences implements SharedPreferences {

    BrokerEntity broker;


    public MyBrokerPreferences(BrokerEntity broker){
        this.broker = broker;
    }

    public BrokerEntity getBroker(){
        return this.broker;
    }

    @Override
    public Map<String, ?> getAll() {
        return null;
    }

    private Object getCustomValue(String key){
        Method[] methods = BrokerEntity.class.getMethods();
        Object value = null;
        for(Method method:methods){
            if(method.getName().equals("get"+key)){
                try {
                    value = method.invoke(broker);
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

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        Object value = getCustomValue(key);
        if(value!=null){
            return String.valueOf(value);
        }
        return defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        Object value = getCustomValue(key);
        if(value!=null){
            return (Set<String>) value;
        }
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = getCustomValue(key);
        if(value!=null){
            return (int) value;
        }
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = getCustomValue(key);
        if(value!=null){
            return (long) value;
        }
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = getCustomValue(key);
        if(value!=null){
            return (float) value;
        }
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = getCustomValue(key);
        if(value!=null){
            return (boolean) value;
        }
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        Object value = getCustomValue(key);
        if(value!=null){
            return true;
        }
        return false;
    }

    @Override
    public Editor edit() {
        return new Editor();
    }

    protected List<OnSharedPreferenceChangeListener> listeners = new LinkedList<OnSharedPreferenceChangeListener>();

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.remove(listener);
    }

    public class Editor implements SharedPreferences.Editor{

        private void setCustomValue(String key, @Nullable Object value){
            Method[] methods = BrokerEntity.class.getMethods();
            for(Method method:methods){
                if(method.getName().equals("set"+key)){
                    try {
                        method.invoke(broker, value);
                        break;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public SharedPreferences.Editor putString(String key, @Nullable String value) {
            setCustomValue(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
            setCustomValue(key, values);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            setCustomValue(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            setCustomValue(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            setCustomValue(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            setCustomValue(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            setCustomValue(key, null);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
