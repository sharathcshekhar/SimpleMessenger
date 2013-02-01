package edu.buffalo.cse.cse486586.simplemessenger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	final static String TAG = "MESSENGER";
	final static String SERVER_TAG = "SERVER";
	final static String CLIENT_TAG = "CLIENT";
	
	InetAddress partnersIP; 
	int partnersPort = 0; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		partnersPort = getPartnersPort();
		try {
			partnersIP = InetAddress.getByAddress(new byte[]{10, 0, 2, 2});
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		TextView msgs = (TextView) findViewById(R.id.textView1);
		msgs.append("\n");
		msgs.setBackgroundColor(Color.LTGRAY);
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(10000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new ServerThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

		final EditText user_ip = (EditText) findViewById(R.id.editText1);
		
		user_ip.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					String msg = user_ip.getText().toString() + '\n';
					user_ip.setText("");
					new ClientThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);
					return true;
				}
				return false;
			}
		});
	}

	private int getPartnersPort() {
		// TODO Auto-generated method stub
		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String devId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		if (devId.equals("5554")) {
			return 11112;
		} else if (devId.equals("5556")) {
			return 11108;
		}
		Log.v(TAG, devId);
		return 0;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public class ServerThread extends AsyncTask<ServerSocket, String, Void> {
		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			TextView msgs = (TextView) findViewById(R.id.textView1);
			msgs.setMovementMethod(new ScrollingMovementMethod());
			msgs.append(values[0] + '\n');
			
			Log.v(TAG, values[0]);
		}

		@Override
		protected Void doInBackground(ServerSocket... params) {
			// TODO Auto-generated method stub
			ServerSocket serverSocket = params[0];
			while (true) {
				try {
					// listen for incoming clients
					Log.v(TAG, "Listening");
					Socket clientSocket = serverSocket.accept();
					Log.v(TAG, "Received");
					BufferedReader fromClient;
					fromClient = new BufferedReader(new InputStreamReader(
							clientSocket.getInputStream()));
					Log.v(TAG, "Reading till End of Line");
					String clientMsg = fromClient.readLine();
					Log.v(TAG, clientMsg);
					publishProgress(clientMsg);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}

		/** Called when the user clicks the Send button */
		public void SendMessage(View view) {
			// Do something in response to button
			EditText text = (EditText) findViewById(R.id.editText1);
			String text_str = text.getText().toString();
			// Log.v(TAG, text_str);
			text.setText("");
			new ClientThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, text_str);
		}
	}

	public class ClientThread extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String message = params[0];
			try {
				DataOutputStream toServer;
				Log.v(TAG, "Connecting to server");
				Socket clientSocket = new Socket(partnersIP, partnersPort);
				Log.v(TAG, "Connected to Server");

				toServer = new DataOutputStream(clientSocket.getOutputStream());
				toServer.writeBytes(message + '\n');
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			publishProgress(message);
			return null;
		}

		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			Log.v(TAG, values[0]);
		}
	}
}
