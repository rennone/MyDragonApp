package com.net_dimension.android_jni.mxdemoapp.simpleapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.net_dimension.android_jni.mx.MatrixEngineCallback;
import com.net_dimension.android_jni.mx.MatrixEngineSetting;
import com.net_dimension.android_jni.mx.MatrixEngineView;
//import android.hardware.SensorManager;
// MultiTouchここから(コールバックがあるひとは組み合わせて)
class MyCallback<T> implements MatrixEngineCallback<T>
{
	private int   field = 3;
	private int   count = 2;
	private int[] state = new int[count*field];
		
	MyCallback()
	{
		for(int i=0; i<count*field; i++) { state[i] = 0; }
	}
		
	public int embeddedGetIntArray(T mxe, int command, int[] value)
	{
		switch(command)
		{
			case ExCommand_UserCommand+0x101001: return mxTouchEvent(value);
		}
		return count;
	}
	
	private int mxTouchEvent(int[] value)
	{
		for(int id=0; id<count; id++)
		{
			value[id*field + 0] = state[id*field + 0];
			value[id*field + 1] = state[id*field + 1];
			value[id*field + 2] = state[id*field + 2];
		}	
		return count*field;
	}
	
	public void touchClear()
	{
		for(int id=0; id<count; id++)
		{
			state[id*field + 0]=0;
		}
	}	
	
	public void touchSet(int id, int x, int y)
	{
		if(id>=count) return;
		state[id*field + 0] = 1;
		state[id*field + 1] = x;
		state[id*field + 2] = y;
	}
	
	public void touchReset(int id)
	{
		if(id>=count) return;
		state[id*field + 0] = 0;
	}
	
	public void    contentsFinished     (T mxe, int err)                    {              }
	public void    onEnterDrawFrame     (T mxe, GL10 gl)                    {              }
	public void    onExitDrawFrame      (T mxe, GL10 gl)                    {              }
	public void    onSurfaceChanged     (T mxe, GL10 gl, int wid, int hei)  {              } 
	public void    onSurfaceCreated     (T mxe, GL10 gl, EGLConfig config)  {              }
	public boolean embeddedSetState     (T mxe, int command, int value)     { return true; }
	public boolean embeddedGetState     (T mxe, int command, int[] value)   { return true; }
	public boolean embeddedSetFloat     (T mxe, int command, float value)   { return true; }
	public boolean embeddedGetFloat     (T mxe, int command, float[] value) { return true; }
	public boolean embeddedSetString    (T mxe, int command, byte[] str)    { return true; }
	public int     embeddedGetByteString(T mxe, int command, byte[] str)    { return 0;    }	
	public int     embeddedSetIntArray  (T mxe, int command, int[] value)   { return 0;    }
}
// MultiTouchここまで

public class SimpleApp extends Activity
{	
	static final private String CONTENT_FILENAME = "sample.mxra";    
	private Handler  mHandler = new Handler();
	ProgressBar      mProgress;
	MatrixEngineView mMxe;

	// MultiTouchここから
	MyCallback<MatrixEngineSetting> mMxeCallback;
	// MultiTouchここまで

	@Override protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setup();
	}
	
	@Override protected void onPause()
	{
		super.onPause();
		if(mMxe != null) mMxe.onPause();
	}
	
	@Override protected void onResume()
	{
		super.onResume();
		if(mMxe != null) mMxe.onResume();
	}  
	
	@Override public void onDestroy()
	{
		super.onDestroy();
	}
	
	// MultiTouchここから
	@Override public boolean onTouchEvent(MotionEvent event)
	{
		// 全ポインタに関する情報を取得、押されている状態にセット
		mMxeCallback.touchClear();
		for(int i=0; i<event.getPointerCount(); i++)
		{
			int id = event.getPointerId(i);
			int px = (int)event.getX(i);
			int py = (int)event.getY(i);
			
			mMxeCallback.touchSet(id, px, py);
		}

		// 離された場合でも上の処理にかかってしまうため修正
		switch(event.getAction()& MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				// 何もしない
			break;
				
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_OUTSIDE:
				mMxeCallback.touchReset(event.getPointerId(event.getActionIndex()));
			break;
		}
		
		return super.onTouchEvent(event);
	} 
	// MultiTouchここまで
	
    private boolean needToSetupContents()
    {
		String currentVersionPath = "/data/data/" + getPackageName() + "/version.txt";
		try
		{
			try
			{
				BufferedReader in;
				String line;
				
				// Assets version.txt 読み込み
				AssetManager as = getAssets();
				in = new BufferedReader(new InputStreamReader(as.open("version.txt"), "UTF-8"));
				line = in.readLine();
				in.close();
				if(line == null) return true;
	
				// Android version.txt　読み込み
				int assetVersion = Integer.parseInt(line);
				in = new BufferedReader(new InputStreamReader(new FileInputStream(currentVersionPath), "UTF-8"));
				line = in.readLine();
				in.close();
				if(line == null) return true;
				
				// version.txt　内容を比較
				int currentVersion = Integer.parseInt(line);
				if(assetVersion < 0 || currentVersion < assetVersion)
				{
					return true;
				}
				
				return false;
    		}
    		catch(java.lang.NumberFormatException e)
    		{
    			return true;
    		}
		}
		catch (java.io.IOException e)
		{
			return true;
		}
    }
    
    private void setup()
    {
    	if(needToSetupContents())	{ setupContents();     }
    	else						{ setupMatrixEngine(); }	
    }

    private void setupMatrixEngine()
    {
    	MatrixEngineSetting setting  = new MatrixEngineSetting();

		// MultiTouchここから
    	mMxeCallback = new MyCallback<MatrixEngineSetting>();
		setting.callback      = mMxeCallback;
		setting.maxMultiTouch = 2; // 念のため1にセット
		// MultiTouchここまで

		setting.textMode      = MatrixEngineSetting.TEXTMODE_ROTATABLE;
		setting.filename = "/data/data/" + getPackageName() + "/" + CONTENT_FILENAME;
		//setting.sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		
		mMxe = new MatrixEngineView(this, setting);
		// MultiTouchここから
		mMxe.setEventMode(MatrixEngineView.EVENTMODE_NONE);
		// MultiTouchここまで
		
		setContentView(mMxe);    	
    } 
    
    private void setupContents()
    {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		setContentView(layout);
		
		TextView text = new TextView(this);
		text.setTextSize(16);
		text.setText("Setup contents now...");
		text.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		layout.addView(text);
		
		ProgressBar progress1 = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
		progress1.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		layout.addView(progress1);
		
		mProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
		mProgress.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		layout.addView(mProgress);

		
		new Thread
		(
			new Runnable()
			{
				int progressStatus = 0;
				public void run()
				{
					AssetManager as = getAssets();
					try
					{
						String[] list = as.list("");
						long[] fileSize = new long[list.length];
						long totalFileSize = 0;
						int fileCount = 0;
						for(int i=0; i<list.length; i++)
						{
							try
							{
								fileCount++;
								InputStream in = as.open(list[i]);
								in.close();
								fileSize[i] = as.openFd(list[i]).getLength();
							}
							catch(java.io.FileNotFoundException e)
							{
								fileSize[i] = 0;
							}
							totalFileSize+= fileSize[i];
						}
						
	    	    		int writtenFileCount = 0;
	    	    		long totalWrittenSize = 0;
	    	    		for(int i=0; i<list.length; i++)
	    	    		{
	    	    			//try { Thread.sleep(1000); } catch (InterruptedException e) { }
	    	    			
	    	    			InputStream in;
	    	    			try
	    	    			{
	    	    				in = as.open(list[i]);
	    	    			}
	    	    			catch(java.io.FileNotFoundException e)
	    	    			{
	    	    				continue;
	    	    			}
	    	    			
	    	    	    	String dstPath = "/data/data/" + getPackageName() + "/" + list[i];
	    	    			File outFile = new File(dstPath);
	    	    			FileOutputStream fout = new FileOutputStream(outFile);
	    	    			byte[] buf = new byte[100000];
	    	    			int n;
	    	    			long writtenSize = 0;
	    	    			
	    	    			do
	    	    			{
	    	    				n = in.read(buf);
	    	    				if(n > -1) fout.write(buf, 0, n);
	    	    				writtenSize+= n;
	    	    				if(writtenSize > fileSize[i])
	    	    				{
	    	    					totalFileSize+= (writtenSize - fileSize[i]);
	    	    					fileSize[i] = writtenSize;
	    	    				}
	    	    				float progress = (float)(writtenSize + totalWrittenSize)/totalFileSize;
	    	    				progressStatus = (int)(100*progress);
	    	    				mHandler.post
	    	    				(
		    	    				new Runnable()
		    	    				{
		    	    					public void run()
		    	    					{
		    	    						mProgress.setProgress(progressStatus);
		    	    					}
		    	    				}
	    	    				);
	    	    				
	    	    				try
	    	    				{
	    	    					Thread.sleep(0);
	    	    				}
	    	    				catch(InterruptedException e)
	    	    				{
	    	    					return;
	    	    				}
	    	    			}
	    	    			while(n > -1);
	    	    			
	    	    			fout.close();
	    	    			in.close();
	    	    			writtenFileCount++;
	    	    			totalWrittenSize+= writtenSize;
	    	    		}
	    	    	}
					catch(java.io.IOException e)
					{
	    	    		e.printStackTrace();
	    	    		return;
	    	    	}
					
	    			mHandler.post( new Runnable() { public void run() {setupMatrixEngine();} } );
	    		}
	    	}
		).start();
    }
}
