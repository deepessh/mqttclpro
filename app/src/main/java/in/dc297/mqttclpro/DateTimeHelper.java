package in.dc297.mqttclpro;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by deepesh on 30/3/16.
 */
public class DateTimeHelper {
    public String formatTime(String paramString) {
        return paramString!=null ? paramString.substring(0,10) : null;
    }

}
