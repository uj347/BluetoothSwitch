package com.uj.bluetoothswitch.mainfragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
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
import com.uj.bluetoothswitch.MainActivityViewModel;
import com.uj.bluetoothswitch.R;
import com.uj.bluetoothswitch.dbStuff.DeviceDAO;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;
import com.uj.bluetoothswitch.serviceparts.BTConnectionService;
import com.uj.bluetoothswitch.serviceparts.Commander;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class RememberedDevicesRecyclerAdapter extends RecyclerView.Adapter  {
public static final String TAG="RememberedDevicesRV";
    private MainActivity activityContext;
    private MainActivityViewModel mMainViewModel;
    private List<DeviceEntity> recyclerContents= new ArrayList<>();



    public RememberedDevicesRecyclerAdapter(MainActivity activity, DeviceDAO deviceDAO){
        this.activityContext=activity;
        this.mMainViewModel=activity.getMainActivityVM();
        LiveData<List<DeviceEntity>> allDevicesLD=mMainViewModel.getDeviceDb()
               .deviceDAO()
               .getAllLivedata();
        List<DeviceEntity> currentContentsOfDb=allDevicesLD.getValue();
        if(currentContentsOfDb!=null) {
            setRecyclerContents(currentContentsOfDb);
        }

               allDevicesLD.observe(activity, new Observer<List<DeviceEntity>>() {
            @Override
            public void onChanged(List<DeviceEntity> deviceEntities) {
                setRecyclerContents(deviceEntities);
                RememberedDevicesRecyclerAdapter.this.notifyDataSetChanged();
            }
        });
    }

    public synchronized List<DeviceEntity> getRecyclerContents() {
        return recyclerContents;
    }

    public synchronized void setRecyclerContents(List<DeviceEntity> recyclerContents) {
        this.recyclerContents = recyclerContents;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull  ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.device_recycler_element,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull  RecyclerView.ViewHolder holder, int position) {
         MyViewHolder myViewHolder=(MyViewHolder)holder;
         myViewHolder.setViewHolderToDevice(recyclerContents.get(position));
    }

    @Override
    public int getItemCount() {
     return recyclerContents.size();
    };



    class MyViewHolder extends RecyclerView.ViewHolder{
        private DeviceEntity bluetoothDevice;
        private TextView textView;
        private Button deleteButton;
        private Button editButton;

        public void setViewHolderToDevice (DeviceEntity bluetoothDevice){
            this.bluetoothDevice=bluetoothDevice;
             this.textView.setText(bluetoothDevice.deviceName+"\n"+bluetoothDevice.macAdress);

        }


        public MyViewHolder(@NonNull  View itemView) {
        super(itemView);
        this.textView=(TextView) itemView.findViewById(R.id.foundDeviceTextView);
        this.deleteButton=(Button) itemView.findViewById(R.id.deleteButton);
        this.editButton=(Button) itemView.findViewById(R.id.editEntryButton);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String macOfDevice=MyViewHolder.this.bluetoothDevice.macAdress;
                Intent intentForService=new Intent(BTConnectionService.COMMAND_USER_SEEKS_CONNECT);
                intentForService.putExtra(BluetoothDevice.EXTRA_DEVICE,
                        BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macOfDevice));
                Log.d(TAG, "broadcasting intent to reach device: "+intentForService.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) );
                mMainViewModel.getAppContext().sendBroadcast(intentForService);
                }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMainViewModel.getDeviceDb().deviceDAO().delete(MyViewHolder.this.bluetoothDevice)
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    activityContext.getNavController()
                        .navigate(MainScreenFragmentDirections
                                .actionMainScreenFragmentToEditDeviceEntryFragment(MyViewHolder.this.bluetoothDevice.id));
            }
        });


    }
}


}

