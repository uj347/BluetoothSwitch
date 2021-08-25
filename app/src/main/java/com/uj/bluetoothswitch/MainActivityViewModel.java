package com.uj.bluetoothswitch;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;
import com.uj.bluetoothswitch.serviceparts.BTConnectionService;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class MainActivityViewModel extends ViewModel {
    private final Context mContext;
    private final DeviceDB mDeviceDB;
    private MutableLiveData<Set<DeviceEntity>> discoveredDevicesLD;

    public BluetoothSwitcherApp getAppContext() {
        return appContext;
    }

    private final BluetoothSwitcherApp appContext;


    public MainActivityViewModel(Context context) {
        super();
        this.mContext=context;
        mDeviceDB=DeviceDB.getInstance(context);
        appContext=(BluetoothSwitcherApp) mContext.getApplicationContext();
    }

    public DeviceDB getDeviceDb(){return mDeviceDB;}



    public MutableLiveData<Set<DeviceEntity>> getDiscoveredDevicesLD(){
     if (discoveredDevicesLD ==null){
         this.discoveredDevicesLD=new MutableLiveData<>(new HashSet<>());
         return discoveredDevicesLD;
     }   else{
         return discoveredDevicesLD;
     }
    }





    public static class MainActivityVMFactory implements ViewModelProvider.Factory{

        private final Context context;
        public MainActivityVMFactory(Context context){
        this.context=context;
        }

        @NonNull
        @NotNull
        @Override
        public <T extends ViewModel> T create(@NonNull @NotNull Class<T> modelClass) {
            return (T) new MainActivityViewModel(context);
        }
    }
}
