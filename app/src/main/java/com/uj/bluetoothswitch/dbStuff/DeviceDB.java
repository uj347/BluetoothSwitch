package com.uj.bluetoothswitch.dbStuff;

import android.content.Context;
import android.util.AndroidRuntimeException;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.ReplaySubject;


@Database(entities = {DeviceEntity.class},version = 1)
public abstract class DeviceDB extends RoomDatabase {
    private static final String TAG="DBClass";

    public abstract DeviceDAO deviceDAO();


static volatile   private DeviceDB instance;


    static public synchronized DeviceDB getInstance(Context context){
        Log.d(TAG, "New Invocation of getInstance of DB");
    if(instance==null){
       instance=Room.databaseBuilder(context.getApplicationContext(),DeviceDB.class,"btDevices.db")
               .build();
        }
        DeviceEntity[] initialDevices={DeviceEntity
                                          .getEntityFor("JBL Tune 115","68:D6:ED:14:17:35"),
                                       DeviceEntity
                                          .getEntityFor("Tronsmart T6","FC:58:FA:C1:03:29")
    };

        List<DeviceEntity> devicesFromDB= instance.deviceDAO()
                .getAllRXSingle()
                .subscribeOn(Schedulers.io())
                //Debug
                .doOnSuccess(list->{
                    Log.d(TAG, "contents of DB: "+ list);
                })
                .blockingGet();
        //TODO Возможно вот вся эта хуйня не работает, ну хотя бы не крашится и на том спасибо


        Stream.of(new ArrayList<DeviceEntity>(Arrays.asList(initialDevices)))
                .flatMap(list->{
                    list.removeAll(devicesFromDB);
                    return list.stream();})
                .forEach(entity->instance.deviceDAO().insertAll(entity)
                .subscribeOn(Schedulers.io())
                .subscribe()
        );




        Log.d(TAG, "Contents of contentsofDB: "+ devicesFromDB);

     return instance;

    }

}




