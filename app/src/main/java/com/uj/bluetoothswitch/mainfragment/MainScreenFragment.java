package com.uj.bluetoothswitch.mainfragment;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import com.uj.bluetoothswitch.BluetoothSwitcherApp;
import com.uj.bluetoothswitch.MainActivity;
import com.uj.bluetoothswitch.MainActivityViewModel;
import com.uj.bluetoothswitch.R;
import com.uj.bluetoothswitch.serviceparts.BTConnectionService;

import org.jetbrains.annotations.NotNull;


public class MainScreenFragment extends Fragment {

    private MainActivity mMainActivity;
    private RecyclerView mDevicesRecyclerView;
    private Switch mServiceStateSwitch;
    private Button mDisconnectButton;
    private BluetoothSwitcherApp appContext;
    private Button mToggleServiceButton;
    private LiveData<Boolean>mIsServerRunningLD;
    private LiveData<BluetoothDevice>mCurrentlyConnectedDeviceLD;
    private MainActivityViewModel mMainActivityViewModel;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_screen, container, false);

    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        mServiceStateSwitch = view.findViewById(R.id.serviceStateIndicator);
        mDisconnectButton = view.findViewById(R.id.disconnectCurrentDeviceButton);
        mToggleServiceButton=view.findViewById(R.id.toggleService);


    }

    @Override
    public void onStart() {
        super.onStart();
        mMainActivity = (MainActivity) getActivity();
        mMainActivityViewModel=mMainActivity.getMainActivityVM();
        mIsServerRunningLD=mMainActivityViewModel.getIsServerRunningLD();
        mCurrentlyConnectedDeviceLD=mMainActivityViewModel.getCurrentlyConnectedSoundDeviceLD();


        mDevicesRecyclerView = (RecyclerView) mMainActivity.findViewById(R.id.devicesRecycler);
        mDevicesRecyclerView.setAdapter(new RememberedDevicesRecyclerAdapter(mMainActivity,
                mMainActivity.getMainActivityVM().getDeviceDb().deviceDAO()));
        appContext = mMainActivity.getMainActivityVM().getAppContext();
        mMainActivityViewModel.getIsServerRunningLD()
                .observe(this, state -> {
                    if (mServiceStateSwitch != null) {
                        mServiceStateSwitch.setChecked(state);
                    }
                });

        mCurrentlyConnectedDeviceLD.observe(this, device -> {
            if (device == null) {
                mDisconnectButton.setEnabled(false);
            } else {
                mDisconnectButton.setEnabled(true);
                mDisconnectButton.setOnClickListener((view) -> {
                    Intent disconnectIntent = new Intent(BTConnectionService.COMMAND_USER_SEEKS_DISCONNECT);

                    mMainActivity.sendBroadcast(disconnectIntent);
                });
            }
        });
        mToggleServiceButton.setOnClickListener(this::toggleServiceOnClick);
    }

    public void toAddDeviceOnClick(View view) {
        mMainActivity.getNavController().navigate(MainScreenFragmentDirections.actionMainScreenFragmentToAddNewDeviceFragment());
    }

    public void toggleServiceOnClick(View view){
        boolean isServiceRunning=mIsServerRunningLD.getValue();
        if (isServiceRunning){
            mMainActivity.sendBroadcast(new Intent(BTConnectionService.COMMAND_USER_SEEKS_STOPSERVICE));
        }else{
            mMainActivity.startService(new Intent(mMainActivity,BTConnectionService.class));
        }

    }

}

