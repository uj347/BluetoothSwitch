package com.uj.bluetoothswitch;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavHost;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.app.LauncherActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.uj.bluetoothswitch.mainfragment.MainScreenFragmentDirections;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BlueSwitchMainActivity";

    private MainActivityViewModel viewModel;
    private NavController navController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        viewModel = new ViewModelProvider(this, new MainActivityViewModel.MainActivityVMFactory(this))
                .get(MainActivityViewModel.class);
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
        return viewModel;
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
}