package com.uj.bluetoothswitch;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.uj.bluetoothswitch.mainfragment.MainScreenFragmentDirections;
import com.uj.bluetoothswitch.serviceparts.BTConnectionService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BlueSwitchMainActivity";

    private MainActivityViewModel mViewModel;
    private NavController navController;
    private MutableLiveData<Boolean> mIsServerRunningLD;
    private MutableLiveData<BluetoothDevice> mCurrentlyConnectedSoundDeviceLD;
    private static final IntentFilter stateIntentFilter = new IntentFilter();

    static {
        stateIntentFilter.addAction(BTConnectionService.STATE_DISABLING);
        stateIntentFilter.addAction(BTConnectionService.STATE_IDLE);
        stateIntentFilter.addAction(BTConnectionService.STATE_LISTENING);
        stateIntentFilter.addAction(BTConnectionService.STATE_REACHING);
    }

    ;

    private final BroadcastReceiver serverStateBroadcastReceiver = new ServiceStateBR();


    @Override
    protected void onStart() {
        super.onStart();
        sendBroadcast(new Intent(BTConnectionService.COMMAND_USER_SEEKS_CURRENTSTATE));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, new MainActivityViewModel.MainActivityVMFactory(this))
                .get(MainActivityViewModel.class);
        this.mCurrentlyConnectedSoundDeviceLD = mViewModel.getCurrentlyConnectedSoundDeviceLD();
        this.mIsServerRunningLD = mViewModel.getIsServerRunningLD();

        Intent serviceIntent = new Intent(this, BTConnectionService.class);
        registerReceiver(serverStateBroadcastReceiver, stateIntentFilter);
        startService(serviceIntent);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        setContentView(R.layout.activity_main);
        navController = Navigation.findNavController(this, R.id.main_activity_host_fragment);
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            registerBluetoothRequest().launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));

        }


        //TODO Предстоит много работы, нужно прокинуть в адаптер вью модел, и основательно переделать его для работы с лайв дэйтой из вью модел


    }


    public NavController getNavController() {
        return navController;
    }

    public MainActivityViewModel getMainActivityVM() {
        return mViewModel;
    }


    public void toAddDeviceOnClick(View view) {
        this.getNavController().navigate(MainScreenFragmentDirections.actionMainScreenFragmentToAddNewDeviceFragment());
    }

    public void startBTDiscoveryOnClick(View view) {
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    private ActivityResultLauncher<Intent> registerBluetoothRequest() {
        Intent bluetoothEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ActivityResultContracts.StartActivityForResult bluetoothContract = new ActivityResultContracts.StartActivityForResult();
        ActivityResultLauncher<Intent> bluetoothRequest = registerForActivityResult(bluetoothContract,
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Toast.makeText(MainActivity.this, "Bluetooth device enabled succesfully", Toast.LENGTH_LONG)
                                    .show();
                        }
                        if (result.getResultCode() == RESULT_CANCELED) {
                            Toast.makeText(MainActivity.this, "This application requires bluetooth conenction", Toast.LENGTH_LONG)
                                    .show();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            System.exit(1);
                        }
                    }
                });

        return bluetoothRequest;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(serverStateBroadcastReceiver);
    }

    private class ServiceStateBR extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received new state: " + intent.getAction());
            switch (intent.getAction()) {
                case (BTConnectionService.STATE_IDLE):
                    mIsServerRunningLD.setValue(true);
                    mCurrentlyConnectedSoundDeviceLD.setValue(null);
                    ;
                    break;
                case (BTConnectionService.STATE_LISTENING):
                    mIsServerRunningLD.setValue(true);
                    BluetoothDevice currentDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (currentDevice != null) {
                        mCurrentlyConnectedSoundDeviceLD.setValue(currentDevice);
                        Toast.makeText(getApplicationContext()
                                , "Connected to sound device: " + currentDevice
                                , Toast.LENGTH_SHORT)
                                .show();
                    }
                    ;
                    break;
                case (BTConnectionService.STATE_REACHING):
                    mIsServerRunningLD.setValue(true);
                    mCurrentlyConnectedSoundDeviceLD.setValue(null);
                    break;
                case (BTConnectionService.STATE_DISABLING):
                    mIsServerRunningLD.setValue(false);
                    mCurrentlyConnectedSoundDeviceLD.setValue(null);
                    break;
            }
        }
    }
}
