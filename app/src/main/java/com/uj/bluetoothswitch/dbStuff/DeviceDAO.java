package com.uj.bluetoothswitch.dbStuff;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.reactivestreams.Subscriber;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface DeviceDAO {
    @Query("SELECT * FROM DeviceEntity")
    LiveData<List<DeviceEntity>> getAllLivedata();


    @Query("SELECT * FROM DeviceEntity")
    Single<List<DeviceEntity>> getAllRXSingle();


    @Query("SELECT * FROM deviceentity WHERE device_name LIKE :deviceName LIMIT 1 ")
    DeviceEntity getByName(String deviceName);


    @Query("SELECT * FROM deviceentity WHERE id LIKE :id LIMIT 1 ")
    Single<DeviceEntity> getByID(Integer id);

    @Query("UPDATE deviceentity SET device_name = :name, mac_Adress= :mac WHERE id = :id")
    Completable updateByID( int id, String name, String mac);


    @Insert
    Completable insertAll(DeviceEntity... deviceEntities);

    @Delete
    Completable delete(DeviceEntity deviceEntity);





 }
