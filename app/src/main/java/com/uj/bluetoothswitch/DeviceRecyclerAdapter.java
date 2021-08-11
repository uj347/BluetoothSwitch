package com.uj.bluetoothswitch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;



import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;

import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.ReplaySubject;

public class DeviceRecyclerAdapter extends RecyclerView.Adapter  {
    private final static String TAG= "RecyclerAdapter";
    private List<DeviceEntity>bluetoothDevices=new ArrayList<>();
    private DeviceDB deviceDB;
    //TODO
    private ReplaySubject <DeviceEntity> notifyDeleteSubject= ReplaySubject.create();
    private ReplaySubject <List<DeviceEntity>> dbUpdatedNotifySubj;
       //TODO
    private Observer <List<DeviceEntity>>updateOflistNeededObserver=new Observer<List<DeviceEntity>>() {
        @Override
        public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

        }

        @Override
        public void onNext(@io.reactivex.rxjava3.annotations.NonNull List<DeviceEntity> deviceEntities) {
setBluetoothDevices(deviceEntities);

        }

        @Override
        public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
e.printStackTrace();
        }

        @Override
        public void onComplete() {

        }
    };
    private Observer<DeviceEntity>dbDeleteObserver,dbInsertObserver;







    private synchronized List<DeviceEntity> getBluetoothDevices() {
        return bluetoothDevices;
    }

    private synchronized void setBluetoothDevices(List<DeviceEntity> bluetoothDevices) {
        this.bluetoothDevices = bluetoothDevices;
        this.notifyDataSetChanged();
    }


    public DeviceRecyclerAdapter (Context context){
        this.deviceDB=DeviceDB.getInstance(context);
        this.dbDeleteObserver= deviceDB.getDeleteObserver();
        this.dbUpdatedNotifySubj =deviceDB.getUpdateListSubject();
        this.dbInsertObserver=deviceDB.getInsertObserver();



        dbUpdatedNotifySubj
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updateOflistNeededObserver);

        notifyDeleteSubject
                .observeOn(Schedulers.io())
                .subscribe(dbDeleteObserver);
        dbUpdatedNotifySubj
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updateOflistNeededObserver);
        Completable.complete()
                .observeOn(Schedulers.io())
                .subscribe(()->dbUpdatedNotifySubj
                        .onNext(deviceDB.deviceDAO().getAll()));



//Тестовая заглушка

//        HashSet<DeviceEntity> entitySet=new HashSet<>();
//        for (int inta = 0; inta < 3; inta++) {
//            entitySet.add(DeviceEntity.getEntityFor(("test"+inta),("testAdress"+inta)));
//        }
//        setBluetoothDevices(entitySet);


    }



    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull  ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.device_recycler_element,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull  RecyclerView.ViewHolder holder, int position) {
MyViewHolder myViewHolder=(MyViewHolder)holder;
myViewHolder.setViewHolderToDevice(getBluetoothDevices().get(position));
    }

    @Override
    public int getItemCount() {
        return getBluetoothDevices().size();
    }



    class MyViewHolder extends RecyclerView.ViewHolder{
        private DeviceEntity bluetoothDevice;
        private TextView textView;
        private Button deleteButton;



        public void setViewHolderToDevice (DeviceEntity bluetoothDevice){
            this.bluetoothDevice=bluetoothDevice;
             this.textView.setText(bluetoothDevice.deviceName+"\n"+bluetoothDevice.macAdress);

        }


        public MyViewHolder(@NonNull  View itemView) {
        super(itemView);
        this.textView=(TextView) itemView.findViewById(R.id.nameText);
        this.deleteButton=(Button) itemView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener((View view)->{
//TODO
                if(bluetoothDevice!=null)
                    notifyDeleteSubject.onNext(bluetoothDevice);
            });;


    }
}


}

