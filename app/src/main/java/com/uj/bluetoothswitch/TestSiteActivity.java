package com.uj.bluetoothswitch;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;
import com.uj.bluetoothswitch.serviceparts.BTConnectionService;
import com.uj.bluetoothswitch.serviceparts.SoundProfileManager;
import com.uj.bluetoothswitch.serviceparts.BTInquirer;
import com.uj.bluetoothswitch.serviceparts.BTListener;
import com.uj.bluetoothswitch.serviceparts.BTReplier;
import com.uj.bluetoothswitch.serviceparts.Commander;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.reactivex.rxjava3.core.Observer;

public class TestSiteActivity extends AppCompatActivity {

    private static final String TAG= "TEST_ACTIVITY";
    private static final UUID MYUUID= UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static final String NAME= "MyRandomName";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private static final String TRONSMARTMAC="FC:58:FA:C1:03:29";
    private static final Byte[] KEYPATTERN=new Byte[]{125,125,125};
    SoundProfileManager man;
    BTReplier replier;
    BTInquirer inquirer;

    //BTClient client;

    private DeviceDB deviceDB;



    Button b1,b2;
      TextView t;
      Set<DeviceEntity> entitySet;
      private static BluetoothAdapter adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_site);
        b1=findViewById(R.id.testButton1);
        b2=findViewById(R.id.testButton2);
        t=findViewById(R.id.testText);
        adapter=BluetoothAdapter.getDefaultAdapter();

        man = new SoundProfileManager(this);
        replier = new BTReplier(new BTListener(KEYPATTERN,MYUUID,NAME),man);
        Intent serviceIntent=new Intent(this,BTConnectionService.class);

        //TODO Не забыть включить назад когда настрою менеджер

//        startService(serviceIntent);
//        bindService(serviceIntent,new MyConnection(), Context.BIND_AUTO_CREATE);
//


//
//        replier.waitForInquiry(adapter.getRemoteDevice(TRONSMARTMAC));
       // client=new BTClient(KEYPATTERN,MYUUID,NAME);


    }


//    public void tryConnectToPlansh(View v) {
//        replier.stopWaitingForInquiry();
//        try{
//            Thread.sleep(750);
//        }
//        catch(InterruptedException exc){
//            Log.d(TAG, exc.getMessage());
//        }
//        client.starConnectionToSpecifiedMAC(PLANSHMAC);
//        Completable.create(e->
//                {
//                    Thread.sleep(750);
//                    client.getExternalOutputHook().onNext(TRONSMARTMAC);
//                    Thread.sleep(250);
//                    client.getExternalInputHook()
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe(s->{t.setText(s);});
//                }
//        ).subscribeOn(Schedulers.newThread()).subscribe();
//}

//    public void disconnectTronsmart(View v){
//        man.tryUnbindFromDevice(TRONSMARTMAC).subscribe();
//    }
    public void inquirer (View v){
        inquirer.makeInquiry(TRONSMARTMAC,adapter.getRemoteDevice(PLANSHMAC));
    }




    @Override
    protected void onDestroy() {

        super.onDestroy();


    }


}
