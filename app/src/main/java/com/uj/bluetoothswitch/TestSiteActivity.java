package com.uj.bluetoothswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;
import com.uj.bluetoothswitch.disposables.StringMessageIOProcessors;
import com.uj.bluetoothswitch.serviceparts.A2DPManager;
import com.uj.bluetoothswitch.serviceparts.BTClient;
import com.uj.bluetoothswitch.serviceparts.BTInquirer;
import com.uj.bluetoothswitch.serviceparts.BTListener;
import com.uj.bluetoothswitch.serviceparts.BTReplier;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class TestSiteActivity extends AppCompatActivity {

    private static final String TAG= "TEST_ACTIVITY";
    private static final UUID MYUUID= UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static final String NAME= "MyRandomName";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private static final String TRONSMARTMAC="FC:58:FA:C1:03:29";
    private static final Byte[] KEYPATTERN=new Byte[]{125,125,125};
    A2DPManager man;
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

        man = new A2DPManager(this);
        replier = new BTReplier(new BTListener(KEYPATTERN,MYUUID,NAME),man);
        inquirer=new BTInquirer(new BTClient(KEYPATTERN,MYUUID,NAME),man);


//
//        replier.waitForInquiry(adapter.getRemoteDevice(TRONSMARTMAC));
       // client=new BTClient(KEYPATTERN,MYUUID,NAME);


    }

    public void replier(View v){
        replier.waitForInquiry(adapter.getRemoteDevice(TRONSMARTMAC));
    }

    public void disconnectOnClick(View view){
        man.tryUnbindFromDevice(TRONSMARTMAC).subscribe();
    }

    public void connectOnClick(View view){
        man.tryConnectToDevice(TRONSMARTMAC);
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