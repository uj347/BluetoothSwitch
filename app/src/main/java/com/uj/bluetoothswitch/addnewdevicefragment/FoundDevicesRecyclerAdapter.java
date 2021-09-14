package com.uj.bluetoothswitch.addnewdevicefragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.uj.bluetoothswitch.MainActivity;
import com.uj.bluetoothswitch.R;
import com.uj.bluetoothswitch.dbStuff.DeviceDAO;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class FoundDevicesRecyclerAdapter extends RecyclerView.Adapter {
    private static final String TAG = "FOUND_DEVCIE_RECYCLERVIEW_ADAPTER";
    private MainActivity mActivity;
    private List<DeviceEntity> mContents;


    public FoundDevicesRecyclerAdapter(MainActivity activity) {
        this.mActivity = activity;
        LiveData<Set<DeviceEntity>> FoundLD = mActivity.getMainActivityVM().getDiscoveredDevicesLD();
        setmContents(new ArrayList<>(FoundLD.getValue()));
        FoundLD.observe(mActivity, new Observer<Set<DeviceEntity>>() {
            @Override
            public void onChanged(Set<DeviceEntity> deviceEntities) {
                setmContents(new ArrayList<>(deviceEntities));
                FoundDevicesRecyclerAdapter.this.notifyDataSetChanged();
                Log.d(TAG, "Live Data nptified DataSetChanged, current contents of LD: \n" + deviceEntities);
            }
        });

    }

    private synchronized List<DeviceEntity> getmContents() {
        return mContents;
    }

    private synchronized void setmContents(List<DeviceEntity> mContents) {
        this.mContents = mContents;
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View holderView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.found_device_element, parent, false);
        return new NewDeviceViewHolder(holderView);

    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        NewDeviceViewHolder myHolder = (NewDeviceViewHolder) holder;
        myHolder.setAppearence(mContents.get(position));
    }

    @Override
    public int getItemCount() {
        return mContents.size();
    }

    class NewDeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private Button addButton;
        private DeviceEntity entity;

        public NewDeviceViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.foundDeviceTextView);
            addButton = (Button) itemView.findViewById(R.id.foundDeviceAddButton);
        }

        public DeviceEntity getEntity() {
            return entity;
        }

        public void setAppearence(DeviceEntity entity) {
            this.entity = entity;
            this.textView.setText(entity.deviceName + "\n" + entity.macAdress);
            this.addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeviceDAO dao = mActivity.getMainActivityVM().getDeviceDb().deviceDAO();
                    dao.insertAll(NewDeviceViewHolder.this.getEntity())
                            .subscribeOn(Schedulers.io())
                            .doOnComplete(() -> Log.d(TAG, "onClick: new element inserted into DB"))
                            .subscribe();

                }
            });
        }
    }

}
