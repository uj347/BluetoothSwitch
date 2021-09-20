package com.uj.bluetoothswitch.disposables;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class  Triple <F,S,T> {
    public final F first;
    public final S second;
    public final T third;
    public Triple(F f,S s, T t){
        this.first=f;
        this.second=s;
        this.third =t;
    }
    @NonNull
    @Override
    public String toString(){
        ArrayList <Object>objects=new ArrayList<Object>();
        objects.add(first);
        objects.add(second);
        objects.add(third);
        return objects.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return first.equals(triple.first) && second.equals(triple.second) && third.equals(triple.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }
}
