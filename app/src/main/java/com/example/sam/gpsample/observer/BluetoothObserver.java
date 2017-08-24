package com.example.sam.gpsample.observer;

import android.util.Log;

import java.util.Observable;


/*
 *  @创建者:   sam
 */

public class BluetoothObserver
        extends Observable
{
    public static final int SEND_RECEIPT                  = 1;
    public static final int FEASYCOM_PAIRED               = 2;
    public static final int GP_PAIRED                     = 3;
    public static final int STATE_TURNING_OFF             = 4;
    public static final int STATE_OFF                     = 5;
    public static final int ACTION_DEVICE_REAL_STATUS_NORMAL     = 6;
    public static final int ACTION_DEVICE_REAL_STATUS_UNNORMAL = 7;
    private static BluetoothObserver mBluetoothObserver;
    private BluetoothObserver(){}

    public static BluetoothObserver getInstance(){
        if(mBluetoothObserver==null){
            synchronized (BluetoothObserver.class){
                if (mBluetoothObserver==null){
                    mBluetoothObserver=new BluetoothObserver();
                }
            }
        }
        return mBluetoothObserver;
    }

    public void stateChanged(int state){
        setChanged();
        Log.d("aaaa", "setchanged");
        notifyObservers(state);
        Log.d("aaaa", "notifyObservers");
    }
}
