package com.uj.bluetoothswitch.dbStuff;


import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Entity
public class DeviceEntity {

    @PrimaryKey (autoGenerate = true)
    public int id;

    @ColumnInfo(name = "device_name")
    public String deviceName;

    @ColumnInfo(name = "mac_Adress")
    public String macAdress;

    @Override
    public boolean equals(@Nullable @org.jetbrains.annotations.Nullable Object other) {
        return other instanceof DeviceEntity
                &&this.deviceName.equals(((DeviceEntity) other).deviceName)
                &&this.macAdress.equals(((DeviceEntity)other).macAdress);
    }

    @NonNull
    @NotNull
    @Override
    public String toString() {
        return "[DeviceEntity: "+this.deviceName+": "+this.macAdress;
    }

    @Override
    public int hashCode() {
return Objects.hash(this.deviceName,this.macAdress);
    }

    static public DeviceEntity getEntityFor(String name, String macAdress){
        DeviceEntity entity=new DeviceEntity();

        entity.deviceName=name;
        entity.macAdress=macAdress;
        return entity;
    }

}
