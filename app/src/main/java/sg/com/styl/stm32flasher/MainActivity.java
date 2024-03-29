package sg.com.styl.stm32flasher;


import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnUsbChangeListener, OnFirmwareUpgrade{

    private String TAG = "MainActivity: ";
    Button btnMassErase, btnProgram;
    ProgressBar upgradeProgressbar;
    TextView txtLog;
    ScrollView scrollLog;
    private STM32F042UsbManager m_Stm32F042UsbManager;
    private DeviceFirmwareUpgrade deviceFirmwareUpgrade;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewInXml();
        deviceFirmwareUpgrade = new DeviceFirmwareUpgrade(this, STM32F042UsbManager.getStm32f042UsbVid(), STM32F042UsbManager.getStm32f042UsbPid());
        deviceFirmwareUpgrade.setOnFirmwareUpgrade(this);
        btnMassErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                deviceFirmwareUpgrade.massErase();

            }
        });
        btnProgram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DfuFile dfuFile = new DfuFile();
                dfuFile.filePath = "/storage/emulated/0/Download/YF2.dfu";
                deviceFirmwareUpgrade.setDfuFile(dfuFile);

                deviceFirmwareUpgrade.program();

            }
        });
    }

    private void findViewInXml() {
        btnMassErase = findViewById(R.id.btnMassErase);
        btnProgram = findViewById(R.id.btnProgram);
        txtLog = findViewById(R.id.txtLog);
        upgradeProgressbar = findViewById(R.id.upgradeProgress);
        scrollLog = findViewById(R.id.scrollLog);
        upgradeProgressbar.setProgress(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        m_Stm32F042UsbManager = new STM32F042UsbManager(this);
        m_Stm32F042UsbManager.setUsbManager((UsbManager) getSystemService(Context.USB_SERVICE));
        m_Stm32F042UsbManager.setOnUsbChangeListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(STM32F042UsbManager.ACTION_USB_PERMISSION);
        registerReceiver(m_Stm32F042UsbManager.getUsbBroadcastReceiver(), filter);
        m_Stm32F042UsbManager.requestPermission(this, STM32F042UsbManager.getStm32f042UsbVid(), STM32F042UsbManager.getStm32f042UsbPid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        deviceFirmwareUpgrade.setUsb(null);
        m_Stm32F042UsbManager.release();
        try {
            unregisterReceiver(m_Stm32F042UsbManager.getUsbBroadcastReceiver());
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "onStop: Already unregister");
        }
    }

    @Override
    public void onUsbConnected() {
        Toast.makeText(getApplicationContext(), "STM32F042 Device Connected", Toast.LENGTH_SHORT).show();
        final String deviceInfo = m_Stm32F042UsbManager.getDeviceInfo(m_Stm32F042UsbManager.getUsbDevice());
        deviceFirmwareUpgrade.setUsb(m_Stm32F042UsbManager);
        txtLog.append(deviceInfo);
    }


    @Override
    public void onFirmwareUpgradeLog(String logText) {
        txtLog.append(logText + "\n");
        scrollLog.post(new Runnable() {
            @Override
            public void run() {
                scrollLog.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onUpdateProgressBar(int value) {
        upgradeProgressbar.setProgress(value);
    }
}
