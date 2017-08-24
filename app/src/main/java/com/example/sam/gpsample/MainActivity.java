package com.example.sam.gpsample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.sam.gpsample.observer.BluetoothObserver;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.service.GpPrintService;

import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;

public class MainActivity
        extends AppCompatActivity implements Observer, View.OnClickListener
{

    private BluetoothAdapter mBtAdapter;
    private PrinterServiceConnection conn;
    private GpService mGpService;
    private static final int                      MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private static final int                      REQUEST_PRINT_LABEL       = 0xfd;
    private static final int                      REQUEST_PRINT_RECEIPT     = 0xfc;
    private BluetoothDevice mDevice;

    class PrinterServiceConnection
            implements ServiceConnection
    {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("ServiceConnection", "onServiceDisconnected() called");
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = GpService.Stub.asInterface(service);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button test= (Button) findViewById(R.id.test);
        BluetoothObserver.getInstance().addObserver(this);
        test.setOnClickListener(this);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }



    @Override
    public void update(Observable observable, Object o) {
        int type= (int) o;
        switch (type){
            case BluetoothObserver.SEND_RECEIPT:
                sendReceipt();
                break;
            case BluetoothObserver.ACTION_DEVICE_REAL_STATUS_NORMAL:
                gpPrint();
                break;
            case BluetoothObserver.ACTION_DEVICE_REAL_STATUS_UNNORMAL:
                //发现状态错误
                //1.先看看有没有配对
                doGetBondedDevices();
                if(mDevice!=null){
                    //配对了
                    connectOrDisConnectToDevice();
                }else{
                    //去配对
                    mBtAdapter.startDiscovery();
                }

                break;
            case BluetoothObserver.GP_PAIRED:
                Log.d("aaaa","11111");
                connectOrDisConnectToDevice();
                break;
        }
    }

    void connectOrDisConnectToDevice() {
        int rel = 0;
        try {
            mGpService.closePort(0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            rel = mGpService.openPort(0, 4, mDevice.getAddress(), 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onClick(View view) {
        checkGPprinter();
    }

    private void checkGPprinter() {
        try {
            mGpService.queryPrinterStatus(0, 500, MAIN_QUERY_PRINTER_STATUS);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void doGetBondedDevices() {
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().contains("Printer")||device.getName().contains("Feasycom")) {
                    this.mDevice = device;
                }
            }
        }
    }

    private void gpPrint() {
        try {
            int type = mGpService.getPrinterCommandType(0);
            if (type == GpCom.ESC_COMMAND) {
                mGpService.queryPrinterStatus(0, 1000, REQUEST_PRINT_RECEIPT);
            } else {
                Toast.makeText(this, "Printer is not receipt mode", Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    void sendReceipt() {

        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
        esc.addSelectPrintModes(EscCommand.FONT.FONTA,
                                EscCommand.ENABLE.ON,
                                EscCommand.ENABLE.ON,
                                EscCommand.ENABLE.ON,
                                EscCommand.ENABLE.OFF);// 设置为倍高倍宽
        esc.addTurnDoubleStrikeOnOrOff(EscCommand.ENABLE.ON);
        esc.addText("中国邮政储蓄银行\n"); // 打印文字
        esc.addPrintAndFeedLines((byte) 2);
        esc.addTurnDoubleStrikeOnOrOff(EscCommand.ENABLE.OFF);
        esc.addSelectPrintModes(EscCommand.FONT.FONTB,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.ON,
                                EscCommand.ENABLE.ON,
                                EscCommand.ENABLE.OFF);// 取消倍高倍宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        esc.addText("排队号/Line Num:5\n");
        esc.addPrintAndFeedLines((byte) 1);
        esc.addSelectPrintModes(EscCommand.FONT.FONTB,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.OFF);
        esc.addText("业务类型/business: 贵宾业务/VIP\n");
        esc.addPrintAndFeedLines((byte) 1);
        esc.addSelectPrintModes(EscCommand.FONT.FONTB,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.ON,
                                EscCommand.ENABLE.ON,
                                EscCommand.ENABLE.OFF);// 取消倍高倍宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        esc.addText("等待人数/Waiting:5\n");
        esc.addPrintAndFeedLines((byte) 1);
        esc.addSelectPrintModes(EscCommand.FONT.FONTB,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.OFF,
                                EscCommand.ENABLE.OFF);
        esc.addText("时间/Time: 2017年８月５号\n");
        esc.addPrintAndFeedLines((byte) 1);
        esc.addText("温馨提示/Tips:注意叫号,过号作废");
        esc.addPrintAndFeedLines((byte) 3);
        esc.addPrintAndLineFeed();

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[]       bytes = GpUtils.ByteTo_byte(datas);
        String       sss   = Base64.encodeToString(bytes, Base64.DEFAULT);
        int          rs;
        try {
            rs = mGpService.sendEscCommand(0, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                // Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
