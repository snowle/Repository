package com.demo.recordvoice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import ex.jtrans;

@SuppressLint({ "SimpleDateFormat", "HandlerLeak" })
public class MainActivity extends Activity {

	public static final String TAG = "MainActivity";
	// LinearLayout linearLayout;
	TextView textView, textView2;
	Button bt_record, bt_stop, bt_play, bt_delete, bt_speak;
	// ListView listView;
	PopupWindow popup;
	TextView popupText;

	Handler handler;
	Message msg, msg2;
	private AudioRecord audioRecord;
	jtrans Jtrans; // ��ɢ�任��
	AudioProcess audioProcess; // ��Ƶ¼�����װ������
	WaveFileReader waveFileReader; // ������ȡWAV�ļ�������
	Calcs calcs; // �źŴ����������

	private int audioSource = MediaRecorder.AudioSource.MIC; // ��Ƶ��ȡԴ
	public static int sampleRateInHz = 8000; // ������Ƶ������
	private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO; // ������Ƶ��¼�Ƶ�����CHANNEL_IN_STEREOΪ˫����
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // ��Ƶ���ݸ�ʽ:PCM 16λÿ������
	public static boolean isRecord = false;// ��������¼�Ƶ�״̬
	private int bufferSizeInBytes = 0; // �������ֽڴ�С

	public static int a = 0; //���͵�popupWindow

	public static int readsize = 0;
	public static String rootPath = Environment.getExternalStorageDirectory()
			.getAbsolutePath(); // ��ȡSD����·��
	private static final String AudioName = rootPath + "/¼������/voice.pcm"; // AudioName����Ƶ�����ļ�
	private static final String NewAudioName = rootPath + "/¼������/voice.wav"; // NewAudioName�ɲ��ŵ���Ƶ�ļ�

	public final double KAI_YUZHI = 22; // ʶ�𡰿�����ֵ
	public final double GUAN_YUZHI = 22; // ʶ�𡰹ء���ֵ

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initView();
		Jtrans = new jtrans();
		calcs = new Calcs();
		audioProcess = new AudioProcess();

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case 0x12345:
					Log.v(TAG, "��ʼʶ��");
					textView2.setText("\n\t" + (String) msg.obj);
					//getResult(); //��ȡʶ����
					break;

				case 0x11111:					
					textView2.setText("\n\t" + (String) msg.obj);
					break;
					
				default:
					break;
				}
			}
		};

	}

	/** ��ʼ���ؼ� */
	private void initView() {

		bt_record = (Button) findViewById(R.id.bt_record);
		bt_stop = (Button) findViewById(R.id.bt_stop);
		bt_play = (Button) findViewById(R.id.bt_play);
		bt_delete = (Button) findViewById(R.id.bt_delete);
		bt_speak = (Button) findViewById(R.id.speak); // ����һ����Ť������¼�����ɿ�ֹͣ
		textView = (TextView) findViewById(R.id.textview);
		textView2 = (TextView) findViewById(R.id.textView2);

		MyClickListener myClickListener = new MyClickListener();
		bt_record.setOnClickListener(myClickListener);
		bt_stop.setOnClickListener(myClickListener);
		bt_play.setOnClickListener(myClickListener);
		bt_delete.setOnClickListener(myClickListener);
		bt_speak.setOnClickListener(myClickListener);

		setPopupParams();
		setTimerTask();
		bt_speak.setOnTouchListener(new OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					bt_speak.setBackgroundColor(0xff6495ed);
					bt_speak.setText("�ɿ�ֹͣ¼��");

					textView.setText("¼����...");
					textView2.setText("\n");
					bt_record.setEnabled(false);
					recordVoice();
					
					popup.showAtLocation(bt_speak, Gravity.CENTER, 0, 0);

					break;

				case MotionEvent.ACTION_UP:
					bt_speak.setBackgroundColor(0xffd3d3d3);
					bt_speak.setText("��ס��ʼ˵��");

					textView.setText("");
					textView2.append("¼������");
					bt_record.setEnabled(true);
					stopRecord();
					
					popup.dismiss();

					break;

				default:
					break;
				}

				return true;
			}
		});
	}

	/**
	 * ��ť�����¼���
	 */
	private class MyClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.bt_record:
				textView.setText("¼����...");
				bt_record.setEnabled(false);
				audioProcess.recordVoice(NewAudioName);
				break;

			case R.id.bt_stop:
				textView.setText("");
				bt_record.setEnabled(true);
				audioProcess.stopRecord();
				break;

			case R.id.bt_play:
				if (isRecord == true) {
					Toast.makeText(getApplicationContext(), "�ļ�����д��!",
							Toast.LENGTH_SHORT).show();
					return;
				}
				textView.setText("������...");
				playAudio();
				break;

			case R.id.bt_delete:
				textView.setText("");
				delete();
				break;

			default:
				break;
			}
		}

	}
	
	/**����popupwindow��ز���*/
	@SuppressLint("InflateParams") @SuppressWarnings("deprecation")
	private void setPopupParams() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int disPlayWidth =dm.widthPixels; 
		int displayHeight = dm.heightPixels;
		View popupView = getLayoutInflater().inflate(R.layout.popup, null);
		popup = new PopupWindow(popupView, disPlayWidth*4/5, displayHeight*1/3);
		popup.setBackgroundDrawable(new BitmapDrawable());
		popup.setFocusable(true);
		popup.setOutsideTouchable(true);
		popupText = (TextView) popupView.findViewById(R.id.popText);
	}
	

	/**
	 * ¼��
	 */
	private void recordVoice() {
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat); // ��û������ֽڴ�С
		Log.v(TAG, "�����ʣ�" + sampleRateInHz + "\n�������ֽڴ�С:" + bufferSizeInBytes);
		audioRecord = new AudioRecord(audioSource, sampleRateInHz,
				channelConfig, audioFormat, bufferSizeInBytes); // ����AudioRecord����
		audioRecord.startRecording();
		isRecord = true; // ����¼��״̬Ϊtrue
		// new Thread(new RecordRunnable()).start();// ����¼���߳�
		new Thread(new WriteToFileRunnable()).start();// �����ļ�д���߳�
	}

	/**
	 * ֹͣ¼��
	 */
	private void stopRecord() {
		if (audioRecord != null) {
			isRecord = false; // ֹͣ�ļ�д��
			audioRecord.stop();
			audioRecord.release();// �ͷ���Դ
			audioRecord = null;
		}
		// bt_play.setEnabled(true);
	}

	/**
	 * ������Ƶ
	 */
	private void playAudio() {
		File file = new File(NewAudioName);
		if (!file.exists()) {
			Toast.makeText(this, "��Ƶ�ļ�������", Toast.LENGTH_SHORT).show();
			return;
		}

		bt_play.setEnabled(false);

		MediaPlayer player = new MediaPlayer();
		try {
			player.setDataSource(NewAudioName);
			player.prepare();
			player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer player) {
				textView.setText("");
				bt_play.setEnabled(true);
				player.stop();
				player.release();
				Toast.makeText(getApplicationContext(), "���Ž���",
						Toast.LENGTH_SHORT).show();
			}
		});

	}

	/**
	 * ɾ���ļ�
	 */
	private void delete() {
		File file = new File(NewAudioName);
		if (file.exists()) {
			if (file.isFile()) {
				file.delete();
			}
		} else {
			Toast.makeText(getApplicationContext(), "�ļ�������", Toast.LENGTH_SHORT).show();
		}
	}
	

	/**
	 * ����һ��¼����д���ļ��࣬ʵ��Runnable�ӿ�
	 */
	class WriteToFileRunnable implements Runnable {

		@Override
		public void run() {

			byte[] audiodata = new byte[bufferSizeInBytes]; // newһ��byte����������һЩ�ֽ����ݣ���СΪ��������С
			// Log.v(TAG, "�������ֽڴ�СΪ��"+bufferSizeInBytes);
			FileOutputStream fos = null;
			int readsize = 0;
			try {
				File folder = new File(rootPath + "/¼������");
				if (!folder.exists()) {
					folder.mkdirs();
					Log.v(TAG, "�����ļ���" + folder.getPath());
				}
				File file = new File(AudioName);
				if (file.exists()) {
					file.delete();
				}
				fos = new FileOutputStream(file);// ����һ���ɴ�ȡ�ֽڵ��ļ�
			} catch (Exception e) {
				e.printStackTrace();
			}

			while (isRecord == true) {
				readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
				// Log.v(TAG, "�����ֽڳ��ȣ�"+readsize);
				if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
					try {
						fos.write(audiodata);
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
			}
			try {
				fos.close();// �ر�д����
			} catch (IOException e) {
				e.printStackTrace();
			}

			copyWaveFile(AudioName, NewAudioName);
			
			msg = new Message();
			msg.what = 0x12345;
			msg.obj = "ʶ����...";
			handler.sendMessage(msg);

			// ƥ��
			double[][] tes = getVoiceParams(NewAudioName);
			if (tes == null) {
				Log.e(TAG, "δ��⵽��Ч����");
				
				msg = new Message();
				msg.what = 0x11111;
				msg.obj = "δ��⵽��Ч����";
				handler.sendMessage(msg);
				
				return;
			}

			String mouldsPath = rootPath + "/¼������/moulds";  //ģ��Ŀ¼·��
			File modFolder = new File(mouldsPath);
			Log.v(TAG, "ģ��Ŀ¼��" + modFolder.getPath());
			String[] strings = modFolder.list();
			for (int i = 0; i < strings.length; i++) {
				Log.v(TAG, "Mould: "+strings[i]);
			}	
			
			String[] resultStrings = getBestModDis(NewAudioName, rootPath + "/¼������/moulds");
			if (resultStrings != null) {
				
				for (int i = 0; i < resultStrings.length; i++) {
					Log.v(TAG, resultStrings[i]);
				}
				
				//���ƥ�����������жϼ���ʾ
				String recResult = resultStrings[0]; // ʶ����
				double dis = Double.parseDouble(resultStrings[1]);  // ģ�����
				double disR = Double.parseDouble(resultStrings[2]);  // ģ�����/֡����
				
				Log.v(TAG, ":  " + recResult + "\n:  " + dis + "\n:  "+disR);				
				
				if (disR < KAI_YUZHI) {
					Log.d(TAG, "*** ʶ��Ϊ��" + recResult + " ***");
					if (fileCount(mouldsPath+"/"+recResult) <3 )  
					moveFileandRename(NewAudioName, mouldsPath + "/" + recResult + "/" + getCurrentSysTime() + ".wav");
				}else {
					recResult = "ʶ��ʧ��";
					Log.d(TAG, "---- δ�ҵ�ƥ����  ----");
				}

				msg = new Message();
				msg.what = 0x11111;
				msg.obj = recResult;
				handler.sendMessage(msg);
			
			}
		}
	}
	
	public void getResult() {
		double[][] tes = getVoiceParams(NewAudioName);
		if (tes == null) {
			Log.e(TAG, "δ��⵽��Ч����");
			textView.setText("δ��⵽��Ч����");			
			return;
		}

		String mouldsPath = rootPath + "/¼������/moulds";  //ģ��Ŀ¼·��
		File modFolder = new File(mouldsPath);
		Log.v(TAG, "ģ��Ŀ¼��" + modFolder.getPath());
		String[] strings = modFolder.list();
		for (int i = 0; i < strings.length; i++) {
			Log.v(TAG, "Mould: "+strings[i]);
		}	
		
		String[] resultStrings = getBestModDis(NewAudioName, rootPath + "/¼������/moulds");
		if (resultStrings != null) {
			
			for (int i = 0; i < resultStrings.length; i++) {
				Log.v(TAG, resultStrings[i]);
			}
			
			//���ƥ�����������жϼ���ʾ
			String recResult = resultStrings[0]; // ʶ����
			double dis = Double.parseDouble(resultStrings[1]);  // ģ�����
			double disR = Double.parseDouble(resultStrings[2]);  // ģ�����/֡����
			
			Log.v(TAG, ":  " + recResult + "\n:  " + dis + "\n:  "+disR);				
			
			if (disR < KAI_YUZHI) {
				Log.d(TAG, "*** ʶ��Ϊ��" + recResult + " ***");
				if (fileCount(mouldsPath+"/"+recResult) <3 )  
				moveFileandRename(NewAudioName, mouldsPath + "/" + recResult + "/" + getCurrentSysTime() + ".wav");
			}else {
				recResult = "ʶ��ʧ��";
				Log.d(TAG, "---- δ�ҵ�ƥ����  ----");
			}

			textView.setText(recResult);
		
		}
	}
	

	/**
	 * �õ��ɲ��ŵ���Ƶ�ļ�
	 * 
	 * @param inFilename
	 * @param outFilename
	 */
	private void copyWaveFile(String inFilename, String outFilename) {

		FileInputStream in = null;
		FileOutputStream out = null;

		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = sampleRateInHz;
		int channels = 2;
		long byteRate = 16 * sampleRateInHz * channels / 8;
		byte[] data = new byte[bufferSizeInBytes];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);
			while (in.read(data) != -1) {
				out.write(data);
			}
			in.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * �ṩһ��ͷ��Ϣ��������Щ��Ϣ�Ϳ��Եõ����Բ��ŵ��ļ���
	 * 
	 * @param out
	 * @param totalAudioLen
	 * @param totalDataLen
	 * @param longSampleRate
	 * @param channels
	 * @param byteRate
	 * @throws IOException
	 */
	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = 16; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}

	/**
	 * ��ȡ������������
	 * 
	 * @param voicename
	 * @return
	 */
	private double[][] getVoiceParams(String voicename) {

		double[] wavedata; // ����Ԥ���صĲ�������
		double[][] frames; // ֡����
		double[][] voice_frames; // ��Ч֡����
		waveFileReader = new WaveFileReader(voicename);
		Log.v(TAG, getCurrentSysTime());
		if (waveFileReader.isSuccess()) {
			int[] data = waveFileReader.getData()[0]; // ��ȡ��һ����
			// waveParam = data;
			if (data.length >= 1200) {
				// wavedata = new float[data.length - 1];
				wavedata = calcs.PreEmphasis(calcs.ToOne(data)); // ����һ���Ĳ�������Ԥ����
				Log.v(TAG, "Ԥ���ش�����ɣ����鳤��Ϊ��" + wavedata.length);

				// ��֡
				frames = calcs.divideToFrame(wavedata);
				Log.v(TAG, "��֡������ɣ�֡��Ϊ��" + frames.length);

				// �˵��⣬�õ���Ч֡
				voice_frames = calcs.getEffectFrames(frames);
				Log.v(TAG, "�˵������ " + "��Ч֡��Ϊ��" + voice_frames.length);

				if (voice_frames.length >= 15) {

					// ��ÿһ֡��ȡMFCC����
					double[][] mfccs = new double[voice_frames.length][12];
					for (int i = 0; i < voice_frames.length; i++) {
						mfccs[i] = calcs.LogAndDCT(calcs.DFT(voice_frames[i]));
					}
					Log.v(TAG, "��ȡMFCC������� ");

					// ����MFCC����
					double[][] dt_mfccs = new double[voice_frames.length][12];
					for (int i = 2; i < mfccs.length - 2; i++) {
						for (int j = 0; j < 12; j++) {
							dt_mfccs[i][j] = (2 * mfccs[i + 2][j]
									+ mfccs[i + 1][j] - mfccs[i - 1][j] - 2 * mfccs[i - 2][j]) / 3;
						}
					}
					Log.v(TAG, "��ȡ���MFCC������� ");

					// ��ȡ��MFCC��һ�ײ��MFCC��ɵ���������
					double[][] mixParams = new double[voice_frames.length - 4][24]; // ������������
					for (int i = 0; i < mixParams.length; i++) {
						System.arraycopy(mfccs[i + 2], 0, mixParams[i], 0, 12);
						System.arraycopy(dt_mfccs[i + 2], 0, mixParams[i], 12,
								12);
					}
					Log.v(TAG, "����������ȡ��� ");

					return mixParams;
				} else {
					Log.e(TAG, "����̫��,������˵��");
					return null;
				}

			} else {
				Log.e(TAG, "δ��⵽����");
				return null;
			}

		} else {
			Log.e(TAG, "��Ƶ���ݶ�ȡʧ��");
			return null;
		}

	}

	/**
	 * ������������ģ��Ŀ¼�������ģ�����ƥ�䣬�������ƥ����
	 * 
	 * @param tesPath
	 *            ��������·��
	 * @param resFoldPath
	 *            ģ���ļ���·��  
	 * @return ����3��Ԫ�ص��ַ�������{����ʶ��������С���룬 ���ڶ����жϵľ���}
	 */
	public String[] getBestModDis(String tesPath, String resFoldPath) {
		String[] outString = new String[3];
		double[] tmp = new double[2];
		double[] result = new double[] { Double.MAX_VALUE, 0 }; // resule[0]Ϊ��Сģ����룬result[1]Ϊresult[0]/ģ�峤�Ⱥ�
		File modFolder = new File(resFoldPath); // ȫ��ģ���ļ���
		File[] moulds = modFolder.listFiles();
		if (moulds == null) {
			outString = null;  //���ģ���ļ���Ϊ�գ��򷵻�ֵΪnull;
		}else {
			for (int i = 0; i < moulds.length; i++) {
				if (moulds[i].isDirectory() && moulds[i].listFiles().length > 0) {
					tmp = calcs.getMinDis(tesPath, getPaths(moulds[i]));
					if (tmp[0] < result[0]) {
						result = tmp;
						Log.v(TAG, moulds[i].getPath());
						outString[0] = moulds[i].getName();
						outString[1] = String.valueOf(result[0]);
						outString[2] = String.valueOf(result[1]);
					}
				}else {
					outString = null;  //�����ģ��Ŀ¼Ϊ�գ��򷵻�ֵΪnull
					msg = new Message();
					msg.what = 0x12345;
					msg.obj = "ģ�� ��"+moulds[i].getName()+"�� Ŀ¼Ϊ�գ���¼��ģ�壬����ɾ����ģ��Ŀ¼";
					handler.sendMessage(msg);
					break;
				}
			}
		}
		
		return outString;
	}

	/**
	 * ��ȡ�ļ������������ļ�·��
	 * 
	 * @param file
	 * @return
	 */
	public String[] getPaths(File file) {
		if (!file.isDirectory()) {
			return null;
		}
		File[] files = file.listFiles();
		int count = files.length;
		String[] paths = new String[count];
		for (int i = 0; i < count; i++) {
			paths[i] = files[i].getPath();
		}

		return paths;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.actionbar_menu, menu);
		return true;
	}

	/** �������˵�����¼� */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.options:
			Intent intent = new Intent(MainActivity.this,AddModuleActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}

		return true;
	}

	/**
	 * ��ʱ��
	 */
	 private void setTimerTask() {
		 new Timer().schedule(new TimerTask() {	
			@Override
			public void run() {
				if (a<10000000) {
					a++;
				}else {
					a=0;
				}	
			}
		 }, 0, 10 );
	 }

	/**
	 * ��ȡ��ǰϵͳʱ��
	 * 
	 * @return
	 */
	public String getCurrentSysTime() {
		String str;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());// ��ȡ��ǰʱ��
		str = formatter.format(curDate);
		return str;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/** ������ת�����ַ��� */
	public String arrayToString(short[] b) {
		String tmpString = "";
		for (int i = 0; i < b.length; i++) {
			tmpString = tmpString + " " + b[i];
		}
		return tmpString;
	}

	/** д������ */
	public void WriteData(String key, int value) {
		SharedPreferences preferences = getSharedPreferences("amp_data",
				Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	/** ��ȡ���� */
	public int ReadData(String key) {
		SharedPreferences preferences = getSharedPreferences("amp_data",
				Context.MODE_PRIVATE);
		int value = preferences.getInt(key, 0);
		return value;
	}

	/**
	 * ��ȡ�ļ�Ŀ¼�µ��ļ���Ŀ
	 * 
	 * @param path
	 *            //Ŀ¼�ľ���·��
	 * @return Ŀ¼�µ��ļ���Ŀ
	 */
	public int fileCount(String path) {
		int count = 0;
		File file = new File(path);
		if (file.isDirectory()) {
			count = file.list().length;
		}
		return count;
	}

	/**
	 * �ƶ��ļ�
	 * 
	 * @param srcFileName Դ�ļ�����·��
	 * @param destDirName Ŀ���ļ�����·��
	 * @return �ļ��ƶ��ɹ�����true�����򷵻�false
	 */
	public boolean moveFileandRename(String srcFileName, String destFileName) {

		File srcFile = new File(srcFileName);
		File dstFile = new File(destFileName);
		if (!srcFile.exists() || !srcFile.isFile())
			return false;

		File destDir = new File(dstFile.getParent());
		if (!destDir.exists())
			destDir.mkdirs();

		return srcFile.renameTo(dstFile);
	}
}
