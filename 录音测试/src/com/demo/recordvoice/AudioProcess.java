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
	WaveFileReader waveFileReader; //������ȡWAV�ļ�������
//	Context context;
//	String fileFold;
	String filename;
	
	int audioSource = MediaRecorder.AudioSource.MIC; // ��Ƶ��ȡԴ 
	int sampleRateInHz = 8000; // ������Ƶ������
	int channelConfig = AudioFormat.CHANNEL_IN_STEREO;   // ������Ƶ��¼�Ƶ�����CHANNEL_IN_STEREOΪ˫����
	int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // ��Ƶ���ݸ�ʽ:PCM 16λÿ������
	
	String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath(); //��ȡSD����·��
	String AudioName = rootPath+"/¼������/voice.pcm";  //AudioName����Ƶ�����ļ� 
	
	public static boolean isRecord = false;// ��������¼�Ƶ�״̬
    int bufferSizeInBytes = 0; // �������ֽڴ�С
    int readsize = 0;
    
    
    /**
	 * ¼��
	 */
    public void  recordVoice(String audiofilename) {
    	bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat); // ��û������ֽڴ�С 
        Log.v(TAG, "�����ʣ�"+sampleRateInHz+"\n�������ֽڴ�С:"+bufferSizeInBytes);
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes); // ����AudioRecord����
        audioRecord.startRecording();
        filename = audiofilename;
        File file = new File(filename);
        File fold = new File(file.getParent());
        Log.v(TAG, ""+file.getParent());
        if (!fold.exists()) {
        	fold.mkdirs();
		}
        isRecord = true; // ����¼��״̬Ϊtrue 
        new Thread(new WriteToFileRunnable()).start();//�����ļ�д���߳�
        
	}
    
    
    /**
	 * ֹͣ¼��
	 */
	public void stopRecord() {
		if (audioRecord!=null) {
			isRecord = false; //ֹͣ�ļ�д��
			audioRecord.stop(); 
			audioRecord.release();//�ͷ���Դ 
            audioRecord = null; 
		}
	}
	
	
	/**
	 *����һ���ļ�д���࣬ʵ��Runnable�ӿ�
	 */
	class WriteToFileRunnable implements Runnable{

		@Override
		public void run() {
			byte[] audiodata = new byte[bufferSizeInBytes]; //newһ��byte����������һЩ�ֽ����ݣ���СΪ��������С
			//Log.v(TAG, "�������ֽڴ�СΪ��"+bufferSizeInBytes);
			FileOutputStream fos = null; 
			int readsize = 0; 
			try { 
				File folder = new File(rootPath+"/¼������");
				if (!folder.exists()) {
					folder.mkdirs();
					Log.v(TAG, "�����ļ���"+folder.getPath());
				}
				File file = new File(AudioName); 
		        if (file.exists()) { 
		            file.delete(); 
		        } 
		        fos = new FileOutputStream(file);// ����һ���ɴ�ȡ�ֽڵ��ļ� 
		     } catch (Exception e) { 
		        e.printStackTrace(); 
		     } 
			 
			 while (isRecord == true ){
				 readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes); 				 
				 //Log.v(TAG, "�����ֽڳ��ȣ�"+readsize);
				 if (AudioRecord.ERROR_INVALID_OPERATION != readsize){
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
			 copyWaveFile(AudioName, filename);
		}		
		
	}
	
	/**
	 * �õ��ɲ��ŵ���Ƶ�ļ� 
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
	 * �ṩһ��ͷ��Ϣ��������Щ��Ϣ�Ϳ��Եõ����Բ��ŵ��ļ���
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
