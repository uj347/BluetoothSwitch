package com.uj.bluetoothswitch.serviceparts.connectionpart;

import io.reactivex.rxjava3.core.Single;

public interface IInquirer<T> {

    Single<Boolean> makeInquiries(String whatAboutMAC, T... devicesToConnect);

    void stopInqueries();


}
