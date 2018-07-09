package in.dc297.mqttclpro.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.activity.SubscribedTopicsActivity;

public class AddTopicDialogFragment extends DialogFragment {

    private long brokerId;

    public void setBrokerId(long brokerId){
        this.brokerId = brokerId;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.add_topic_desc)
                .setTitle(R.string.add_topic)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(getActivity(),SubscribedTopicsActivity.class);
                        intent.putExtra(SubscribedTopicsActivity.EXTRA_BROKER_ID,brokerId);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}