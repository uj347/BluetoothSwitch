package com.uj.bluetoothswitch.addnewdevicefragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.MacAddress;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding4.widget.RxAdapter;
import com.jakewharton.rxbinding4.widget.RxTextView;
import com.uj.bluetoothswitch.MainActivity;
import com.uj.bluetoothswitch.R;
import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddNewDeviceFragment extends Fragment {
         private static final String TAG = "ADD_NEW_DEVICE_ FRAGMENT";
         private final BluetoothAdapter mAdapter=BluetoothAdapter.getDefaultAdapter();
         private MainActivity mActivity;
         private RecyclerView foundDevicesRecycler;
         private TextView mDeviceNameField, mDeviceMACField;
         private BroadcastReceiver mDeviceFoundReceiver;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity=(MainActivity) this.getActivity();


    }

    @Override
    public void onDestroy() {
        if (mDeviceFoundReceiver!=null) {
            getActivity().unregisterReceiver(mDeviceFoundReceiver);
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        super.onDestroy();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_new_device, container, false);

    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        mDeviceFoundReceiver=new BroadcastReceiver() {
            MutableLiveData <Set<DeviceEntity>> foundDevicesLiveData=((MainActivity)getActivity())
                    .getMainActivityVM().getDiscoveredDevicesLD();
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals( BluetoothDevice.ACTION_FOUND))   {
                    BluetoothDevice foundDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (foundDevice!=null) {
                        Log.d(TAG, "Recived broadcast about found device: "+ foundDevice.getName());
                        DeviceEntity foundDeviceEntity= DeviceEntity.getEntityFor(foundDevice.getName()
                                ,foundDevice.getAddress());
                        Set<DeviceEntity> entitiesFromLD=foundDevicesLiveData.getValue();
                        entitiesFromLD.add(foundDeviceEntity);
                        foundDevicesLiveData.setValue(entitiesFromLD);
                    }
                }
            }
        };

        mDeviceNameField=(TextView) getView().findViewById(R.id.devToAddNameTextField);
        mDeviceMACField=(TextView)getView().findViewById(R.id.devToAddMacTextField);
        foundDevicesRecycler=(RecyclerView) this.getActivity().findViewById(R.id.addFoundedRecycler);
        if(foundDevicesRecycler!=null){
            foundDevicesRecycler.setAdapter(new FoundDevicesRecyclerAdapter(mActivity));
        }
        getActivity().registerReceiver(mDeviceFoundReceiver,new IntentFilter(BluetoothDevice.ACTION_FOUND));
        BluetoothAdapter.getDefaultAdapter().startDiscovery();

    }

    @Override
    public void onStart() {
        super.onStart();
        Completable
                .timer(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(()->{BluetoothAdapter.getDefaultAdapter().cancelDiscovery();});

    }
//TODO
    public void addDeviceManualyOnClick(View view){
       String name=this.mDeviceNameField.getText().toString().trim();
       String mac=this.mDeviceMACField.getText().toString().trim().toUpperCase();
       boolean validMac=BluetoothAdapter.checkBluetoothAddress(mac);
       if(validMac){
          DeviceEntity newEntity=DeviceEntity.getEntityFor(name,mac);
          mActivity.getMainActivityVM().getDeviceDb().deviceDAO().insertAll(newEntity)
                  .subscribeOn(Schedulers.io())
                  .subscribe();
       }else{
           Toast.makeText(mActivity,"Invalid input",Toast.LENGTH_SHORT);
       }
}


}