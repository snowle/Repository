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
	public static String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath(); //获取SD卡根路径
	private String audioName;
	String[] strs;  //存储模板名称的数组
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
	
	
	/**修改导航栏*/
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
	
	/**初始化控件*/
	private void initView() {
		textViewstate = (TextView) findViewById(R.id.textviewxstate);
		textView1 = (TextView) findViewById(R.id.textviewx);
		textView2 = (TextView) findViewById(R.id.textviewx2);
		
		textViewstate.setText("请选择要录入的模板：");
		listView = (ListView) findViewById(R.id.listview);
		File file = new File(rootPath+"/录音测试/moulds");
		if (file.exists()) {
			strs = file.list();
			adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, strs);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(new MyClickListener());
			listView.setOnItemLongClickListener(new MyLongClickListener());
		}else {
			textViewstate.setText("请添加模板：");
		}
		
		bt_speak2 = (Button) findViewById(R.id.speak2);
		bt_speak2.setOnTouchListener(new OnTouchListener() {			
			@SuppressLint("ClickableViewAccessibility") @Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					bt_speak2.setBackgroundColor(0xff6495ed);
					bt_speak2.setText("松开停止录音");
					audioProcess.recordVoice(audioName);
					
					break;
					
				case MotionEvent.ACTION_UP:
					bt_speak2.setBackgroundColor(0xffd3d3d3);
					bt_speak2.setText("按下开始录音");				
					audioProcess.stopRecord();
					
					double[][] tmp = calcs.getVoiceParams(audioName);
					if (tmp == null) {
						File file = new File(audioName);
						if (file.exists()) {
							file.delete();
						}
						Toast.makeText(getApplicationContext(), "未检测到语音或者语音太短，请重新录入！", Toast.LENGTH_SHORT).show();
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
	 * 清空文件夹目录下面的文件
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
	
	
	/**列表元素点击事件*/
	class MyClickListener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
			String path = rootPath+"/录音测试/moulds/"+strs[position];
			textViewstate.setText(path);
			textView1.setText("当前选择要录入的模板： " + strs[position]);
			clearfileDir(path);
			audioName = path + "/" + getCurrentSysTime() + ".wav";
			if (!bt_speak2.isEnabled()) {
				bt_speak2.setEnabled(true);
			}
			
		}
		
	}
	
	/**
	 * 列表元素长按点击事件类
	 */
	class MyLongClickListener implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,int position, long id) {
			final File file = new File(rootPath+"/录音测试/moulds/"+strs[position]);
			Builder builder = new AlertDialog.Builder(AddModuleActivity.this).setTitle("确定删除？");
			builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					file.delete();
					strs = new File(rootPath+"/录音测试/moulds").list();
					adapter = new ArrayAdapter<String>(AddModuleActivity.this, android.R.layout.simple_list_item_single_choice, strs);
					adapter.notifyDataSetChanged();
					listView.setAdapter(adapter);	
					
				}
			}).setNegativeButton("取消", null);
			builder.create().show();			
			
			return false;
		}
		
	}
	
	
	
	/**ActionBar创建选项菜单*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.actionbar_menu2, menu);
		return true;
	}
	
	/**actionbar菜单点击事件*/
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
	
	
	/**设置popupwindow相关参数*/
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
				File file = new File(rootPath+"/录音测试/moulds/" + name);
				if (!file.exists()) {
					file.mkdirs();
				} else {
					Toast.makeText(getApplicationContext(), "模板目录已存在！", Toast.LENGTH_SHORT).show();
				}
				strs = new File(rootPath+"/录音测试/moulds").list();
				adapter = new ArrayAdapter<String>(AddModuleActivity.this, android.R.layout.simple_list_item_single_choice, strs);
				adapter.notifyDataSetChanged();
				listView.setAdapter(adapter);
				
				popup.dismiss();				
			}
		});			
	}
	
	
	
	/**
	 * 获取当前系统时间
	 * @return
	 */
	@SuppressLint("SimpleDateFormat") 
	public String getCurrentSysTime() {
		String str;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		str = formatter.format(curDate);
		return str;
	}


}
