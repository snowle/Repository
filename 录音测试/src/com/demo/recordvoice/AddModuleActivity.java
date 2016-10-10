package com.demo.recordvoice;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi") 
public class AddModuleActivity extends Activity{
	
	Button bt_speak2 ,popButton;
	EditText popEditText;
	ListView listView;
	TextView textView1,textView2,textViewstate;
	PopupWindow popup;
	AudioProcess audioProcess;
	Calcs calcs;
	public static String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath(); //��ȡSD����·��
	private String audioName;
	String[] strs;  //�洢ģ�����Ƶ�����
	ArrayAdapter<String> adapter;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_addmod);
		setActionBar();
		
		initView();
		audioProcess = new AudioProcess();
		calcs = new Calcs();		
		
	}
	
	
	/**�޸ĵ�����*/
	public void setActionBar(){
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(R.layout.actionbar);
		actionBar.getCustomView().setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	/**��ʼ���ؼ�*/
	private void initView() {
		textViewstate = (TextView) findViewById(R.id.textviewxstate);
		textView1 = (TextView) findViewById(R.id.textviewx);
		textView2 = (TextView) findViewById(R.id.textviewx2);
		
		textViewstate.setText("��ѡ��Ҫ¼���ģ�壺");
		listView = (ListView) findViewById(R.id.listview);
		File file = new File(rootPath+"/¼������/moulds");
		if (file.exists()) {
			strs = file.list();
			adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, strs);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(new MyClickListener());
			listView.setOnItemLongClickListener(new MyLongClickListener());
		}else {
			textViewstate.setText("�����ģ�壺");
		}
		
		bt_speak2 = (Button) findViewById(R.id.speak2);
		bt_speak2.setOnTouchListener(new OnTouchListener() {			
			@SuppressLint("ClickableViewAccessibility") @Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					bt_speak2.setBackgroundColor(0xff6495ed);
					bt_speak2.setText("�ɿ�ֹͣ¼��");
					audioProcess.recordVoice(audioName);
					
					break;
					
				case MotionEvent.ACTION_UP:
					bt_speak2.setBackgroundColor(0xffd3d3d3);
					bt_speak2.setText("���¿�ʼ¼��");				
					audioProcess.stopRecord();
					
					double[][] tmp = calcs.getVoiceParams(audioName);
					if (tmp == null) {
						File file = new File(audioName);
						if (file.exists()) {
							file.delete();
						}
						Toast.makeText(getApplicationContext(), "δ��⵽������������̫�̣�������¼�룡", Toast.LENGTH_SHORT).show();
					}
					break;

				default:
					break;
				}

				return true;
			}
		});
		bt_speak2.setEnabled(false);
		
	}
	
	/**
	 * ����ļ���Ŀ¼������ļ�
	 * @param fileneme
	 */
	public void clearfileDir(String filedir) {
		File file = new File(filedir);
		if (!file.exists() || !file.isDirectory()) return;		
		File[] filelist = file.listFiles();
		if (filelist!=null) {
			for (int i = 0; i < filelist.length; i++) {
				filelist[i].delete();
			}
		}			
	}
	
	
	/**�б�Ԫ�ص���¼�*/
	class MyClickListener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
			String path = rootPath+"/¼������/moulds/"+strs[position];
			textViewstate.setText(path);
			textView1.setText("��ǰѡ��Ҫ¼���ģ�壺 " + strs[position]);
			clearfileDir(path);
			audioName = path + "/" + getCurrentSysTime() + ".wav";
			if (!bt_speak2.isEnabled()) {
				bt_speak2.setEnabled(true);
			}
			
		}
		
	}
	
	/**
	 * �б�Ԫ�س�������¼���
	 */
	class MyLongClickListener implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,int position, long id) {
			final File file = new File(rootPath+"/¼������/moulds/"+strs[position]);
			Builder builder = new AlertDialog.Builder(AddModuleActivity.this).setTitle("ȷ��ɾ����");
			builder.setPositiveButton("ɾ��", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					file.delete();
					strs = new File(rootPath+"/¼������/moulds").list();
					adapter = new ArrayAdapter<String>(AddModuleActivity.this, android.R.layout.simple_list_item_single_choice, strs);
					adapter.notifyDataSetChanged();
					listView.setAdapter(adapter);	
					
				}
			}).setNegativeButton("ȡ��", null);
			builder.create().show();			
			
			return false;
		}
		
	}
	
	
	
	/**ActionBar����ѡ��˵�*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.actionbar_menu2, menu);
		return true;
	}
	
	/**actionbar�˵�����¼�*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.plus:
			setPopupParams();
			popup.showAtLocation(listView, Gravity.CENTER, 0, -200);
			
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	/**����popupwindow��ز���*/
	@SuppressLint("InflateParams") @SuppressWarnings("deprecation")
	private void setPopupParams() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int disPlayWidth =dm.widthPixels; 
		int displayHeight = dm.heightPixels;
		View popupView = getLayoutInflater().inflate(R.layout.popup2, null);
		popup = new PopupWindow(popupView, disPlayWidth*4/5, displayHeight*1/5);
		popup.setBackgroundDrawable(new BitmapDrawable());
		popup.setFocusable(true);
		popup.setOutsideTouchable(true);
		popButton = (Button) popupView.findViewById(R.id.popButton);
		popEditText = (EditText) popupView.findViewById(R.id.popEditText);
		popButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				String name = popEditText.getText().toString();
				File file = new File(rootPath+"/¼������/moulds/" + name);
				if (!file.exists()) {
					file.mkdirs();
				} else {
					Toast.makeText(getApplicationContext(), "ģ��Ŀ¼�Ѵ��ڣ�", Toast.LENGTH_SHORT).show();
				}
				strs = new File(rootPath+"/¼������/moulds").list();
				adapter = new ArrayAdapter<String>(AddModuleActivity.this, android.R.layout.simple_list_item_single_choice, strs);
				adapter.notifyDataSetChanged();
				listView.setAdapter(adapter);
				
				popup.dismiss();				
			}
		});			
	}
	
	
	
	/**
	 * ��ȡ��ǰϵͳʱ��
	 * @return
	 */
	@SuppressLint("SimpleDateFormat") 
	public String getCurrentSysTime() {
		String str;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());// ��ȡ��ǰʱ��
		str = formatter.format(curDate);
		return str;
	}


}
