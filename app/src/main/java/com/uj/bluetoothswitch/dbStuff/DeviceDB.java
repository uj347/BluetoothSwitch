package com.uj.bluetoothswitch.dbStuff;

import android.content.Context;
import android.util.AndroidRuntimeException;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.ReplaySubject;


@Database(entities = {DeviceEntity.class},version = 1)
public abstract class DeviceDB extends RoomDatabase {
    private static final String TAG="DBClass";

    public abstract DeviceDAO deviceDAO();


static volatile   private DeviceDB instance;


    static public synchronized DeviceDB getInstance(Context activity){
        Log.d(TAG, "New Invocation of getInstance of DB");
    if(instance==null){
       instance=Room.databaseBuilder(activity.getApplicationContext(),DeviceDB.class,"btDevices.db")
               .build();
        }
        DeviceEntity[] initialDevices={DeviceEntity.getEntityFor("JBL Tune 115","68:D6:ED:14:17"),
                DeviceEntity.getEntityFor("Tronsmart T6","FC:58:FA:C1:03:29")};

        ArrayList<DeviceEntity> contentsOfDB=new ArrayList<>();
//        Completable.create(e->{
//           contentsOfDB.addAll(instance.deviceDAO().getAll());
//           e.onComplete();
//        })
//                .subscribeOn(Schedulers.io())
//                .blockingSubscribe(()-> Log.d(TAG, "Completable completed"));



            Observable.fromArray(initialDevices)
                    .subscribeOn(Schedulers.io())
                    .filter(entity -> !instance.deviceDAO().getAll().contains(entity))
                    .collect(ArrayList<DeviceEntity>::new, (arl, entity) -> arl.add(entity))
                    .doOnSuccess(o -> {Log.d(TAG, "Succes happened on " + Thread.currentThread().getName() + " inserted in DB: " + o);
                    })
                    .map(arlEntity -> instance.deviceDAO().insertAll(arlEntity.toArray(new DeviceEntity[0])).subscribe())

                    .blockingSubscribe(obj-> Log.d(TAG, "Finally i've done it?"));

       //TODO Почему то не происходит сабскрайба
//        Single.<List<DeviceEntity>>fromSupplier(instance.deviceDAO()::getAll)
//                .subscribeOn(Schedulers.io())
//                .flatMapObservable(Observable::fromIterable)
//                .observeOn(AndroidSchedulers.mainThread())
//                .collectInto(contentsOfDb,(contents, entity)->contents.add(entity))
//                .subscribe(l-> Log.d(TAG, "Seems like succes"));

        Log.d(TAG, "Contents of contentsofDB: "+ contentsOfDB);

//        for (DeviceEntity entity:
//                initialDevices) {
//            if (!contentsOfDB.contains(entity))
//                Log.d(TAG, "Insertion of new Entities in DB in getInstance");
//                instance.deviceDAO().insertAll(entity)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe();
//        }
        

     return instance;

    }









private ReplaySubject<List<DeviceEntity>> updateListSubject=ReplaySubject.create();


    //TODO
public Observer<DeviceEntity> getDeleteObserver(){
        return new Observer<DeviceEntity>() {
            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

            }

            @Override
            public void onNext(@io.reactivex.rxjava3.annotations.NonNull DeviceEntity deviceEntity) {
deviceDAO().delete(deviceEntity);
updateListSubject.onNext(deviceDAO().getAll());
            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
e.printStackTrace();
            }

            @Override
            public void onComplete() {

            }
        };
}
//TODO
public Observer<DeviceEntity> getInsertObserver(){
        return  new Observer<DeviceEntity>() {
            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

            }

            @Override
            public void onNext(@io.reactivex.rxjava3.annotations.NonNull DeviceEntity deviceEntity) {
deviceDAO().insertAll(deviceEntity);
                updateListSubject.onNext(deviceDAO().getAll());

            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
}

public ReplaySubject<List<DeviceEntity>> getUpdateListSubject(){
        return updateListSubject;
}
}




