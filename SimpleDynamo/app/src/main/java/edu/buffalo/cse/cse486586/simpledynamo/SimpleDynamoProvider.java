package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.SortedMap;
import java.util.TreeMap;

public class SimpleDynamoProvider extends ContentProvider {

	static final String TAG = SimpleDynamoProvider.class.getSimpleName();
	static final String REMOTE_PORT0 = "11108";
	static final String REMOTE_PORT1 = "11112";
	static final String REMOTE_PORT2 = "11116";
	static final String REMOTE_PORT3 = "11120";
	static final String REMOTE_PORT4 = "11124";
	static final int SERVER_PORT = 10000;
	Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledynamo.provider");
	String portStr = "empty";
	String myPort = "empty";
	String genHash_Port = "empty";
	String node1 = "empty";
	String node2 = "empty";
	String node3 = "empty";
	String nextNode = "empty";
	String nextNode2 = "empty";
	String previousNode1 = "empty";
	String previousNode2 = "empty";

	SortedMap<String, String> hashedNodes = new TreeMap<String, String>();
	ArrayList<String> messageValues = new ArrayList<String>();
	ArrayList<String> messageKeys = new ArrayList<String>();

	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		if(selection.equals("@"))
		{
			File dirName = getContext().getFilesDir();
			File files[] = dirName.listFiles();
			for (File key : files)
			{
				key.delete();

			}
		}
		else if(selection.equals("*")) {
			try {
				for (String node : hashedNodes.values()) {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(node));
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeUTF("DeleteAll" + "//" + nextNode + "//" + myPort);
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			//Multiple nodes, single key case.
//			this.findKeyPosition(selection);
			String hashedKey = "";

			try {
				hashedKey = genHash(selection);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			messageKeys.add(hashedKey);
			Collections.sort(messageKeys);
			int keyIndex = messageKeys.indexOf(hashedKey);

			if(keyIndex == 3) {
				node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
				node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
				node3 = hashedNodes.get(messageKeys.get(0));
			}
			else if(keyIndex == 4) {
				node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
				node2 = hashedNodes.get(messageKeys.get(0));
				node3 = hashedNodes.get(messageKeys.get(1));
			}
			else if(keyIndex == 5) {
				node1 = hashedNodes.get(messageKeys.get(0));
				node2 = hashedNodes.get(messageKeys.get(1));
				node3 = hashedNodes.get(messageKeys.get(2));
			}
			else {
				node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
				node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
				node3 = hashedNodes.get(messageKeys.get(keyIndex+3));
			}
			messageKeys.remove(keyIndex);

			ArrayList<String> keyNodes = new ArrayList<String>();
			keyNodes.add(node1);
			keyNodes.add(node2);
			keyNodes.add(node3);

			for (String node : keyNodes) {
				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(node));

					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeUTF("DeleteOne" + "//" + selection + "//" + myPort);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		try
		{
			Thread.sleep(500);
		}catch (Exception e)
		{

		}

		Object key = null;
		Object value = null;
		for (String k : values.keySet())
		{
			key = values.get("key");
			value = values.get("value");
		}

//		this.findKeyPosition(key.toString());
		String hashedKey = "";

		try {
			hashedKey = genHash(key.toString());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		messageKeys.add(hashedKey);
		Collections.sort(messageKeys);
		int keyIndex = messageKeys.indexOf(hashedKey);

		if(keyIndex == 3) {
			node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
			node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
			node3 = hashedNodes.get(messageKeys.get(0));
		}
		else if(keyIndex == 4) {
			node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
			node2 = hashedNodes.get(messageKeys.get(0));
			node3 = hashedNodes.get(messageKeys.get(1));
		}
		else if(keyIndex == 5) {
			node1 = hashedNodes.get(messageKeys.get(0));
			node2 = hashedNodes.get(messageKeys.get(1));
			node3 = hashedNodes.get(messageKeys.get(2));
		}
		else {
			node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
			node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
			node3 = hashedNodes.get(messageKeys.get(keyIndex+3));
		}
		messageKeys.remove(keyIndex);

//		System.out.println("For port: "+myPort+", Node 1 is: "+ node1+", Node 2 is: "+ node2+", Node 3 is: "+ node3+" & key is: "+key);

		String packetToSend = "insertmsg" + "//" + node1 + "//" + node2 + "//" + node3 + "//" + key + "//" + value;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, packetToSend);

		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		File dirName = getContext().getFilesDir();
		if (dirName.isDirectory())
		{
			String[] files = dirName.list();
			for (int i = 0; i < files.length; i++)
			{
				System.out.println("deleting files in on create");
				new File(dirName, files[i]).delete();
			}
		}

		try
		{
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
			genHash_Port = genHash(portStr);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		try {
			hashedNodes.put(genHash("5554"), REMOTE_PORT0);
			hashedNodes.put(genHash("5556"), REMOTE_PORT1);
			hashedNodes.put(genHash("5558"), REMOTE_PORT2);
			hashedNodes.put(genHash("5560"), REMOTE_PORT3);
			hashedNodes.put(genHash("5562"), REMOTE_PORT4);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		for (String key : hashedNodes.keySet())
		{
			messageKeys.add(key);
		}

		for (String value : hashedNodes.values())
		{
			messageValues.add(value);
		}
		int currIndex = messageKeys.indexOf(genHash_Port);

		if(currIndex == messageKeys.size()-1) {
			nextNode = hashedNodes.get(messageKeys.get(0));
			nextNode2 = hashedNodes.get(messageKeys.get(1));
		}
		else if (currIndex == messageKeys.size()-2)
		{
			nextNode = hashedNodes.get(messageKeys.get(currIndex+1));
			nextNode2 = hashedNodes.get(messageKeys.get(0));
		}
		else
		{
			nextNode = hashedNodes.get(messageKeys.get(currIndex+1));
			nextNode2 = hashedNodes.get(messageKeys.get(currIndex+2));
		}

		if(currIndex == 0)
		{
			previousNode1 = hashedNodes.get(messageKeys.get(messageKeys.size()-1));
			previousNode2 = hashedNodes.get(messageKeys.get(messageKeys.size()-2));
		}
		else if(currIndex == 1)
		{
			previousNode1 = hashedNodes.get(messageKeys.get(0));
			previousNode2 = hashedNodes.get(messageKeys.get(messageKeys.size()-1));
		}
		else
		{
			previousNode1 = hashedNodes.get(messageKeys.get(currIndex-1));
			previousNode2 = hashedNodes.get(messageKeys.get(currIndex-2));
		}

		String packet = "recovery" +"//"+ previousNode1 +"//"+ previousNode2 + "//"+ nextNode + "//" + nextNode2;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, packet);

		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		String value = "";
		MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});

//		try
//		{
//			Thread.sleep(300);
//		}catch (Exception e)
//		{
//
//		}
		if (selection.equals("@")) {
//			System.out.println("Multiple nodes @");

			File dirName = getContext().getFilesDir();
			File files[] = dirName.listFiles();
			for (File key : files) {
				try {
					BufferedReader br = new BufferedReader(new FileReader(key));
					String packet = br.readLine();
					String splitPacket[] = packet.split("@@");
					cursor.addRow(new String[]{key.getName(), splitPacket[0]});
				} catch (FileNotFoundException e) {
					System.out.println("File does not exist");
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return cursor;
		} else if (selection.equals("*")) {
//			System.out.println("Multiple nodes *");
			try {
				for (String node : hashedNodes.values()) {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(node));
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeUTF("ReturnAll" + "//" + nextNode + "//" + myPort);
					String packet = "";
					try {
						DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
						packet = input.readUTF();
					} catch (Exception e) {

					}
					if(!packet.equals("")) {
						String keyvalue[] = packet.split("_");
						for (String parts : keyvalue) {
							String part[] = parts.split("##");
							String splitPacket[] = part[1].split("@@");
							cursor.addRow(new String[]{part[0], splitPacket[0]});
						}
					}
				}
				return cursor;
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
//			System.out.println("Multiple avds, single key");
			MatrixCursor cursor1 = new MatrixCursor(new String[]{"key", "value"});
//			this.findKeyPosition(selection);
//			TreeMap<String,Integer> voting = new TreeMap<String, Integer>();
//			ArrayList <Pair <String,String> > keyValues = new ArrayList <Pair <String,String> > ();

			String hashedKey = "";

			try {
				hashedKey = genHash(selection);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			messageKeys.add(hashedKey);
			Collections.sort(messageKeys);
			int keyIndex = messageKeys.indexOf(hashedKey);

			if(keyIndex == 3) {
				node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
				node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
				node3 = hashedNodes.get(messageKeys.get(0));
			}
			else if(keyIndex == 4) {
				node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
				node2 = hashedNodes.get(messageKeys.get(0));
				node3 = hashedNodes.get(messageKeys.get(1));
			}
			else if(keyIndex == 5) {
				node1 = hashedNodes.get(messageKeys.get(0));
				node2 = hashedNodes.get(messageKeys.get(1));
				node3 = hashedNodes.get(messageKeys.get(2));
			}
			else {
				node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
				node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
				node3 = hashedNodes.get(messageKeys.get(keyIndex+3));
			}
			messageKeys.remove(keyIndex);

			ArrayList<String> keyNodes = new ArrayList<String>();
			keyNodes.add(node1);
			keyNodes.add(node2);
			keyNodes.add(node3);

			for (String node : keyNodes) {
				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(node));

					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeUTF("ReturnOne" + "//" + node1 + "//" + selection);

					String packet = "";
					try {
						DataInputStream input = new DataInputStream(socket.getInputStream());
						packet = input.readUTF();
					} catch (Exception w) {
//						System.out.println("Exception caught in query");
					}
					if(!packet.equals("")) {
						String keyvalue[] = packet.split("_");
						String splitPacket[] = keyvalue[1].split("@@");
//						keyValues.add(new Pair<String, String>(keyvalue[0], splitPacket[0]));
						cursor1.addRow(new String[]{keyvalue[0], splitPacket[0]});
//						System.out.println("In voting, key is: "+keyvalue[0]+" & value is: "+splitPacket[0]);
//						if(!voting.keySet().contains(splitPacket[0]))
//							voting.put(splitPacket[0],1);
//						else {
//							voting.put(splitPacket[0], (voting.get(splitPacket[0]) + 1));
//						}
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

//			int max = Collections.max(voting.values());
//			String finalValue = "";
//			for(TreeMap.Entry<String,Integer> v : voting.entrySet())
//			{
//				if(v.getValue() == (max))
//				{
//					finalValue = v.getKey();
//				}
//			}

//			if(!finalValue.equals("")) {
//				for (Pair<String,String> kv : keyValues) {
//					if (kv.second.equals(finalValue)) {
//						cursor1.addRow(new String[]{kv.first, kv.second});
////						System.out.println("Chosen value for key: " + kv.first + " after voting is: " + kv.second+", max is: "+max);
//					}
//				}
//			}
//			cursor1.addRow(new String[]{selection, finalValue});
			return cursor1;
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected synchronized Void doInBackground(ServerSocket... serverSockets) {
			ServerSocket serverSocket = serverSockets[0];
			try {
				while (true) {
					Socket socket = serverSocket.accept();

					DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					String packet = input.readUTF();

					String splitPacket[] = packet.split("//");

					if (splitPacket[0].equals("insertmsg")) {
						FileOutputStream outputStream;
						try {

							File file = new File(getContext().getFilesDir(),splitPacket[2]);
							if(file.exists()) {
								System.out.println("Deleting file in insert");
								file.delete();
							}
							outputStream = getContext().openFileOutput(splitPacket[2], Context.MODE_PRIVATE);
							String valueToInsert = splitPacket[3] + "@@" + splitPacket[1];
							outputStream.write(valueToInsert.getBytes());
//							System.out.println("Written file in port: " + splitPacket[1] + ", key: " + splitPacket[2] + " & value: " + splitPacket[3]);
							outputStream.close();
						} catch (Exception e) {
							Log.e(TAG, "File write failed");
							e.printStackTrace();
						}
					} else if (splitPacket[0].equals("recovermsgs")) {
						String finalMessages = "";
						File dirName = getContext().getFilesDir();
						File files[] = dirName.listFiles();
						for (File key : files) {
							BufferedReader br = new BufferedReader(new FileReader(key));
							String value = br.readLine();
//							System.out.println("Recovery value is: " + value);
							String splitValue[] = value.split("@@");
							if (splitValue[1].equals(splitPacket[1])) {
								String keyvalue = key.getName() + "##" + value;
								finalMessages += keyvalue + "_";
							}
						}
						DataOutputStream finalOut = new DataOutputStream(socket.getOutputStream());
						finalOut.writeUTF(finalMessages);
					} else if (splitPacket.length == 3) {
						String finalMessages = "";

						if (splitPacket[0].equals("ReturnAll")) {

							File dirName = getContext().getFilesDir();
							File files[] = dirName.listFiles();
							for (File key : files) {
								BufferedReader br = new BufferedReader(new FileReader(key));
								String value = br.readLine();
								String keyvalue = key.getName() + "##" + value;
								finalMessages += keyvalue + "_";
							}

							DataOutputStream finalOut = new DataOutputStream(socket.getOutputStream());
							finalOut.writeUTF(finalMessages);
						} else if (splitPacket[0].equals("ReturnOne")) {
//							System.out.println("Inside return one case in server");
							File dirName = getContext().getFilesDir();
							File files[] = dirName.listFiles();
							for (File key : files) {
								if (key.getName().equals(splitPacket[2])) {
									try {
										BufferedReader br = new BufferedReader(new FileReader(key));
										String value = br.readLine();
										if(value.contains(splitPacket[1])) {
											DataOutputStream outOne = new DataOutputStream(socket.getOutputStream());
											outOne.writeUTF(splitPacket[2] + "_" + value);
										}
									} catch (IOException e) {
										e.printStackTrace();
									} catch (RuntimeException e) {
										e.printStackTrace();
									}
								}
							}
						} else if (splitPacket[0].equals("DeleteAll")) {
							File dirName = getContext().getFilesDir();
							File files[] = dirName.listFiles();
							for (File key : files) {
								key.delete();
							}
						} else if (splitPacket[0].equals("DeleteOne")) {
							File dirName = getContext().getFilesDir();
							File files[] = dirName.listFiles();
							for (File key : files) {
								if (key.getName().equals(splitPacket[1])) {
									key.delete();
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	private class ClientTask extends AsyncTask<String, String, String> {

		protected String doInBackground(String... msgs) {
//			System.out.println("Inside Client Task");
			String packet[] = msgs[0].split("//");


			if (packet[0].equals("insertmsg")) {
				String emulatorNo = packet[1];
				String next = packet[2];
				String nextToNext = packet[3];
				String key = packet[4];
				String value = packet[5];

				ArrayList<String> nodesToInsert = new ArrayList<String>();
				nodesToInsert.add(emulatorNo);
				nodesToInsert.add(next);
				nodesToInsert.add(nextToNext);

				try {
					for (String node : nodesToInsert) {
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
								Integer.parseInt(node));

						String packageToSend = "insertmsg" + "//" + emulatorNo + "//" + key + "//" + value;
						DataOutputStream output = new DataOutputStream(socket.getOutputStream());
						output.writeUTF(packageToSend);
						output.flush();
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (packet[0].equals("recovery")) {
				ArrayList<String> recoveryNodes = new ArrayList<String>();
				recoveryNodes.add(packet[1]);
				recoveryNodes.add(packet[2]);
				recoveryNodes.add(packet[3]);

				for (String node : recoveryNodes) {

					try {
//						System.out.println("For node: " + myPort + ", recovery node is: " + node);
						String nodeToSend = "";
						if (node.equals(nextNode))// || node.equals(nextNode2))
							nodeToSend = myPort;
						else
							nodeToSend = node;

						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
								Integer.parseInt(node));
						String packageToSend = "recovermsgs" + "//" + nodeToSend;
						DataOutputStream output = new DataOutputStream(socket.getOutputStream());
						output.writeUTF(packageToSend);
						output.flush();

						String packet1 = "";

						try {
							DataInputStream input = new DataInputStream(socket.getInputStream());
							packet1 = input.readUTF();


							if (!packet1.equals("")) {
								String keyvalue[] = packet1.split("_");
								for (String parts : keyvalue) {
									String part[] = parts.split("##");
									String key = part[0];
									String value = part[1];

									FileOutputStream outputStream;
									try {
										outputStream = getContext().openFileOutput(key, Context.MODE_PRIVATE);
										outputStream.write(value.getBytes());
//									System.out.println("Written file in port: " + myPort + ", key: " + key + " & value: " + value);
										outputStream.close();
									} catch (Exception e) {
										Log.e(TAG, "File write failed");
										e.printStackTrace();
									}
								}

							}
						} catch (Exception e) {

						}

					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
