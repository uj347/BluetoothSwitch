package com.uj.bluetoothswitch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;


import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
private static final String TAG="BlueSwitchMainActivity";

    private DeviceDB deviceDB;
    private RecyclerView recyclerView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceDB=DeviceDB.getInstance(this);


        //Debug
        Log.d(TAG, "onCreate: beforesingle");
        Single.just(1).observeOn(Schedulers.io()).map(i->deviceDB.deviceDAO().getAll()).subscribe(list-> {
            Log.d(TAG, "onCreate: in debug subscribe");
            for (DeviceEntity e:
                 list) {
                Log.d(TAG, e.deviceName);

            }
        });
//Commented for debug purposes
        recyclerView=findViewById(R.id.devicesRecycler);
        recyclerView.setAdapter(new DeviceRecyclerAdapter(this));









//        deviceDB.deviceDAO().insertAll(entitySet.toArray(new DeviceEntity[1]))
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .subscribe(()->Log.d("AGA",(Thread.currentThread().getName()+"Entities added ")));
//
//
//        Observable.create(emit->
//                {
//                   emit.onNext(deviceDB.deviceDAO().getByID(7));
//                   emit.onComplete();
//                }
//        ).
//
//
//                  subscribeOn(Schedulers.io())
//
//                .subscribe(dao->Log.d("AGA", " recieved from database"+((DeviceEntity)dao).deviceName+" "+((DeviceEntity) dao).id));
//
//
//
//        Log.d("AGA",entitySet.get(1).deviceName);
//
//

    }

    //Heavy construction
    public void goToTestSiteOnClick(View view){
        Intent intent =new Intent(this,TestSiteActivity.class);
        startActivity(intent);
    }






}