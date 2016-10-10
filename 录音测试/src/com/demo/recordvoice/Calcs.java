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
	 * 信号样本数组归一化
	 * @param input
	 * @return double型数组
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
	 * 波形信号数组预加重处理
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
	 * 将数组进行分帧处理，帧长为240，帧移为80
	 * @param input double型数组
	 * @return 帧数组
	 */
	public double[][] divideToFrame(double[] input) {
		double[][] frames; //帧数组
		int count = input.length/80 - 2;  //帧数目 
		frames = new double[count][240];
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < 240; j++) {
				frames[i][j] = input[i*80 + j];
			}
		}
		return frames;
	}

	/**
	 * 求一帧信号的能量和
	 * @param arr double型
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
	 * 计算三角滤波窗函数
	 * @param a 三角形起始点横坐标
	 * @param b 三角形顶点横坐标
	 * @param c 三角形右端点横坐标
	 * @param i 横坐标
	 * @return i对应的纵坐标
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
	 * 计算数组的过零率
	 * @param frame
	 * @return 过零数
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
	 * 端点检测，获取有效帧
	 * @param frames 帧数组
	 * @return 一个double类型的二维数组
	 */
	public double[][] getEffectFrames(double[][] frames) {
		double[][] eFrames;  //有效帧数组
		
		double amp1 = 10;  //能量高门限
		double amp2 = 2;  //能量低门限
		//int zcr1 = 10; //过零率高门限
		int zcr2 = 5;  //过零率低门限
		int maxsilence = 6; //判断静音阈值  6*10ms = 60ms
		int minlen = 15;   //语音信号最小长度， 15*10 = 150ms
		int status = 0;   //判断状态，0=静音，1=可能开始，2=语音段
		int start = 0;  //语音开始端点帧序号
		//int end;    //语音终止端点帧序号
		int count = 0;   //语音帧数
		int silence = 0;  //空白帧数
		
		int length = frames.length; //输入的帧数组长度	
		int framelength = frames[0].length;  //帧长度
		double[] amps = new double[length]; //输入帧数组的能量数组
		double[] ampp = new double[length];
		int[] zcrs = new int[length];  //输入帧数组的过零率数组
		double ampMax = 0;  //帧能量最大值
		double[][] P = new double[length][framelength/2]; //帧的功率谱密度数组
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
		
		//调整能量门限
		amp1 = amp1<ampMax/16 ? amp1 : ampMax/16;
		amp2 = amp2<ampMax/ 64 ? amp2 : ampMax/64; 
		
		//Log.v(";eigiuehgiuwerhier", "amp1:"+amp1+"\namp2"+amp2);
		for (int i = 0; i < length; i++) {
			switch (status) {
			case 0:     
			case 1:
				if (amps[i]>amp1) {  //确定进入语音段
					if (start == 0) {
						start = i - count ;
						Log.v(TAG, "语音起始点帧序号："+start);
					}
					status = 2;
					silence = 0;
					count++;
				}else if (amps[i]>amp2 && amps[i]<=amp1 && zcrs[i]>zcr2) {  //可能处于语音段
					status = 1;
					count++;
				}else {  //静音状态
					status = 0;
					count = 0;
				}
				break;
			
			case 2:
				if (amps[i]>amp2 && zcrs[i]>zcr2) {  //保持在语音段
					count++;
				}else {     //语音将结束
					silence++;
					if (silence<maxsilence) {  //无声段不够长，尚未结束
						count++;
					} else if(count<minlen){   //语音长度太短，认为是噪声
						start = 0;
						status = 0;
						silence = 0;
						count = 0;
					}else {   //语音结束
						Log.v(TAG, "语音终止点帧序号："+(start+count));
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
	 * 离散傅里叶变换，获取能量谱
	 * @param frame
	 * @return  double类型的能量谱数组
	 */
	public double[] DFT(double[] frame) {
		double[] res;
		int length = frame.length;
		jtrans jtrans = new jtrans();  //离散变换库
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
	 * 将频谱与三角滤波器组分别相乘并积分，分别计算每个三角形下的能量
	 * @param fft 频谱
	 * @return  double类型的能量值数组
	 */
	public double[] sumWindows(double[] fft) {
		int length = fft.length;
		double[] ang_energies = new double[24];  //三角滤波器下的能量
		double[] freq = new double[length/2]; //频率数组，fft的每一个点对应一个频率，计算公式为f=(n-1)*Fs/N，其中Fs为采样频率，N为采样点数
		double tmp;
		for (int i = 0; i < 24; i++) {
			ang_energies[i] = 0; //初始化
		}
		for (int i = 0; i < length/2; i++) {
			freq[i] = i*8000/240;  //第i个点对应的频率
			
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
	 * 对能量值取对数并进行DCT变换
	 * @param energies 经三角滤波求和后的能量数组
	 * @return  double类型的MFCC系数数组
	 */
	public double[] LogAndDCT(double[] energies) {
		int len = energies.length;
		//求对数能量
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
	 * 计算两个向量的欧氏距离
	 * @param a
	 * @param b 数组a与b的长度相同
	 * @return double类型的距离值
	 */
	public double d(double[] a, double[] b) {
		
		if (a.length == b.length) {
			double distance ;  //距离
			double sum = 0; //平方和
			for (int i = 0; i < b.length; i++) {
				sum += Math.pow((a[i] - b[i]), 2);
			}
			distance = Math.sqrt(sum);			
			return distance;
		}else {
			Log.e("Calcs", "a和b维数不相等，无法计算距离");
			return Double.MAX_VALUE;
		}		
		
	}
	
	
	/**
	 * 获取语音特征参数
	 * @param voicename
	 * @return
	 */
	public double[][] getVoiceParams(String voicename) {
		
		double[] wavedata; //经过预加重的波形数组
		double[][] frames; //帧数组
		double[][] voice_frames; //有效帧数组
		waveFileReader = new WaveFileReader(voicename); 
		//Log.v(TAG, getCurrentSysTime());
		if (waveFileReader.isSuccess()) {
			int[] data = waveFileReader.getData()[0]; //获取第一声道
			//waveParam = data;
			if (data.length >= 1200) {
				wavedata = PreEmphasis(ToOne(data));  //将归一化的波形数据预加重
				//Log.v(TAG, "预加重处理完成，数组长度为："+wavedata.length);
				 
				//分帧
				frames = divideToFrame(wavedata);
				//Log.v(TAG, "分帧处理完成，帧数为："+frames.length);	
				 
				//端点检测，得到有效帧
				voice_frames = getEffectFrames(frames);
				//Log.v(TAG, "端点检测完成 "+"有效帧数为："+voice_frames.length);				
				
				if (voice_frames.length >=15) {
					
					//对每一帧提取MFCC参数
					double[][] mfccs = new double[voice_frames.length][12];
					for (int i = 0; i < voice_frames.length; i++) {
						mfccs[i] = LogAndDCT(DFT(voice_frames[i]));					
					}
					//Log.v(TAG, "提取MFCC参数完成 " );
					
					
					//求差分MFCC参数
					double[][] dt_mfccs = new double[voice_frames.length][12];
					for (int i = 2; i < mfccs.length-2; i++) {
						for (int j = 0; j < 12; j++) {
							dt_mfccs[i][j] = (2*mfccs[i+2][j] + mfccs[i+1][j] - mfccs[i-1][j] - 2*mfccs[i-2][j])/3;
						}
					}
					//Log.v(TAG, "提取差分MFCC参数完成 " );
					
					//获取由MFCC和一阶差分MFCC组成的特征参数
					double[][] mixParams = new double[voice_frames.length-4][24]; //特征参数数组
					for (int i = 0; i < mixParams.length; i++) {
						System.arraycopy(mfccs[i+2], 0, mixParams[i], 0, 12);
						System.arraycopy(dt_mfccs[i+2], 0, mixParams[i], 12, 12);
					}
					Log.v(TAG, "特征参数提取完成 " );
					
					return mixParams;
				}else {
					Log.e(TAG, "语音太短,请重新说话");
					return null;
				}			
				
			}	else {
				Log.e(TAG, "未检测到语音");
				return null;
			}		
			 
		 } else {
			Log.e(TAG, "音频数据读取失败");
			return null;
		}
		
	}	

	
	
	/**
	 * 动态规整算法求特征参数模板最小匹配距离
	 * @param res 参考模板参数
	 * @param tes 测试模板参数
	 * @return 最小匹配距离
	 */
	public double dtw(double[][] res, double[][] tes) {
		double distance;
		double realmax = Double.MAX_VALUE;
		
		if (res == null || tes == null) {
			return Double.MAX_VALUE;
		}
		
		int m = res.length;  //参考模板长度
		int n = tes.length;  //测试模板长度
		double[][] D = new double[m][n];  //最小距离和矩阵
		double[][] partd = new double[m][n];  //局部距离矩阵
		
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
	
	
	/**求三个数的最小值*/
	public double min(double a, double b, double c) {
		double min = a;
		if (b<min) min = b;
		if (c<min) min = c;
		
		return min;
	}
	
	/**求数组元素的最小值*/
	public double min(double[] input) {
		double output = input[0];
		for (int i = 0; i < input.length; i++) {
			if (output > input[i]) output = input[i];
		}
		
		return output;
	}
	
	
	/**求数组元素的最大值*/
	public double max(double[] input) {
		double output = input[0];
		for (int i = 0; i < input.length; i++) {
			if(output < input[i]) output = input[i];
		}
		
		return output;
	}
	
	/**数组元素求和*/
	public double sum(double[] input) {
		double output = 0;
		for (int i = 0; i < input.length; i++) {
			output+=input[i];
		}
		
		return output;
	}
	
	/**
	 * 获取当前系统时间
	 * @return
	 */
	public String getCurrentSysTime() {
		String str;
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy年MM月dd日 HH:mm:ss ");
		Date curDate = new Date(System.currentTimeMillis());//获取当前时间
		str = formatter.format(curDate);
		return str;
	}
	
	
	
	/**
	 * 获取一个模板组的最优匹配结果
	 * @param tesName  //String类型的模板文件名数组
	 * @param resNames  //String类型的测试语音文件名
	 * @return //一个长度为2的数组，a[0]为最优匹配距离，a[1]为啊a[0]/对应模板的长度
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
