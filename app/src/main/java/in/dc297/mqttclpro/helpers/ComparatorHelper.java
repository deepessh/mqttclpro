package in.dc297.mqttclpro.helpers;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Deepesh on 10/27/2017.
 */

public class ComparatorHelper {

    public boolean eq(String a, String b) {
        return a.equals(b);
    }

    public boolean neq(String a, String b) {
        return !a.equals(b);
    }

    public boolean mR(String a, String b) {
        try {
            Pattern p = Pattern.compile(b);
            Matcher m = p.matcher(a);
            return m.find();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean nmR(String a, String b) {
        try {
            Pattern p = Pattern.compile(b);
            Matcher m = p.matcher(a);
            return !m.find();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean mlt(String a, String b) {
        try{
            return (new BigDecimal(a)).compareTo(new BigDecimal(b))<0;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean mgt(String a, String b) {
        try{
            return (new BigDecimal(a)).compareTo(new BigDecimal(b))>0;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean meq(String a, String b) {
        try{
            return (new BigDecimal(a)).compareTo(new BigDecimal(b))==0;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean mneq(String a, String b) {
        try{
            return (new BigDecimal(a)).compareTo(new BigDecimal(b))!=0;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
