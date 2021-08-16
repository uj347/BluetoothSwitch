package com.uj.bluetoothswitch.serviceparts;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;

public interface IConnection {
    Observable<String> getExternalInputHook();

    Observer<String> getExternalOutputHook();
}
