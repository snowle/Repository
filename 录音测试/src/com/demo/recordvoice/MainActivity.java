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
	jtrans Jtrans; // 离散变换库
	AudioProcess audioProcess; // 音频录制与封装处理类
	WaveFileReader waveFileReader; // 声明读取WAV文件数据类
	Calcs calcs; // 信号处理与计算类

	private int audioSource = MediaRecorder.AudioSource.MIC; // 音频获取源
	public static int sampleRateInHz = 8000; // 设置音频采样率
	private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO; // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // 音频数据格式:PCM 16位每个样本
	public static boolean isRecord = false;// 设置正在录制的状态
	private int bufferSizeInBytes = 0; // 缓冲区字节大小

	public static int a = 0; //发送到popupWindow

	public static int readsize = 0;
	public static String rootPath = Environment.getExternalStorageDirectory()
			.getAbsolutePath(); // 获取SD卡根路径
	private static final String AudioName = rootPath + "/录音测试/voice.pcm"; // AudioName裸音频数据文件
	private static final String NewAudioName = rootPath + "/录音测试/voice.wav"; // NewAudioName可播放的音频文件

	public final double KAI_YUZHI = 22; // 识别“开”阈值
	public final double GUAN_YUZHI = 22; // 识别“关”阈值

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
					Log.v(TAG, "开始识别");
					textView2.setText("\n\t" + (String) msg.obj);
					//getResult(); //获取识别结果
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

	/** 初始化控件 */
	private void initView() {

		bt_record = (Button) findViewById(R.id.bt_record);
		bt_stop = (Button) findViewById(R.id.bt_stop);
		bt_play = (Button) findViewById(R.id.bt_play);
		bt_delete = (Button) findViewById(R.id.bt_delete);
		bt_speak = (Button) findViewById(R.id.speak); // 加入一个按扭，按下录音，松开停止
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
					bt_speak.setText("松开停止录音");

					textView.setText("录音中...");
					textView2.setText("\n");
					bt_record.setEnabled(false);
					recordVoice();
					
					popup.showAtLocation(bt_speak, Gravity.CENTER, 0, 0);

					break;

				case MotionEvent.ACTION_UP:
					bt_speak.setBackgroundColor(0xffd3d3d3);
					bt_speak.setText("按住开始说话");

					textView.setText("");
					textView2.append("录音结束");
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
	 * 按钮监听事件类
	 */
	private class MyClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.bt_record:
				textView.setText("录音中...");
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
					Toast.makeText(getApplicationContext(), "文件正在写入!",
							Toast.LENGTH_SHORT).show();
					return;
				}
				textView.setText("播放中...");
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
	
	/**设置popupwindow相关参数*/
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
	 * 录音
	 */
	private void recordVoice() {
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat); // 获得缓冲区字节大小
		Log.v(TAG, "采样率：" + sampleRateInHz + "\n缓冲区字节大小:" + bufferSizeInBytes);
		audioRecord = new AudioRecord(audioSource, sampleRateInHz,
				channelConfig, audioFormat, bufferSizeInBytes); // 创建AudioRecord对象
		audioRecord.startRecording();
		isRecord = true; // 设置录制状态为true
		// new Thread(new RecordRunnable()).start();// 开启录音线程
		new Thread(new WriteToFileRunnable()).start();// 开启文件写入线程
	}

	/**
	 * 停止录音
	 */
	private void stopRecord() {
		if (audioRecord != null) {
			isRecord = false; // 停止文件写入
			audioRecord.stop();
			audioRecord.release();// 释放资源
			audioRecord = null;
		}
		// bt_play.setEnabled(true);
	}

	/**
	 * 播放音频
	 */
	private void playAudio() {
		File file = new File(NewAudioName);
		if (!file.exists()) {
			Toast.makeText(this, "音频文件不存在", Toast.LENGTH_SHORT).show();
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
				Toast.makeText(getApplicationContext(), "播放结束",
						Toast.LENGTH_SHORT).show();
			}
		});

	}

	/**
	 * 删除文件
	 */
	private void delete() {
		File file = new File(NewAudioName);
		if (file.exists()) {
			if (file.isFile()) {
				file.delete();
			}
		} else {
			Toast.makeText(getApplicationContext(), "文件不存在", Toast.LENGTH_SHORT).show();
		}
	}
	

	/**
	 * 创建一个录音并写入文件类，实现Runnable接口
	 */
	class WriteToFileRunnable implements Runnable {

		@Override
		public void run() {

			byte[] audiodata = new byte[bufferSizeInBytes]; // new一个byte数组用来存一些字节数据，大小为缓冲区大小
			// Log.v(TAG, "缓冲区字节大小为："+bufferSizeInBytes);
			FileOutputStream fos = null;
			int readsize = 0;
			try {
				File folder = new File(rootPath + "/录音测试");
				if (!folder.exists()) {
					folder.mkdirs();
					Log.v(TAG, "创建文件夹" + folder.getPath());
				}
				File file = new File(AudioName);
				if (file.exists()) {
					file.delete();
				}
				fos = new FileOutputStream(file);// 建立一个可存取字节的文件
			} catch (Exception e) {
				e.printStackTrace();
			}

			while (isRecord == true) {
				readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
				// Log.v(TAG, "缓冲字节长度："+readsize);
				if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
					try {
						fos.write(audiodata);
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
			}
			try {
				fos.close();// 关闭写入流
			} catch (IOException e) {
				e.printStackTrace();
			}

			copyWaveFile(AudioName, NewAudioName);
			
			msg = new Message();
			msg.what = 0x12345;
			msg.obj = "识别中...";
			handler.sendMessage(msg);

			// 匹配
			double[][] tes = getVoiceParams(NewAudioName);
			if (tes == null) {
				Log.e(TAG, "未检测到有效语音");
				
				msg = new Message();
				msg.what = 0x11111;
				msg.obj = "未检测到有效语音";
				handler.sendMessage(msg);
				
				return;
			}

			String mouldsPath = rootPath + "/录音测试/moulds";  //模板目录路径
			File modFolder = new File(mouldsPath);
			Log.v(TAG, "模板目录：" + modFolder.getPath());
			String[] strings = modFolder.list();
			for (int i = 0; i < strings.length; i++) {
				Log.v(TAG, "Mould: "+strings[i]);
			}	
			
			String[] resultStrings = getBestModDis(NewAudioName, rootPath + "/录音测试/moulds");
			if (resultStrings != null) {
				
				for (int i = 0; i < resultStrings.length; i++) {
					Log.v(TAG, resultStrings[i]);
				}
				
				//获得匹配结果并进行判断及显示
				String recResult = resultStrings[0]; // 识别结果
				double dis = Double.parseDouble(resultStrings[1]);  // 模板距离
				double disR = Double.parseDouble(resultStrings[2]);  // 模板距离/帧数和
				
				Log.v(TAG, ":  " + recResult + "\n:  " + dis + "\n:  "+disR);				
				
				if (disR < KAI_YUZHI) {
					Log.d(TAG, "*** 识别为：" + recResult + " ***");
					if (fileCount(mouldsPath+"/"+recResult) <3 )  
					moveFileandRename(NewAudioName, mouldsPath + "/" + recResult + "/" + getCurrentSysTime() + ".wav");
				}else {
					recResult = "识别失败";
					Log.d(TAG, "---- 未找到匹配结果  ----");
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
			Log.e(TAG, "未检测到有效语音");
			textView.setText("未检测到有效语音");			
			return;
		}

		String mouldsPath = rootPath + "/录音测试/moulds";  //模板目录路径
		File modFolder = new File(mouldsPath);
		Log.v(TAG, "模板目录：" + modFolder.getPath());
		String[] strings = modFolder.list();
		for (int i = 0; i < strings.length; i++) {
			Log.v(TAG, "Mould: "+strings[i]);
		}	
		
		String[] resultStrings = getBestModDis(NewAudioName, rootPath + "/录音测试/moulds");
		if (resultStrings != null) {
			
			for (int i = 0; i < resultStrings.length; i++) {
				Log.v(TAG, resultStrings[i]);
			}
			
			//获得匹配结果并进行判断及显示
			String recResult = resultStrings[0]; // 识别结果
			double dis = Double.parseDouble(resultStrings[1]);  // 模板距离
			double disR = Double.parseDouble(resultStrings[2]);  // 模板距离/帧数和
			
			Log.v(TAG, ":  " + recResult + "\n:  " + dis + "\n:  "+disR);				
			
			if (disR < KAI_YUZHI) {
				Log.d(TAG, "*** 识别为：" + recResult + " ***");
				if (fileCount(mouldsPath+"/"+recResult) <3 )  
				moveFileandRename(NewAudioName, mouldsPath + "/" + recResult + "/" + getCurrentSysTime() + ".wav");
			}else {
				recResult = "识别失败";
				Log.d(TAG, "---- 未找到匹配结果  ----");
			}

			textView.setText(recResult);
		
		}
	}
	

	/**
	 * 得到可播放的音频文件
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
	 * 提供一个头信息，插入这些信息就可以得到可以播放的文件。
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
	 * 获取语音特征参数
	 * 
	 * @param voicename
	 * @return
	 */
	private double[][] getVoiceParams(String voicename) {

		double[] wavedata; // 经过预加重的波形数组
		double[][] frames; // 帧数组
		double[][] voice_frames; // 有效帧数组
		waveFileReader = new WaveFileReader(voicename);
		Log.v(TAG, getCurrentSysTime());
		if (waveFileReader.isSuccess()) {
			int[] data = waveFileReader.getData()[0]; // 获取第一声道
			// waveParam = data;
			if (data.length >= 1200) {
				// wavedata = new float[data.length - 1];
				wavedata = calcs.PreEmphasis(calcs.ToOne(data)); // 将归一化的波形数据预加重
				Log.v(TAG, "预加重处理完成，数组长度为：" + wavedata.length);

				// 分帧
				frames = calcs.divideToFrame(wavedata);
				Log.v(TAG, "分帧处理完成，帧数为：" + frames.length);

				// 端点检测，得到有效帧
				voice_frames = calcs.getEffectFrames(frames);
				Log.v(TAG, "端点检测完成 " + "有效帧数为：" + voice_frames.length);

				if (voice_frames.length >= 15) {

					// 对每一帧提取MFCC参数
					double[][] mfccs = new double[voice_frames.length][12];
					for (int i = 0; i < voice_frames.length; i++) {
						mfccs[i] = calcs.LogAndDCT(calcs.DFT(voice_frames[i]));
					}
					Log.v(TAG, "提取MFCC参数完成 ");

					// 求差分MFCC参数
					double[][] dt_mfccs = new double[voice_frames.length][12];
					for (int i = 2; i < mfccs.length - 2; i++) {
						for (int j = 0; j < 12; j++) {
							dt_mfccs[i][j] = (2 * mfccs[i + 2][j]
									+ mfccs[i + 1][j] - mfccs[i - 1][j] - 2 * mfccs[i - 2][j]) / 3;
						}
					}
					Log.v(TAG, "提取差分MFCC参数完成 ");

					// 获取由MFCC和一阶差分MFCC组成的特征参数
					double[][] mixParams = new double[voice_frames.length - 4][24]; // 特征参数数组
					for (int i = 0; i < mixParams.length; i++) {
						System.arraycopy(mfccs[i + 2], 0, mixParams[i], 0, 12);
						System.arraycopy(dt_mfccs[i + 2], 0, mixParams[i], 12,
								12);
					}
					Log.v(TAG, "特征参数提取完成 ");

					return mixParams;
				} else {
					Log.e(TAG, "语音太短,请重新说话");
					return null;
				}

			} else {
				Log.e(TAG, "未检测到语音");
				return null;
			}

		} else {
			Log.e(TAG, "音频数据读取失败");
			return null;
		}

	}

	/**
	 * 将测试语音与模板目录里的所有模板进行匹配，获得最优匹配结果
	 * 
	 * @param tesPath
	 *            测试语音路径
	 * @param resFoldPath
	 *            模板文件夹路径  
	 * @return 包含3个元素的字符串数组{初步识别结果，最小距离， 用于二次判断的距离}
	 */
	public String[] getBestModDis(String tesPath, String resFoldPath) {
		String[] outString = new String[3];
		double[] tmp = new double[2];
		double[] result = new double[] { Double.MAX_VALUE, 0 }; // resule[0]为最小模板距离，result[1]为result[0]/模板长度和
		File modFolder = new File(resFoldPath); // 全部模板文件夹
		File[] moulds = modFolder.listFiles();
		if (moulds == null) {
			outString = null;  //如果模板文件夹为空，则返回值为null;
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
					outString = null;  //如果该模板目录为空，则返回值为null
					msg = new Message();
					msg.what = 0x12345;
					msg.obj = "模板 “"+moulds[i].getName()+"” 目录为空，请录入模板，或者删除该模板目录";
					handler.sendMessage(msg);
					break;
				}
			}
		}
		
		return outString;
	}

	/**
	 * 获取文件夹下面所有文件路径
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

	/** 导航栏菜单点击事件 */
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
	 * 定时器
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
	 * 获取当前系统时间
	 * 
	 * @return
	 */
	public String getCurrentSysTime() {
		String str;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		str = formatter.format(curDate);
		return str;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/** 将数组转换成字符串 */
	public String arrayToString(short[] b) {
		String tmpString = "";
		for (int i = 0; i < b.length; i++) {
			tmpString = tmpString + " " + b[i];
		}
		return tmpString;
	}

	/** 写入数据 */
	public void WriteData(String key, int value) {
		SharedPreferences preferences = getSharedPreferences("amp_data",
				Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	/** 读取数据 */
	public int ReadData(String key) {
		SharedPreferences preferences = getSharedPreferences("amp_data",
				Context.MODE_PRIVATE);
		int value = preferences.getInt(key, 0);
		return value;
	}

	/**
	 * 获取文件目录下的文件数目
	 * 
	 * @param path
	 *            //目录的绝对路径
	 * @return 目录下的文件数目
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
	 * 移动文件
	 * 
	 * @param srcFileName 源文件完整路径
	 * @param destDirName 目的文件完整路径
	 * @return 文件移动成功返回true，否则返回false
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
