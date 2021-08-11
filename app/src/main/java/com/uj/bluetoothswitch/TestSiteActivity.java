package com.uj.bluetoothswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.dbStuff.DeviceEntity;
import com.uj.bluetoothswitch.disposables.StringMessageIOProcessors;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
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
    private static final Byte[] KEYPATTERN=new Byte[]{125,125,125};

    private DeviceDB deviceDB;

      Button b;
      TextView t;
      Set<DeviceEntity> entitySet;
      private static final BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();


    private CompositeDisposable senderSubscriptions =new CompositeDisposable();
    private CompositeDisposable senderSocketeerSubscriptions =new CompositeDisposable();
    private CompositeDisposable recieverSubscriptions= new CompositeDisposable();


    private final PublishSubject<String> buttonSubject=PublishSubject.create();

    private void subscribeReciever(){
        Observer recieveObserver=getNewRecieverObserver();
        Observable<String> recieverObservable = getNewRecieverObservable();
                recieverObservable.subscribeOn(Schedulers.io())
                .doOnError(e->{
                    subscribeReciever();
                })
                //Временная затычка в виду отсутсвия нормальной маршрутизации между режимами
                .doOnComplete(this::subscribeReciever)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(recieveObserver);
    }

private  Observer<String>getNewRecieverObserver(){
    return new Observer<String>() {
        Disposable disposable;
        @Override
        public void onSubscribe(@NonNull Disposable d) {
            disposable=d;
            recieverSubscriptions.add(d);
        }

        @Override
        public void onNext(@NotNull String str) {

            Toast.makeText(TestSiteActivity.this,"Recieved message:   "+str ,Toast.LENGTH_LONG).show();

        }

        @Override
        public void onError(@NonNull Throwable e) {
            Log.d(TAG, "Recieved error in reciever:  "+ e.getMessage());
            if(disposable!=null)
                disposable.dispose();

        }

        @Override
        public void onComplete() {
            Log.d(TAG, "in reciever oncomplete, disposing resources");
            if(disposable!=null)
                disposable.dispose();
        }
    };


}






private Observable<String> getNewRecieverObservable(){
        return Observable.using(
                ()->{
                    BluetoothSocket inputBlueSocket;
                     adapter.cancelDiscovery();
                    BluetoothServerSocket serverSocket= adapter.listenUsingRfcommWithServiceRecord(NAME,MYUUID);
                    Log.d(TAG, "SSocket created");
                    inputBlueSocket=serverSocket.accept();
                    Log.d(TAG, "inputBlueSocketObtained");
                    serverSocket.close();
                    Log.d(TAG, "SSocket disposed");
                    return inputBlueSocket;},
                (inputBlueSocket)->{return Observable.create( emitter->{

                    InputStream blueInputStream=inputBlueSocket.getInputStream();
                    //TODO Проверить нормально ли рабоатет нижняя строка
                    while(!emitter.isDisposed()){
                        if (inputBlueSocket.isConnected()) {
                            String msg = StringMessageIOProcessors.extract(blueInputStream, KEYPATTERN);
                            if (msg.equalsIgnoreCase("killpill")) {
                                Log.d(TAG, "KillPillRecieved ");
                                emitter.onComplete();
                            }
                            Log.d(TAG, "In recieverObservable obtained message " + msg);
                            emitter.onNext(msg);
                        }
                    }
                });},
                (inputBlueSocket)->{inputBlueSocket.close();
                    Log.d(TAG, "inputBlueSocketDisposed");
                }
        );
    }



    private Observer<String> senderSocketeerObserver=new Observer<String>() {
        @Override
        public void onSubscribe(@NonNull Disposable d) {
          senderSocketeerSubscriptions.add(d);
        }

        @Override
        public void onNext(@NonNull String s) {
            if(s.equals("plansh")||s.equals("phone")) {
                String deviceMAC = s.equals("plansh") ? PLANSHMAC : PHONEMAC;
                buttonSubject.observeOn(Schedulers.io()).subscribe(getSenderObserver(deviceMAC));
            }
        }

        @Override
        public void onError(@NonNull Throwable e) {
            Log.d(TAG, "onError in socketeer: "+ e.getMessage());
        }

        @Override
        public void onComplete() {
            Log.d(TAG, "Sender Socketeer onComplete:");
        }
    };

      private Observer<String> getSenderObserver (String deviceMAC) {
          return new Observer<String>() {
              BluetoothSocket outSocket;
              OutputStream outputStream;



              @Override
              public void onSubscribe(@NonNull Disposable d) {

                  Log.d(TAG, "on Subscribe in sender");
                  senderSocketeerSubscriptions.dispose();
                 senderSubscriptions.add(d);

                  try {
                      adapter.cancelDiscovery();
                      outSocket = adapter.getRemoteDevice(deviceMAC).createRfcommSocketToServiceRecord(MYUUID);
                      while (!outSocket.isConnected()){
                          outSocket.connect();
                      }
                      outputStream=outSocket.getOutputStream();

                      //Постоянно чекает подключен ли исходящий сокет и кончает сендера, если сокет помер
                      Single.<Integer>create(
                             emitter -> {
                                while (!emitter.isDisposed()) {
                                    if (outSocket != null) {
                                        if (!outSocket.isConnected())
                                            emitter.onSuccess(1);

                                    }
                                }

                             })
                              .subscribeOn(Schedulers.newThread())
                              .subscribe(new SingleObserver<Integer>(){
                                  @Override
                                  public void onSubscribe(@NonNull Disposable d) {
                                      senderSubscriptions.add(d);
                                  }

                                  @Override
                                  public void onSuccess(@NonNull Integer integer) {
                                      Log.d(TAG, "Dropping outputConnection because of disconection of the outputBlueSocket");
                                      onComplete();

                                  }

                                  @Override
                                  public void onError(@NonNull Throwable e) {
                                      Log.d(TAG, "Error in connection observer ");
                                  }
                              });

                  } catch (IOException e) {
                      Log.d(TAG, "Error in ConnectionCreation");
                      onError(e);
                  }
              }

              @Override
              public void onNext(@NonNull String s) {
                  try {
                      if (s.equalsIgnoreCase("selfkill"))
                          onComplete();
                      StringMessageIOProcessors.send(s,outputStream,KEYPATTERN);
                  } catch (IOException e) {
                      onError(e);
                  }

              }

              @Override
              public void onError(@NonNull Throwable e) {
                  Log.d(TAG, "Eror in senderObserver: " + e.getMessage());
                  finalization();
              }


              @Override
              public void onComplete() {

                  Log.d(TAG, "in sender onComple, initializin finilization");
                  finalization();
              }

              private void finalization(){
                  Log.d(TAG, "finalization initialized ");
                      if(outputStream!=null){
                          try{outputStream.close();
                              Log.d(TAG, "finalization: out stream closed");
                          }catch (IOException e){
                              Log.d(TAG, "Error in outputStream closing in senderObserver");
                          }
                      }

                      if (outSocket != null) {
                          try {

                              outSocket.close();
                              Log.d(TAG, "finalization: outSocket closed");
                          } catch (IOException ioException) {
                              Log.d(TAG, "Error on socketClosing");
                          }
                      }
//TODO Нужно будет здесь заменить статичный обсервер на динамичный
                  //TODO ТАк же нужно будет разобраться с дублирующимся файналайзом
                      buttonSubject.observeOn(Schedulers.io()).subscribe(senderSocketeerObserver);
                      senderSubscriptions.dispose();
                  }

              }

              ;
          };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_site);
        b=findViewById(R.id.testButton);
        t=findViewById(R.id.testText);

    subscribeReciever();


        b.setOnClickListener((view)->{
            String text=t.getText().toString().trim();
            buttonSubject.onNext(text);
        });

        buttonSubject.observeOn(Schedulers.io()).subscribe(senderSocketeerObserver);



    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        CompositeDisposable[] compDispArray={
                recieverSubscriptions,
            senderSocketeerSubscriptions,
            senderSubscriptions
        };

        for (CompositeDisposable compDisp:compDispArray
             ) {
            if(compDisp!=null){
                compDisp.dispose();
            }
        }
    }
}