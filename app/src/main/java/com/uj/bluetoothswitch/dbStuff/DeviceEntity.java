package com.uj.bluetoothswitch.dbStuff;


import android.bluetooth.BluetoothAdapter;
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

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "device_name")
    public String deviceName;

    @ColumnInfo(name = "mac_Adress")
    public String macAdress;

    @Override
    public boolean equals(@Nullable @org.jetbrains.annotations.Nullable Object other) {
        if (!(other instanceof DeviceEntity)) {
            return false;
        }
        if (other == null) {
            return false;
        }
        DeviceEntity thisInstance = this;
        DeviceEntity otherInstance = (DeviceEntity) other;

        return thisInstance.macAdress.equals(otherInstance.macAdress);
//        return other instanceof DeviceEntity
//                &&this.deviceName.equals(((DeviceEntity) other).deviceName)
//                &&this.macAdress.equals(((DeviceEntity)other).macAdress);
    }

    public synchronized String getDeviceName() {
        return deviceName;
    }

    private synchronized void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public synchronized String getMacAdress() {
        return macAdress;
    }

    private synchronized void setMacAdress(String macAdress) {
        this.macAdress = macAdress;
    }

    @NonNull
    @NotNull
    @Override
    public String toString() {
        return "[DeviceEntity: " + this.deviceName + ": " + this.macAdress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.macAdress);
    }

    static public DeviceEntity getEntityFor(String name, String macAdress) {
        if (macAdress == null || !BluetoothAdapter.checkBluetoothAddress(macAdress)) {
            throw new RuntimeException("You trying to create DeviceEntity without proper MAC");
        }
        DeviceEntity entity = new DeviceEntity();
        if (name == null || name.isEmpty()) {
            entity.setDeviceName("NONAME");
            entity.setMacAdress(macAdress);
            return entity;
        }
        entity.setDeviceName(name);
        entity.setMacAdress(macAdress);
        return entity;

    }

}
