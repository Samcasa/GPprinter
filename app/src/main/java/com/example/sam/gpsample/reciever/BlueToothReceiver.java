package com.example.sam.gpsample.reciever;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.sam.gpsample.observer.BluetoothObserver;
import com.example.sam.gpsample.utils.ClsUtils;
import com.gprinter.command.GpCom;





/*
 *  @文件名:   BlueToothReceiver
 *  @创建者:   sam
 */

public class BlueToothReceiver
        extends BroadcastReceiver
{

    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private static final int REQUEST_PRINT_RECEIPT = 0xfc;
    public static BluetoothDevice mDevice;
    private       Context         context;
    private boolean noDevice=true;

    @Override
    public void onReceive(Context context, Intent intent) {
        //LogUtil.e(TAG, "onReceive---------");
        this.context=context;
        switch(intent.getAction()){
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch(blueState){
                    case BluetoothAdapter.STATE_TURNING_ON:
                        BluetoothObserver.getInstance().stateChanged(BluetoothObserver.STATE_TURNING_OFF);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        BluetoothObserver.getInstance().stateChanged(BluetoothObserver.STATE_TURNING_OFF);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        BluetoothObserver.getInstance().stateChanged(BluetoothObserver.STATE_TURNING_OFF);
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        BluetoothObserver.getInstance().stateChanged(BluetoothObserver.STATE_OFF);
                        break;
                }
                break;
            case GpCom.ACTION_DEVICE_REAL_STATUS:
                int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
                if (requestCode == MAIN_QUERY_PRINTER_STATUS) {
                    int    status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    String str;
                    if (status == GpCom.STATE_NO_ERR) {
                        str = "打印机正常";
                        BluetoothObserver.getInstance().stateChanged(BluetoothObserver.ACTION_DEVICE_REAL_STATUS_NORMAL);
                    } else {
                        str = "打印机 ";
                        if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {
                            str += "脱机,正在重连";
                            showToast(str);
                            BluetoothObserver.getInstance().stateChanged(BluetoothObserver.ACTION_DEVICE_REAL_STATUS_UNNORMAL);
                        }
                        if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {
                            str += "缺纸";
                            showToast(str);
                        }
                        if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {
                            str += "开盖";
                            showToast(str);
                        }
                        if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {
                            str += "出错";
                            showToast(str+"请重启");
                        }
                        if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {
                            str += "查询超时";
                            showToast(str+"请重启");
                        }
                    }

                }else if (requestCode == REQUEST_PRINT_RECEIPT) {
                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    if (status == GpCom.STATE_NO_ERR) {
                        BluetoothObserver.getInstance().stateChanged(BluetoothObserver.SEND_RECEIPT);
                    } else {
                        Toast.makeText(context, "query printer status error", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case BluetoothDevice.ACTION_FOUND:
                mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    if (mDevice.getName().contains("Feasycom") || mDevice.getName().contains("Printer")) {
                        noDevice=false;
                        mDevice.getName();
                        ClsUtils.createBond(mDevice.getClass(), mDevice);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                if(noDevice){
                    showToast("没有找到打印机,请开启或重启打印机");
                }
                break;
            case BluetoothDevice.ACTION_PAIRING_REQUEST:
                if (mDevice.getName().contains("Feasycom") || mDevice.getName().contains("Printer"))
                {
                    try {
                        ClsUtils.setPairingConfirmation(mDevice.getClass(), mDevice, true);
                        abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                        //3.调用setPin方法进行配对...
                        boolean ret = ClsUtils.setPin(mDevice.getClass(), mDevice, "0000");
                        if (ret) {
                            Toast.makeText(context, "配对成功", Toast.LENGTH_LONG)
                                 .show();
                            if(mDevice.getName().contains("Feasycom")){
                                BluetoothObserver.getInstance().stateChanged(BluetoothObserver.FEASYCOM_PAIRED);
                            }else{
                                BluetoothObserver.getInstance().stateChanged(BluetoothObserver.GP_PAIRED);
                            }

                        } else {
                            Toast.makeText(context, "配对失败", Toast.LENGTH_LONG)
                                 .show();
                        }


                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                break;

        }
    }

    private void showToast(String msg){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

}
