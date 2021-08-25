package com.uj.bluetoothswitch.editentryfragment;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.uj.bluetoothswitch.MainActivity;
import com.uj.bluetoothswitch.R;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;

import org.jetbrains.annotations.NotNull;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class EditDeviceEntryFragment extends Fragment {
private static final String TAG="EDIT_ENTRY_FRAGMENT";
private int mEntityID;
private EditText mNameText;
private EditText mMacText;
private Button mConfirmButton;
private Button mCancelButton;
private MutableLiveData<DeviceEntity> mEntityForEdit=new MutableLiveData<>();
private MainActivity mMainActivity;
private NavController mNavContoroller;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_edit_device_entry, container, false);

    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {

        mNameText=view.findViewById(R.id.editNameEditText);
        mMacText=view.findViewById(R.id.editMACEditText);
        mConfirmButton=view.findViewById(R.id.editConfirmButton);
        mCancelButton=view.findViewById(R.id.editCancelButton);
        mEntityID=EditDeviceEntryFragmentArgs.fromBundle(getArguments()).getDeviceEntityID();
        mMainActivity=(MainActivity) getActivity();
        mNavContoroller= Navigation.findNavController(view);
        mMainActivity.getMainActivityVM().getDeviceDb().deviceDAO().getByID(mEntityID)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        (entity)->{
                            mEntityForEdit.postValue(entity);
                        }
                );
       mEntityForEdit.observe(getViewLifecycleOwner(), new Observer<DeviceEntity>() {
           @Override
           public void onChanged(DeviceEntity entity) {
               if(entity!=null){
                   mNameText.setText(entity.deviceName);
                   mMacText.setText(entity.macAdress);
                   mConfirmButton.setClickable(true);
               }
           }
       });
      mCancelButton.setOnClickListener(this::cancelOnclick);
      mConfirmButton.setOnClickListener(this::confirmOnClick);

    }


    public void confirmOnClick(View view){
        String nameContents=mNameText.getText().toString();
        String macContents=mMacText.getText().toString().trim();
        if(BluetoothAdapter.checkBluetoothAddress(macContents)){
            mMainActivity.getMainActivityVM().getDeviceDb().deviceDAO()
                    .updateByID(mEntityID,nameContents,macContents)
                    .subscribeOn(Schedulers.io())
                    .subscribe();
            Log.d(TAG, "confirmOnClick with device name: "+nameContents+"Mac: "+macContents);
            mNavContoroller.navigate(EditDeviceEntryFragmentDirections.actionEditDeviceEntryFragmentToMainScreenFragment());
        }
        else {
            Log.d(TAG, "Cannot confirm because of invalid MAC: "+ macContents);
            Toast.makeText(getContext(), "Invalid MAC-adress", Toast.LENGTH_SHORT).show();
        }
    };

    public void cancelOnclick (View view){
        mNavContoroller.navigate(EditDeviceEntryFragmentDirections.actionEditDeviceEntryFragmentToMainScreenFragment());
    }

}