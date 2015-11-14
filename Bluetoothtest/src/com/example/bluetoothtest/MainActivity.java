package com.example.bluetoothtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
public class MainActivity extends Activity {
   private static ScrollView scrollview;
   private TextView msgText,sendEdit;
   static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
   static final UUID uuid = UUID.fromString(SPP_UUID);
   static final String tag = "BtSPP";
   private static final int REQUEST_ENABLE_BT = 1;
   //ToggleButton btSwitch;
   ArrayList<String> devices = new ArrayList<String>();
   ArrayAdapter<String> devAdapter ,adapter1;
   BluetoothAdapter btAdapt;
   BluetoothSocket btSocket;
   InputStream btIn = null;
   OutputStream btOut = null;
   SppServer sppServer;
   boolean sppConnected = false;
   Button myButton0,myButton1,myButton2,myButton3,myButton4,myButton5,myButton6;
   String devAddr = null;
   Spinner spinner1 ;
   private String msg ="";
   
   
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		scrollview = (ScrollView) this.findViewById(R.id.uart_scrollview);
		msgText = (TextView)findViewById(R.id.uart_view);
		sendEdit =(TextView)findViewById(R.id.uart_edit);
		
		myButton0 = (Button)findViewById(R.id.uart_button0);
		myButton1 = (Button)findViewById(R.id.uart_button1);
		myButton2 = (Button)findViewById(R.id.uart_button2);
		myButton3 = (Button)findViewById(R.id.uart_button3);
		myButton4 = (Button)findViewById(R.id.uart_button4);
		myButton5 = (Button)findViewById(R.id.uart_button5);
		myButton6 = (Button)findViewById(R.id.uart_button6);
		
		myButton0.setOnClickListener(myButton0_listener);
		myButton1.setOnClickListener(myButton1_listener);
		myButton2.setOnClickListener(myButton2_listener);
		myButton3.setOnClickListener(myButton3_listener);
		myButton4.setOnClickListener(myButton4_listener);
		myButton5.setOnClickListener(myButton5_listener);
		myButton6.setOnClickListener(myButton6_listener);
		
		spinner1 = (Spinner) findViewById(R.id.uuart_select);
		adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,devices);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(adapter1);
		spinner1.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				devAddr = ((String)devices.get(position).split("\\|")[1]);
				adapterView.setVisibility(View.VISIBLE);
				Toast.makeText(MainActivity.this,"您選擇藍芽裝置:"+ adapterView.getSelectedItem().toString(),Toast.LENGTH_LONG)
				.show();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "您沒有選擇任何藍牙裝置。", Toast.LENGTH_LONG).show();
			}
		});
		
		btAdapt = BluetoothAdapter.getDefaultAdapter(); //初始化藍牙
		//用BroadcastReceiver來取得搜索結果
		IntentFilter intent = new IntentFilter();
		intent.addAction(BluetoothDevice.ACTION_FOUND);
		intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(searchDevices, intent);
		
		//執行Serial Port Profile(SPP)服務Thread
		sppServer = new SppServer();
		sppServer.start();
		
	}
    private BroadcastReceiver searchDevices = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			Bundle b  = intent.getExtras();
			Object[] lstName = b.keySet().toArray();
			
	        //顯示所有收到的資訊及細節
			for(int i =0; i < lstName.length;i++){
				String keyName = lstName[i].toString();
				Log.e(keyName,String.valueOf(b.get(keyName)));
			}
			BluetoothDevice device = null;
			//搜尋設備時，取得設備的MAC位址
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String str =device.getName()+"|"+ device.getAddress();
				if(devices.indexOf(str) == -1){
					devices.add(str);
				}
				adapter1.notifyDataSetChanged();
			}else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				switch ((device.getBondState())) {
				case BluetoothDevice.BOND_BONDING:
					Log.d(tag,"正在配對.....");
					break;
				case BluetoothDevice.BOND_BONDED:
					Log.d(tag,"完成配對");
					break;
				case BluetoothDevice.BOND_NONE:
					Log.d(tag,"取消配對");
				default:
					break;
				}
			}	
		}
    };
    private Button.OnClickListener myButton0_listener = new Button.OnClickListener() {

    	@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			msgText.setText(null);
			sendEdit.setText(null);
			if(sppConnected || devAddr == null)
				return;
			try{
			   btSocket = btAdapt.getRemoteDevice(devAddr).createRfcommSocketToServiceRecord(uuid);
			   btSocket.connect();
			   Log.d(tag,"BT_Socket connect");
			   synchronized (MainActivity.this) {
				if(sppConnected)
					return;
				btServerSocket.close();
				btIn = btSocket.getInputStream();
				btOut = btSocket.getOutputStream();
				conected();
			}
			   Toast.makeText(MainActivity.this,"藍牙裝置已開啟:" + devAddr, Toast.LENGTH_LONG).show();
			}catch(IOException e){
				e.printStackTrace();
				sppConnected =false;
				try{
					btSocket.close();
				}catch(IOException e1){
					e1.printStackTrace();
				}
				btSocket = null;
				Toast.makeText(MainActivity.this, "連接異常", Toast.LENGTH_SHORT).show();
			}
		}
		
	};
	private class SppServer extends Thread {
		public SppServer() {
			// TODO Auto-generated constructor stub
			try{
				btServerSocket = btAdapt.listenUsingRfcommWithServiceRecord("SPP", uuid);
			}catch(IOException e){
				e.printStackTrace();
				btServerSocket =null;
			}
		}
		public void run(){
			BluetoothSocket bs = null;
			if(btServerSocket ==null){
				Log.e(tag,"ServerSocket null");
				return;
			}
			try{
			   bs = btServerSocket.accept();
			   synchronized (MainActivity.this) {
				if(sppConnected)
					return;
				Log.i(tag,"Devices Name:" + bs.getRemoteDevice().getName());
				btIn = bs.getInputStream();
				btOut = bs.getOutputStream();
				conected();
			}
			}catch(IOException e){
				e.printStackTrace();
				Log.d(tag,"ServerSocket accept failed");
			}
			Log.i(tag,"End Bluetooth SPP Server");	
		}
		public void cancel(){
			if(btServerSocket == null)
				return;
			try{
				btServerSocket.close();
			}catch(IOException e){
				e.printStackTrace();
				Log.e(tag,"close ServerSocket Failed");
			}
		}
			
	}
	private void conected() {
		sppConnected =true;
		new SppReceiver(btIn).start();
		spinner1.setClickable(false);
		sppServer = null;
		Log.e(tag, "conected");
	}
	private void disconnect() {
		spinner1.setClickable(true);
		sppConnected = false;
		btIn = null;
		btOut = null;
		sppServer = new SppServer();
		sppServer.start();
		Log.e(tag, "disconnect");
	}
	private class SppReceiver extends Thread{
		private InputStream input = null;
		public SppReceiver(InputStream in){
			input = in;
			Log.i(tag, "SppReceiver");
		}
		/*接收SPP訊息.....*/
		public void run(){
			byte[] data = new byte[1024];
			int length = 0;
			if(input == null){
				Log.d(tag,"InputStream null");
				return;
			}
			while(true){
				try{
					length = input.read(data);
					Log.i(tag,"SPP receiver");
					if(length >0){
						msg = new String(data,0,length,"ASCII")+"\n";
						btHandler.sendEmptyMessage(0);
					}
				}catch(IOException e){
					Log.e(tag,"SppReceiver_disconnect");
					disconnect();
				}
			}
		}
	}
	/*將訊息顯示在TextView中畫面*/
	Handler btHandler = new Handler(){
		public void handleMessage(Message m){
			msgText.append(msg);
			scrollview.fullScroll(ScrollView.FOCUS_DOWN);
		}
	};
	
	private BluetoothServerSocket btServerSocket;
	/*藍牙搜尋*/
	private Button.OnClickListener myButton1_listener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			btAdapt.cancelDiscovery(); //搜尋裝置前先確認藍牙裝置不是處於搜尋中的狀態
			//btAdapt.startDiscovery();
			btAdapt = BluetoothAdapter.getDefaultAdapter();
			if (btAdapt == null) {
			    // Device does not support Bluetooth
				
			}
			if (!btAdapt.isEnabled()) {
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
			
		}
	};
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	/*藍牙開關*/
	private Button.OnClickListener myButton2_listener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
		    if(btAdapt.isEnabled()){
		    	btAdapt.disable();
		    }else {
				Intent intent =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(intent);
			}
		}
		
	};
	
	/*設定藍牙可被偵測*/
	private Button.OnClickListener myButton3_listener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100);
			startActivity(discoverableIntent);
		}
		
	};
	/*送出按鍵*/
	private Button.OnClickListener myButton4_listener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			try{
				msgText.append(sendEdit.getText().toString()+"\n");
				scrollview.fullScroll(ScrollView.FOCUS_DOWN);
				sendEdit.append("\r\n");
				btOut.write(sendEdit.getText().toString().getBytes());
				sendEdit.setText("");
				
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	};
	/*清除按鍵*/
	private Button.OnClickListener myButton5_listener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			msgText.setText(null);
			sendEdit.setText(null);
		}
	};
	/*結束按鍵*/
	private Button.OnClickListener myButton6_listener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			finish();
		}
		
	};
	protected void onPause() {
		super.onPause();
	}
	protected void onStop() {
		super.onStop();
	}
	protected void onDestroy(){
		super.onDestroy();
		if(sppServer != null)
			sppServer.cancel();
		this.unregisterReceiver(searchDevices);
		if(btIn != null){
			try{
				btSocket.close();
				btServerSocket.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	

}
