package com.mgrid.main;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

public class ContainerView extends ViewGroup {

	public ContainerView(Context context) {
		super(context);
	}

	// 解决 VIEWGROUP 中子VIEW布局混乱问题
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

	    if(mCurrentView != null)
	    {
	    	mCurrentView.layout(l, t, r, b);
	    }
	}
	
	protected void dispatchDraw(Canvas canvas)    
	{
	    super.dispatchDraw(canvas);
	    
	    if(mCurrentView != null)
	    {
	        drawChild(canvas, mCurrentView, getDrawingTime());
	    }
	}

	// 解决多重 ViewGroup 嵌套事件响应问题。 TODO: 有无其他影响尚待观察  -- CharlesChen
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		/*
		int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
		int measureHeigth = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(measureWidth, measureHeigth);
		// TODO Auto-generated method stub
		for (int i = 0; i < getChildCount(); i++)
		{
			View v = getChildAt(i);
			// Log.v(TAG, "measureWidth is " +v.getMeasuredWidth() +
			// "measureHeight is " +v.getMeasuredHeight());
			int widthSpec = 0;
			int heightSpec = 0;
			LayoutParams params = v.getLayoutParams();
			if (params.width > 0)
			{
				widthSpec = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
			} else if (params.width == -1)
			{
				widthSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.EXACTLY);
			} else if (params.width == -2)
			{
				widthSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.AT_MOST);
			}

			if (params.height > 0)
			{
				heightSpec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
			} else if (params.height == -1)
			{
				heightSpec = MeasureSpec.makeMeasureSpec(measureHeigth, MeasureSpec.EXACTLY);
			} else if (params.height == -2)
			{
				heightSpec = MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.AT_MOST);
			}
			v.measure(widthSpec, heightSpec);

		}
		*/
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
	    if(mCurrentView != null)
	    {
	    	mCurrentView.measure(widthMeasureSpec, heightMeasureSpec);
	    }
		
		// 改善性能，不处理非显示页面。
		/*
		final int count = getChildCount();
		for (int i = 0; i < count; i++)
		{
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		//*/
	}

	public View mCurrentView;
}
