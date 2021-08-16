package com.uj.bluetoothswitch.serviceparts;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.subjects.Subject;

public interface IListener extends IConnection{

    void startListening();

    void stopListening();

    boolean isRunning();


}
