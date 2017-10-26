package in.dc297.mqttclpro.activity;

import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;

/**
 * Created by Deepesh on 10/15/2017.
 */

public class BindingHolder<B extends ViewDataBinding> extends RecyclerView.ViewHolder {

    protected final B binding;

    public BindingHolder(B binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
