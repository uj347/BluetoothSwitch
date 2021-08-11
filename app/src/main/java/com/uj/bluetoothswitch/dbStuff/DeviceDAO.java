package com.uj.bluetoothswitch.dbStuff;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import org.reactivestreams.Subscriber;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;

@Dao
public interface DeviceDAO {
    @Query("SELECT * FROM DeviceEntity")
    List<DeviceEntity> getAll();



    @Query("SELECT * FROM deviceentity WHERE device_name LIKE :deviceName LIMIT 1 ")
    DeviceEntity getByName(String deviceName);


    @Query("SELECT * FROM deviceentity WHERE id LIKE :id LIMIT 1 ")
    DeviceEntity getByID(Integer id);





    @Insert
    Completable insertAll(DeviceEntity... deviceEntities);

    @Delete
    void delete(DeviceEntity deviceEntity);



 }
