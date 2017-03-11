package in.dc297.mqttclpro;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import static in.dc297.mqttclpro.tasker.Constants.LOG_TAG;

/**
 * Created by deepesh on 29/3/16.
 */
public class MessagesListAdapter extends SimpleCursorAdapter {


    private Context mContext;
    private Context appContext;
    private int layout;
    private Cursor cr;
    private final LayoutInflater inflater;
    private String[] from;
    private int[] to;
    private ViewBinder mViewBinder;
    private DateTimeHelper dth = new DateTimeHelper();

    public MessagesListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
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
        super.bindView(view, context, cursor);
        if("0".equals(((TextView) view.findViewById(R.id.mstatustv)).getText().toString())){
            view.setBackgroundColor(context.getResources().getColor(R.color.unpubmessage));
        }
        else{
            view.setBackgroundColor(Color.rgb(255,255,255));
        }
        if(cursor.getInt(cursor.getColumnIndexOrThrow("retained")) == 0){
            ((TextView)view.findViewById(R.id.mretained_tv)).setText("Not Retained");
        }
        else{
            ((TextView)view.findViewById(R.id.mretained_tv)).setText("Retained");
        }
    }
}
