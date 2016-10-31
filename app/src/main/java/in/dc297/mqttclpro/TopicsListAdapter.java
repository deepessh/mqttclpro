package in.dc297.mqttclpro;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by deepesh on 29/3/16.
 */
public class TopicsListAdapter extends SimpleCursorAdapter {


    private Context mContext;
    private Context appContext;
    private int layout;
    private Cursor cr;
    private final LayoutInflater inflater;
    private String[] from;
    private int[] to;
    private ViewBinder mViewBinder;
    private DateTimeHelper dth = new DateTimeHelper();

    public TopicsListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.layout=layout;
        this.mContext = context;
        this.inflater=LayoutInflater.from(context);
        this.cr=c;
        this.from = from;
        this.to = to;
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        //super.bindView(view, context, cursor);

        int count = this.from.length;
        for (int i = 0; i < count; i++) {
            final TextView v = (TextView) view.findViewById(to[i]);
            if (v != null) {
                if(v.getId()==R.id.message_count){
                    if(!cursor.getString(cursor.getColumnIndexOrThrow("count")).equals("0")){
                        v.setVisibility(View.VISIBLE);
                    }
                    else{
                        v.setVisibility(View.INVISIBLE);
                    }
                }
                v.setText(cursor.getString(cursor.getColumnIndexOrThrow(from[i])));
                if(v.getId()==R.id.message_tv){
                    if(cursor.getString(cursor.getColumnIndexOrThrow("message"))==null){
                        v.setText("No message received.");
                    }
                }
                if(v.getId()==R.id.timestamp_tv){
                    v.setText(dth.formatTime(cursor.getString(cursor.getColumnIndexOrThrow("timest"))));
                }
            }
        }
    }
}
