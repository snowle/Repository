package com.demo.recordvoice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class AudioProcess {
	
	public static String TAG = "AudioProcess";
	AudioRecord audioRecord;
	WaveFileReader waveFileReader; //声明读取WAV文件数据类
//	Context context;
//	String fileFold;
	String filename;
	
	int audioSource = MediaRecorder.AudioSource.MIC; // 音频获取源 
	int sampleRateInHz = 8000; // 设置音频采样率
	int channelConfig = AudioFormat.CHANNEL_IN_STEREO;   // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道
	int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // 音频数据格式:PCM 16位每个样本
	
	String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath(); //获取SD卡根路径
	String AudioName = rootPath+"/录音测试/voice.pcm";  //AudioName裸音频数据文件 
	
	public static boolean isRecord = false;// 设置正在录制的状态
    int bufferSizeInBytes = 0; // 缓冲区字节大小
    int readsize = 0;
    
    
    /**
	 * 录音
	 */
    public void  recordVoice(String audiofilename) {
    	bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat); // 获得缓冲区字节大小 
        Log.v(TAG, "采样率："+sampleRateInHz+"\n缓冲区字节大小:"+bufferSizeInBytes);
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes); // 创建AudioRecord对象
        audioRecord.startRecording();
        filename = audiofilename;
        File file = new File(filename);
        File fold = new File(file.getParent());
        Log.v(TAG, ""+file.getParent());
        if (!fold.exists()) {
        	fold.mkdirs();
		}
        isRecord = true; // 设置录制状态为true 
        new Thread(new WriteToFileRunnable()).start();//开启文件写入线程
        
	}
    
    
    /**
	 * 停止录音
	 */
	public void stopRecord() {
		if (audioRecord!=null) {
			isRecord = false; //停止文件写入
			audioRecord.stop(); 
			audioRecord.release();//释放资源 
            audioRecord = null; 
		}
	}
	
	
	/**
	 *创建一个文件写入类，实现Runnable接口
	 */
	class WriteToFileRunnable implements Runnable{

		@Override
		public void run() {
			byte[] audiodata = new byte[bufferSizeInBytes]; //new一个byte数组用来存一些字节数据，大小为缓冲区大小
			//Log.v(TAG, "缓冲区字节大小为："+bufferSizeInBytes);
			FileOutputStream fos = null; 
			int readsize = 0; 
			try { 
				File folder = new File(rootPath+"/录音测试");
				if (!folder.exists()) {
					folder.mkdirs();
					Log.v(TAG, "创建文件夹"+folder.getPath());
				}
				File file = new File(AudioName); 
		        if (file.exists()) { 
		            file.delete(); 
		        } 
		        fos = new FileOutputStream(file);// 建立一个可存取字节的文件 
		     } catch (Exception e) { 
		        e.printStackTrace(); 
		     } 
			 
			 while (isRecord == true ){
				 readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes); 				 
				 //Log.v(TAG, "缓冲字节长度："+readsize);
				 if (AudioRecord.ERROR_INVALID_OPERATION != readsize){
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
			 copyWaveFile(AudioName, filename);
		}		
		
	}
	
	/**
	 * 得到可播放的音频文件 
	 * @param inFilename
	 * @param outFilename
	 */
	public void copyWaveFile(String inFilename, String outFilename){
		
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
			 WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate); 
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
	 * @param out
	 * @param totalAudioLen
	 * @param totalDataLen
	 * @param longSampleRate
	 * @param channels
	 * @param byteRate
	 * @throws IOException
	 */
	public void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, 
			long totalDataLen, long longSampleRate, int channels, long byteRate)throws IOException { 
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

}
