package in.dc297.mqttclpro.mqtt.internal;

/**
 * Created by Deepesh on 11/1/2016.
 */

public class Util {
    /* Does a topic match a subscription? */
    public static boolean mosquitto_topic_matches_sub(String sub, String topic)
    {
        int slen, tlen;
        int spos, tpos;
        boolean multilevel_wildcard = false;

        if(sub==null || "".equals(sub) || topic==null || "".equals(topic)) return false;

        slen = sub.length();
        tlen = topic.length();

        if(slen!=0 && tlen!=0){
            if((sub.charAt(0) == '$' && topic.charAt(0) != '$')
                    || (topic.charAt(0) == '$' && sub.charAt(0) != '$')){
                return false;
            }
        }

        spos = 0;
        tpos = 0;

        while(spos < slen && tpos < tlen){
            if(sub.charAt(spos) == topic.charAt(tpos)){
                if(tpos == tlen-1){
				/* Check for e.g. foo matching foo/# */
                    if(spos == slen-3
                            && sub.charAt(spos+1) == '/'
                            && sub.charAt(spos+2) == '#'){
                        multilevel_wildcard = true;
                        return true;
                    }
                }
                spos++;
                tpos++;
                if(spos == slen && tpos == tlen){
                    return true;
                }else if(tpos == tlen && spos == slen-1 && sub.charAt(spos) == '+'){
                    spos++;
                    return true;
                }
            }else{
                if(sub.charAt(spos) == '+'){
                    spos++;
                    while(tpos < tlen && topic.charAt(tpos) != '/'){
                        tpos++;
                    }
                    if(tpos == tlen && spos == slen){

                        return true;
                    }
                }else if(sub.charAt(spos) == '#'){
                    multilevel_wildcard = true;
                    if(spos+1 != slen){
                        return false;
                    }else{
                        return true;
                    }
                }else{
                    return false;
                }
            }
        }
        if(multilevel_wildcard == false && (tpos < tlen || spos < slen)){
            return false;
        }

        return true;
    }

    public static boolean isHostValid(String host){
        if(host==null) return false;
        if(host.length()==0) return false;
        if(host.length()>255) return false;

        String IP_ADDRESS = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
        String HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

        if(!host.matches(IP_ADDRESS) && ! host.matches(HOSTNAME)){
            return false;
        }

        return true;
    }
}
