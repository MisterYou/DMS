package jin.you.protect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jin.you.dd.LogData;

public class IOUtil {
	/**
	 * 从给定文件中读取每一行字符串(配对日志)
	 * 并保存至一个集合中返回
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static List<String> loadLogRec(File file) throws Exception{
		List<String> list = new ArrayList<String>();
		BufferedReader br = null;
		try {
			String str =null;
			while((str=br.readLine())!=null){
				list.add(str);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			if(br!=null){
				br.close();
			}
		}
		
	} 
	/**
	 * 从指定文件中读取配对日志并保存至集合中
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static List<LogData> loadLogData(File file) throws Exception{
		List<LogData> list = new ArrayList<LogData>();
		BufferedReader br = null;
		try {
			br=  new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = null;
			while((line = br.readLine())!=null){
				LogData l = new LogData(line);
				list.add(l);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			if(br!=null){
				br.close();
			}
		}
		return list;
				
	}
	
	/**
	 * 读取标签文件中的信息 
	 */
	public static long readLong(File file) throws Exception{

		BufferedReader br =null;
		
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			long position = Long.parseLong(br.readLine());
			return position;		
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			if(br!=null)
				br.close();
		}
		
	}
	/**
	 * 从日志文件中读取一个字符串
	 * @param raf
	 * @param len
	 * @return 长度为len的字符串
	 */
	public static String readString(RandomAccessFile raf,int len) throws Exception{
		
		try {
			byte[] b = new byte[len];
			raf.read(b);
			String str = new String(b,"ISO8859-1");
			return str;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
	}

	/**
	 * 将指定long值保存至指定文件中
	 * @param lon
	 * @param file
	 */
	public static void saveLong(long lon,File file) throws Exception{
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)));
			pw.println(lon);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			if(pw!=null)
				pw.close();
		}
	}
	/**
	 * 将集合中每个元素的toString方法返回的字符串写入文件中
	 * @param c
	 * @param file
	 */
	public static void saveCollections(Collection c,File file) throws Exception{
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)));
			for(Object o:c){
				pw.println(o);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			if(pw!=null){
				pw.close();
			}
		}
	}


}
