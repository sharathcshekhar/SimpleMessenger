/*
* Department of Computer Science, University at Buffalo
*
* CSE 586 Project - 1
*
* Author: Sharath Chandrashekhara - sc296@buffalo.edu
* 
* Date: 30th Jan, 2013
* 
* Application: SimpleMessenger
* 
* Description: SimpleMessenger is an Android App to chat between 2 devices.
*
*/
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
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

	final static String TAG = "MESSENGER";
	InetAddress partnersIP; 
	int partnersPort = 0; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		partnersPort = getPartnersPort();
		/*
		 * All communications between AVDs running on the same host
		 * uses the IP 10.0.2.2
		 */
		try {
			partnersIP = InetAddress.getByAddress(new byte[]{10, 0, 2, 2});
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			Log.v(TAG, "Cannot resolve IP address");
			return;
		}
		//Set the header to the Text view as - "Incoming Messages"
		TextView msgs = (TextView) findViewById(R.id.textView1);
		String str = "<b> Incoming Messages </b>";
		msgs.append(Html.fromHtml(str));
		msgs.append("\n\n");
		msgs.setBackgroundColor(Color.LTGRAY);
		ServerSocket serverSocket = null;
		/*
		 * Server socket number is hardcoded to 10000. The Virtual Router has to be set up
		 * to redirect the incoming connections to 10000
		 */
		
		try {
			serverSocket = new ServerSocket(10000);
		} catch (IOException e) {
			e.printStackTrace();
			Log.v(TAG, "Unable to create server socket");
			return;
		}
		//Start the server as AsyncTask
		new ServerThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

		final EditText user_ip = (EditText) findViewById(R.id.editText1);
		
		//Register an Event handler for key pressed
		user_ip.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					String msg = user_ip.getText().toString() + '\n';
					user_ip.setText("");
					new ClientThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);
					// msg consumed
					return true;
				}
				return false;
			}
		});
	}

	private int getPartnersPort() {
		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String phNumber = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		/*
		 * The last 4 digits of the phNumber gives the console port of the AVD
		 * AVD running on console port 5554 is set to redirect connections to port
		 * 11108 to 10000 and AVD running on 5556 is set to redirect connections
		 * to port 11112 to 10000.
		 */
		if (phNumber.equals("5554")) {
			return 11112;
		} else if (phNumber.equals("5556")) {
			return 11108;
		}
		Log.v(TAG, phNumber);
		//invalid AVD
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
			if (!(values[0].trim().equals(""))) {
				msgs.append(values[0] + '\n');
			}
			// These 2 APIs will scroll to the bottom of the textView when a incoming
			// Messages is received
			msgs.setMovementMethod(new ScrollingMovementMethod());
			((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
			Log.v(TAG, values[0]);
		}

		@Override
		protected Void doInBackground(ServerSocket... params) {
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
					clientSocket.close();
				} catch (Exception e) {
					e.printStackTrace();
					//Socket exception, try again
					continue;
				}
			}
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
				clientSocket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Log.v(TAG, "Unable to resole host");
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				Log.v(TAG, "Unable to communicate with the server");
				return null;
			}
			return null;
		}
	}
}