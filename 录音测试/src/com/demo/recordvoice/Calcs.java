package com.demo.recordvoice;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.util.Log;
import ex.jtrans;

@SuppressLint("SimpleDateFormat") 
public class Calcs {
	
	private static final String TAG = "Calcs";
	WaveFileReader waveFileReader;

	/**
	 * �ź����������һ��
	 * @param input
	 * @return double������
	 */
	public double[] ToOne(int[] input) {
		double[] output = new double[input.length]; 
		double rate = 32767.0f;
		for (int i = 0; i < input.length; i++) {
			output[i] = (double)input[i]/rate;
		}		
		return output;
	}
	
	/**
	 * �����ź�����Ԥ���ش���
	 * @param input
	 * @return 
	 */
	public double[] PreEmphasis(double[] input) {
		double[] output = new double[input.length-1];
		double rate = 0.97f;
		for (int i = 0; i < input.length-1; i++) {
			output[i] = input[i+1] -rate * input[i];
		}		
		return output;	
	}
	
	/**
	 * ��������з�֡����֡��Ϊ240��֡��Ϊ80
	 * @param input double������
	 * @return ֡����
	 */
	public double[][] divideToFrame(double[] input) {
		double[][] frames; //֡����
		int count = input.length/80 - 2;  //֡��Ŀ 
		frames = new double[count][240];
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < 240; j++) {
				frames[i][j] = input[i*80 + j];
			}
		}
		return frames;
	}

	/**
	 * ��һ֡�źŵ�������
	 * @param arr double��
	 * @return
	 */
	public double sumEnergy(double[] arr) {
		
		double sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i]*arr[i];
		}
		
		return sum;		
	}
	
	/**
	 * ���������˲�������
	 * @param a ��������ʼ�������
	 * @param b �����ζ��������
	 * @param c �������Ҷ˵������
	 * @param i ������
	 * @return i��Ӧ��������
	 */
	public double windowCac(double a, double b, double c, double i) {
		double result = 0;
		if (i>a&&i<=b) {
			result = (1/(b-a))*(i-a);
		} else if (i>b&&i<c) {
			result = (1/(c-b))*(c-i);
		} else {
			result = 0;
		}		
		
		return result;		
	}

	/**
	 * ��������Ĺ�����
	 * @param frame
	 * @return ������
	 */
	public int zeroRate(double[] frame) {
		int count = 0;
		double minus = 0f;
		for (int i = 1; i < frame.length; i++) {
			minus = Math.abs(frame[i-1]-frame[i]);
			if (frame[i-1]*frame[i]<0 && minus>0.02) {
				count++;
			}
		}
		return count;		
	}
	
	/**
	 * �˵��⣬��ȡ��Ч֡
	 * @param frames ֡����
	 * @return һ��double���͵Ķ�ά����
	 */
	public double[][] getEffectFrames(double[][] frames) {
		double[][] eFrames;  //��Ч֡����
		
		double amp1 = 10;  //����������
		double amp2 = 2;  //����������
		//int zcr1 = 10; //�����ʸ�����
		int zcr2 = 5;  //�����ʵ�����
		int maxsilence = 6; //�жϾ�����ֵ  6*10ms = 60ms
		int minlen = 15;   //�����ź���С���ȣ� 15*10 = 150ms
		int status = 0;   //�ж�״̬��0=������1=���ܿ�ʼ��2=������
		int start = 0;  //������ʼ�˵�֡���
		//int end;    //������ֹ�˵�֡���
		int count = 0;   //����֡��
		int silence = 0;  //�հ�֡��
		
		int length = frames.length; //�����֡���鳤��	
		int framelength = frames[0].length;  //֡����
		double[] amps = new double[length]; //����֡�������������
		double[] ampp = new double[length];
		int[] zcrs = new int[length];  //����֡����Ĺ���������
		double ampMax = 0;  //֡�������ֵ
		double[][] P = new double[length][framelength/2]; //֡�Ĺ������ܶ�����
		double[] H = new double[length];
		double[] tmpE;
		
		for (int i = 0; i < length; i++) {
			amps[i] = sumEnergy(frames[i]);
			zcrs[i] = zeroRate(frames[i]);
			tmpE = DFT(frames[i]);
			
			for (int j = 0; j < framelength/2; j++) {
				P[i][j] = tmpE[j]/(sum(tmpE)/2);
				H[i] += P[i][j]*Math.log10(P[i][j]);
			}			
		}
		
		double minH = min(H);
		for (int i = 0; i < ampp.length; i++) {
			H[i] = H[i] - minH;
			ampp[i] = H[i]*amps[i];
		}
		
		ampMax = max(ampp);
		
		//������������
		amp1 = amp1<ampMax/16 ? amp1 : ampMax/16;
		amp2 = amp2<ampMax/ 64 ? amp2 : ampMax/64; 
		
		//Log.v(";eigiuehgiuwerhier", "amp1:"+amp1+"\namp2"+amp2);
		for (int i = 0; i < length; i++) {
			switch (status) {
			case 0:     
			case 1:
				if (amps[i]>amp1) {  //ȷ������������
					if (start == 0) {
						start = i - count ;
						Log.v(TAG, "������ʼ��֡��ţ�"+start);
					}
					status = 2;
					silence = 0;
					count++;
				}else if (amps[i]>amp2 && amps[i]<=amp1 && zcrs[i]>zcr2) {  //���ܴ���������
					status = 1;
					count++;
				}else {  //����״̬
					status = 0;
					count = 0;
				}
				break;
			
			case 2:
				if (amps[i]>amp2 && zcrs[i]>zcr2) {  //������������
					count++;
				}else {     //����������
					silence++;
					if (silence<maxsilence) {  //�����β���������δ����
						count++;
					} else if(count<minlen){   //��������̫�̣���Ϊ������
						start = 0;
						status = 0;
						silence = 0;
						count = 0;
					}else {   //��������
						Log.v(TAG, "������ֹ��֡��ţ�"+(start+count));
						status = 3;
					}
				}
				break;	
				
			case 3:
				break;					
			}
		}
		
		//end = start+count;
		eFrames = new double[count][];
		System.arraycopy(frames, start, eFrames, 0, count);
		
		return eFrames;
	}
	
	/**
	 * ��ɢ����Ҷ�任����ȡ������
	 * @param frame
	 * @return  double���͵�����������
	 */
	public double[] DFT(double[] frame) {
		double[] res;
		int length = frame.length;
		jtrans jtrans = new jtrans();  //��ɢ�任��
		zx.complex[] complexs = jtrans.fft(frame);
		res = new double[length];
		double r,img;		
		for (int i = 0; i < length; i++) {
			r = complexs[i].real;
			img = complexs[i].imag;
			res[i] = r*r + img*img;
		}
		
		return res;		
	}
	
	/**
	 * ��Ƶ���������˲�����ֱ���˲����֣��ֱ����ÿ���������µ�����
	 * @param fft Ƶ��
	 * @return  double���͵�����ֵ����
	 */
	public double[] sumWindows(double[] fft) {
		int length = fft.length;
		double[] ang_energies = new double[24];  //�����˲����µ�����
		double[] freq = new double[length/2]; //Ƶ�����飬fft��ÿһ�����Ӧһ��Ƶ�ʣ����㹫ʽΪf=(n-1)*Fs/N������FsΪ����Ƶ�ʣ�NΪ��������
		double tmp;
		for (int i = 0; i < 24; i++) {
			ang_energies[i] = 0; //��ʼ��
		}
		for (int i = 0; i < length/2; i++) {
			freq[i] = i*8000/240;  //��i�����Ӧ��Ƶ��
			
			tmp = fft[i]*windowCac(0, 28, 89, freq[i]); 
			ang_energies[0] += tmp;
			
			tmp = fft[i]*windowCac(28, 89, 154, freq[i]); 
			ang_energies[1] += tmp;
			
			tmp = fft[i]*windowCac(89, 154, 224, freq[i]); 
			ang_energies[2] += tmp;
			
			tmp = fft[i]*windowCac(154, 224, 300, freq[i]); 
			ang_energies[3] += tmp;
			
			tmp = fft[i]*windowCac(224, 300, 383, freq[i]); 
			ang_energies[4] += tmp;
			
			tmp = fft[i]*windowCac(300, 383, 472, freq[i]); 
			ang_energies[5] += tmp;
			
			tmp = fft[i]*windowCac(383, 472, 569, freq[i]);
			ang_energies[6] += tmp;
			
			tmp = fft[i]*windowCac(472, 569, 674, freq[i]);
			ang_energies[7] += tmp;
			
			tmp = fft[i]*windowCac(569, 674, 787, freq[i]); 
			ang_energies[8] += tmp;
			
			tmp = fft[i]*windowCac(674, 787, 907, freq[i]); 
			ang_energies[9] += tmp;
			
			tmp = fft[i]*windowCac(787, 907, 1043, freq[i]); 
			ang_energies[10] += tmp;
			
			tmp = fft[i]*windowCac(907, 1043, 1187, freq[i]); 
			ang_energies[11] += tmp;
			
			tmp = fft[i]*windowCac(1043, 1187, 1343, freq[i]); 
			ang_energies[12] += tmp;
			
			tmp = fft[i]*windowCac(1187, 1343, 1512, freq[i]);
			ang_energies[13] += tmp;
			
			tmp = fft[i]*windowCac(1343, 1512, 1694, freq[i]); 
			ang_energies[14] += tmp;
			
			tmp = fft[i]*windowCac(1512, 1694, 1892, freq[i]); 
			ang_energies[15] += tmp;
			
			tmp = fft[i]*windowCac(1694, 1892, 2106, freq[i]); 
			ang_energies[16] += tmp;
			
			tmp = fft[i]*windowCac(1892, 2106, 2338, freq[i]); 
			ang_energies[17] += tmp;
			
			tmp = fft[i]*windowCac(2106, 2338, 2589, freq[i]); 
			ang_energies[18] += tmp;
			
			tmp = fft[i]*windowCac(2338, 2589, 2860, freq[i]); 
			ang_energies[19] += tmp;
			
			tmp = fft[i]*windowCac(2589, 2860, 3154, freq[i]); 
			ang_energies[20] += tmp;
			
			tmp = fft[i]*windowCac(2860, 3154, 3472, freq[i]); 
			ang_energies[21] += tmp;
			
			tmp = fft[i]*windowCac(3154, 3472, 3817, freq[i]); 
			ang_energies[22] += tmp;
			
			tmp = fft[i]*windowCac(3472, 3817, 4000, freq[i]); 
			ang_energies[23] += tmp;
			
		}
		
		return ang_energies;
	}
	
	/**
	 * ������ֵȡ����������DCT�任
	 * @param energies �������˲���ͺ����������
	 * @return  double���͵�MFCCϵ������
	 */
	public double[] LogAndDCT(double[] energies) {
		int len = energies.length;
		//���������
		double[] logEnergies = new double[len];  
		for (int i = 0; i < len; i++) {
			logEnergies[i] = Math.log(energies[i]);
		}
		//DCT
		double[] mfcc = new double[12];
		jtrans jtrans = new jtrans();
		double[] tmp = jtrans.dct(logEnergies);
		System.arraycopy(tmp, 0, mfcc, 0, 12);
		
		return mfcc;		
	}
	
	/**
	 * ��������������ŷ�Ͼ���
	 * @param a
	 * @param b ����a��b�ĳ�����ͬ
	 * @return double���͵ľ���ֵ
	 */
	public double d(double[] a, double[] b) {
		
		if (a.length == b.length) {
			double distance ;  //����
			double sum = 0; //ƽ����
			for (int i = 0; i < b.length; i++) {
				sum += Math.pow((a[i] - b[i]), 2);
			}
			distance = Math.sqrt(sum);			
			return distance;
		}else {
			Log.e("Calcs", "a��bά������ȣ��޷��������");
			return Double.MAX_VALUE;
		}		
		
	}
	
	
	/**
	 * ��ȡ������������
	 * @param voicename
	 * @return
	 */
	public double[][] getVoiceParams(String voicename) {
		
		double[] wavedata; //����Ԥ���صĲ�������
		double[][] frames; //֡����
		double[][] voice_frames; //��Ч֡����
		waveFileReader = new WaveFileReader(voicename); 
		//Log.v(TAG, getCurrentSysTime());
		if (waveFileReader.isSuccess()) {
			int[] data = waveFileReader.getData()[0]; //��ȡ��һ����
			//waveParam = data;
			if (data.length >= 1200) {
				wavedata = PreEmphasis(ToOne(data));  //����һ���Ĳ�������Ԥ����
				//Log.v(TAG, "Ԥ���ش�����ɣ����鳤��Ϊ��"+wavedata.length);
				 
				//��֡
				frames = divideToFrame(wavedata);
				//Log.v(TAG, "��֡������ɣ�֡��Ϊ��"+frames.length);	
				 
				//�˵��⣬�õ���Ч֡
				voice_frames = getEffectFrames(frames);
				//Log.v(TAG, "�˵������ "+"��Ч֡��Ϊ��"+voice_frames.length);				
				
				if (voice_frames.length >=15) {
					
					//��ÿһ֡��ȡMFCC����
					double[][] mfccs = new double[voice_frames.length][12];
					for (int i = 0; i < voice_frames.length; i++) {
						mfccs[i] = LogAndDCT(DFT(voice_frames[i]));					
					}
					//Log.v(TAG, "��ȡMFCC������� " );
					
					
					//����MFCC����
					double[][] dt_mfccs = new double[voice_frames.length][12];
					for (int i = 2; i < mfccs.length-2; i++) {
						for (int j = 0; j < 12; j++) {
							dt_mfccs[i][j] = (2*mfccs[i+2][j] + mfccs[i+1][j] - mfccs[i-1][j] - 2*mfccs[i-2][j])/3;
						}
					}
					//Log.v(TAG, "��ȡ���MFCC������� " );
					
					//��ȡ��MFCC��һ�ײ��MFCC��ɵ���������
					double[][] mixParams = new double[voice_frames.length-4][24]; //������������
					for (int i = 0; i < mixParams.length; i++) {
						System.arraycopy(mfccs[i+2], 0, mixParams[i], 0, 12);
						System.arraycopy(dt_mfccs[i+2], 0, mixParams[i], 12, 12);
					}
					Log.v(TAG, "����������ȡ��� " );
					
					return mixParams;
				}else {
					Log.e(TAG, "����̫��,������˵��");
					return null;
				}			
				
			}	else {
				Log.e(TAG, "δ��⵽����");
				return null;
			}		
			 
		 } else {
			Log.e(TAG, "��Ƶ���ݶ�ȡʧ��");
			return null;
		}
		
	}	

	
	
	/**
	 * ��̬�����㷨����������ģ����Сƥ�����
	 * @param res �ο�ģ�����
	 * @param tes ����ģ�����
	 * @return ��Сƥ�����
	 */
	public double dtw(double[][] res, double[][] tes) {
		double distance;
		double realmax = Double.MAX_VALUE;
		
		if (res == null || tes == null) {
			return Double.MAX_VALUE;
		}
		
		int m = res.length;  //�ο�ģ�峤��
		int n = tes.length;  //����ģ�峤��
		double[][] D = new double[m][n];  //��С����;���
		double[][] partd = new double[m][n];  //�ֲ��������
		
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				partd[i][j] = d(res[i], tes[j]);
			}			
		}
		D[0][0] = partd[0][0];
		
		//for (int i = 1; i < m; i++) D[i][0] = D[i-1][0] + partd[i][0];
		for (int j = 1; j < n; j++) D[0][j] = D[0][j-1] + partd[0][j];	
			
		double D1,D2,D3;
		for (int i = 1; i < m; i++) {
			for (int j = 0; j < n; j++) {
				D1 = D[i-1][j];
				
				if (j>0) {
					D2 = D[i-1][j-1];
					D3 = D[i][j-1];
				}else {
					D2 = realmax;
					D3 = realmax;
				}
				
				D[i][j] = min(partd[i][j]+D1, partd[i][j]*2+D2, partd[i][j]+D3)	;		 	
			}
		}
		
		distance = D[m-1][n-1];

		return distance;
	}
	
	
	/**������������Сֵ*/
	public double min(double a, double b, double c) {
		double min = a;
		if (b<min) min = b;
		if (c<min) min = c;
		
		return min;
	}
	
	/**������Ԫ�ص���Сֵ*/
	public double min(double[] input) {
		double output = input[0];
		for (int i = 0; i < input.length; i++) {
			if (output > input[i]) output = input[i];
		}
		
		return output;
	}
	
	
	/**������Ԫ�ص����ֵ*/
	public double max(double[] input) {
		double output = input[0];
		for (int i = 0; i < input.length; i++) {
			if(output < input[i]) output = input[i];
		}
		
		return output;
	}
	
	/**����Ԫ�����*/
	public double sum(double[] input) {
		double output = 0;
		for (int i = 0; i < input.length; i++) {
			output+=input[i];
		}
		
		return output;
	}
	
	/**
	 * ��ȡ��ǰϵͳʱ��
	 * @return
	 */
	public String getCurrentSysTime() {
		String str;
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy��MM��dd�� HH:mm:ss ");
		Date curDate = new Date(System.currentTimeMillis());//��ȡ��ǰʱ��
		str = formatter.format(curDate);
		return str;
	}
	
	
	
	/**
	 * ��ȡһ��ģ���������ƥ����
	 * @param tesName  //String���͵�ģ���ļ�������
	 * @param resNames  //String���͵Ĳ��������ļ���
	 * @return //һ������Ϊ2�����飬a[0]Ϊ����ƥ����룬a[1]Ϊ��a[0]/��Ӧģ��ĳ���
	 */
	@SuppressLint("SimpleDateFormat") 
	public double[] getMinDis(String tesName, String[] resNames ) {
		double[] results = new double[]{Double.MAX_VALUE,0};
		double distance;
		double[][] tmpdouble1, tmpdouble2;
		
		tmpdouble1 = getVoiceParams(tesName);
		int tmp1 = tmpdouble1.length;
		for (int i = 0; i < resNames.length; i++) {
			tmpdouble2 = getVoiceParams(resNames[i]);
			distance = dtw(tmpdouble1, tmpdouble2);
			if (distance < results[0]) {
				results[0] = distance;
				results[1] = distance/(tmp1+tmpdouble2.length+8);
			}
			
		}
		return results;		
	}

}
