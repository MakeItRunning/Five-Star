package com.sg.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.util.Log;

public class CFGTLS
{
	public static double getPadHeight(double objheight, double formheight, float fontsize)
	{
		double scheight = 768;
		double rate = scheight/formheight;
		return objheight*rate-CFGTLS.getFontHeight(fontsize);
	}
	
	public static double getFontHeight(float fontSize)
	{
		PAINT.setTextSize(fontSize);
	    FontMetrics fm = PAINT.getFontMetrics();
	    
	    //return (int) Math.ceil(fm.descent - fm.ascent);
	    return Math.ceil(fm.descent - fm.top) + 2;
	}
	
	public static int parseColor(String strValue)
	{
/*
	 // ��������������ɫ
   	 if ("BLACK".equalsIgnoreCase(strValue))
   	 {
   		return Color.BLACK;
   	 }
   	 else if ("BLUE".equalsIgnoreCase(strValue))
   	 {
   		return Color.BLUE;
   	 }
   	 else if ("CYAN".equalsIgnoreCase(strValue))
   	 {
   		return Color.CYAN;
   	 }
   	 else if ("DKGRAY".equalsIgnoreCase(strValue))
   	 {
   		return Color.DKGRAY;
   	 }
   	 else if ("GRAY".equalsIgnoreCase(strValue))
   	 {
   		return Color.GRAY;
   	 }
   	 else if ("GREEN".equalsIgnoreCase(strValue))
   	 {
   		return Color.GREEN;
   	 }
   	 else if ("LTGRAY".equalsIgnoreCase(strValue))
   	 {
   		return Color.LTGRAY;
   	 }
   	 else if ("MAGENTA".equalsIgnoreCase(strValue))
   	 {
   		return Color.MAGENTA;
   	 }
   	 else if ("RED".equalsIgnoreCase(strValue))
   	 {
   		return Color.RED;
   	 }
   	 else if ("TRANSPARENT".equalsIgnoreCase(strValue))
   	 {
   		return Color.TRANSPARENT;
   	 }
   	 else if ("WHITE".equalsIgnoreCase(strValue))
   	 {
   		return Color.WHITE;
   	 }
   	 else if ("YELLOW".equalsIgnoreCase(strValue))
   	 {
   		 return  Color.YELLOW;
   	 }
*/
   	 
   	    // ���� AA,RR,GG,BB ��ʽ
		if (strValue.contains(","))
		{
		String str[] = strValue.split(",");
		int array[] = new int[str.length];

		for (int i = 0; i < str.length; i++)
			array[i] = Integer.parseInt(str[i].trim());

		// �ж��Ƿ���͸����
		if (3 == str.length)
			return Color.rgb(array[0], array[1], array[2]);
		else if (3 < str.length)
			return Color.argb(array[0], array[1], array[2], array[3]);
		}

		/*
   	  // ���� FF000000 ��ʽ
	  if (strValue.length() == 8) return Color.parseColor("#"+strValue);
	  // ���� ##FF000000 ��ʽ
	  else if(strValue.length() == 10)
	  {
		  String str1 = strValue.substring(1);
		  return Color.parseColor(str1);
	  }
	  */
		
	  // ����ֱ�ӽ�����
	  return Color.parseColor(strValue);
	}
	
	/**
	 * ��ȡͼƬ���� ����MD5�롣
	 * 
	 * @param md5 MD5���ַ���
	 * @return �ɹ�����BitMap�����򷵻�null��
	 * @throws 
	 */
	public static Bitmap getBitmap(String md5)
	{
		if (null == md5 || md5.isEmpty()) return null;
		
		return ht_images.get(md5);
	}
	
	/**
	 * ��ȡͼƬ���� �����ļ�·��������Դ������ʱ����������Դ��
	 * 
	 * @param filename �ļ�������·����
	 * @return �ɹ�����BitMap�����򷵻�null��
	 * @throws FileNotFoundException
	 */
	public static Bitmap getBitmapByPath(String filename) throws FileNotFoundException
	{
		String md5 = MD5Util.getMD5ByFilepath(filename);
		if (null == md5 || md5.isEmpty()) return null;
		
		Bitmap bitmap = ht_images.get(md5);
		
		if (null != bitmap) return bitmap;
		
		// ��ͼƬ�ļ�δ�����ع���������ͼƬ����
		BitmapFactory.Options o = null;
		FileInputStream stream = null;

		if (ZOOMOUT || ZOOMIN)
		{
			stream = new FileInputStream(filename);

			// ����ͼƬ�ߴ磬������METADATAԪ������
			o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			
			BitmapFactory.decodeStream(stream, null, o);
			
			try
			{
				stream.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}

			// ��ȡԭʼͼƬ�ߴ�
			int inWidth = o.outWidth;
			int inHeight = o.outHeight;
			Log.v("Warnning", String.format("Original bitmap size: (%dx%d).", inWidth, inHeight));

			// ��ȡԤ����ͼƬ�ߴ�
			int targetWidth = 1024;
			int targetHeight = 768;
			o = new BitmapFactory.Options();
			o.inSampleSize = Math.max(inWidth / targetWidth, inHeight / targetHeight);
			
			// ϵͳ�ڴ治��ʱ�ɻ���pixels
			o.inPurgeable = true;
			
			// ����Ϊ���������Ҫ(���˴��ر�)��
			o.inInputShareable = true;
			
			if (!BITMAP_HIGHQUALITY)
			{
				// ��ʾ16λλͼ 565�����Ӧ��ԭɫռ��λ��
				o.inPreferredConfig = Bitmap.Config.RGB_565;
			}
			
			// TODO: �ṩ������ʾ������INI���䡣
		}

		// ����Ԥ����ͼƬ
		stream = new FileInputStream(filename);
		bitmap = BitmapFactory.decodeStream(stream, null, o);
		
		try
		{
			stream.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			stream = null;
		}
		
		ht_images.put(md5, bitmap);
		
		Log.v("Warnning", String.format("Pre-sized bitmap size: (%dx%d).", bitmap.getWidth(), bitmap.getHeight()));
		return bitmap;
	} // end of getBitmap
	
    private static Paint PAINT = new Paint();
	private static Hashtable<String, Bitmap> ht_images = new Hashtable<String, Bitmap>();
	
	private static boolean ZOOMOUT = true;
	private static boolean ZOOMIN = false;
	
	public static boolean BITMAP_HIGHQUALITY = false;
}
