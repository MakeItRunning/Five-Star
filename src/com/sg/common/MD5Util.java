package com.sg.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MD5Util
{
	/**
	 * ��ȡ�ļ�MD5�룬�����ļ���������
	 * 
	 * @param in FileInputStream����
	 * @return MD5���ַ���
	 * @throws 
	 */
	public static String getMd5ByFileInputStream(FileInputStream in)
	{
		String value = null;
		
		try
		{
			MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, in.available());
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(byteBuffer);
			BigInteger bi = new BigInteger(1, md5.digest());
			value = bi.toString(16);
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
		}

		return value;
	}

	/**
	 * ��ȡ�ļ�MD5�룬�����ļ�����
	 * 
	 * @param file File����
	 * @return MD5���ַ���
	 * @throws FileNotFoundException
	 */
	public static String getMd5ByFile(File file) throws FileNotFoundException
	{
		String value = null;
		FileInputStream in = new FileInputStream(file);

		try
		{
			MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(byteBuffer);
			BigInteger bi = new BigInteger(1, md5.digest());
			value = bi.toString(16);
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			if (null != in)
			{
				try
				{
					in.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return value;
	}

	/**
	 * ��ȡ�ļ�MD5�룬�����ļ�·����
	 * 
	 * @param filepath �ļ�������·����
	 * @return MD5���ַ���
	 * @throws FileNotFoundException
	 */
	public static String getMD5ByFilepath(String filepath) throws FileNotFoundException
	{
		String rtnvel = null;

		if (filepath == null || filepath.isEmpty())
			return rtnvel;

		File file = new File(filepath);

		rtnvel = getMd5ByFile(file);

		return rtnvel;
	}

	/*
	// ��ȡ�ļ�MD5�룬�����ļ�·���� 
	// use Apache Commons Codec.
	public static String getMD5DG(String filepath)
	{
		FileInputStream fis = new FileInputStream(filepath);
		String md5 = DigestUtils.md5Hex(IOUtils.toByteArray(fis));
		IOUtils.closeQuietly(fis);

		return md5;
	}
	*/

	/*
	// test
	public static void main(String[] args) throws IOException
	{
		long begin = System.currentTimeMillis();
		File big = new File("E:\\bigfile.rar");
		String md5 = getFileMD5String(big);
		// String md5 = getFileMD5String_deprecated(big);
		long end = System.currentTimeMillis();
		System.out.println("md5:" + md5 + " time:" + ((end - begin) / 1000) + "s");
	}
	*/

	/**
	 * ��������G����ļ�
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String getFileMD5String(File file) throws IOException
	{
		FileInputStream in = new FileInputStream(file);
		byte[] buffer = new byte[1024 * 1024 * 10];
		
		int len = 0;
		while ((len = in.read(buffer)) > 0)
		{
			messagedigest.update(buffer, 0, len);
		}
		
		in.close();
		return bufferToHex(messagedigest.digest());
	}

	/**
	 * ̫����ļ��ᵼ���ڴ����
	 * 
	 * @deprecated
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String getFileMD5String_deprecated(File file) throws IOException
	{
		FileInputStream in = new FileInputStream(file);
		FileChannel ch = in.getChannel();
		MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		messagedigest.update(byteBuffer);
		
		in.close();
		return bufferToHex(messagedigest.digest());
	}

	public static String getMD5String(String s)
	{
		return getMD5String(s.getBytes());
	}

	public static String getMD5String(byte[] bytes)
	{
		messagedigest.update(bytes);
		return bufferToHex(messagedigest.digest());
	}

	private static String bufferToHex(byte bytes[])
	{
		return bufferToHex(bytes, 0, bytes.length);
	}

	private static String bufferToHex(byte bytes[], int m, int n)
	{
		StringBuffer stringbuffer = new StringBuffer(2 * n);
		int k = m + n;
		for (int l = m; l < k; l++)
		{
			appendHexPair(bytes[l], stringbuffer);
		}
		return stringbuffer.toString();
	}

	private static void appendHexPair(byte bt, StringBuffer stringbuffer)
	{
		char c0 = hexDigits[(bt & 0xf0) >> 4];
		char c1 = hexDigits[bt & 0xf];
		stringbuffer.append(c0);
		stringbuffer.append(c1);
	}

	public static boolean checkPassword(String password, String md5PwdStr)
	{
		String s = getMD5String(password);
		return s.equals(md5PwdStr);
	}

	/**
	 * Ĭ�ϵ������ַ�����ϣ�apacheУ�����ص��ļ�����ȷ���õľ���Ĭ�ϵ�������
	 */
	protected static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	protected static MessageDigest messagedigest = null;
	static
	{
		try
		{
			messagedigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsaex)
		{
			System.err.println(MD5Util.class.getName() + "��ʼ��ʧ�ܣ�MessageDigest��֧��MD5Util��");
			nsaex.printStackTrace();
		}
	}

}
