package jin.you.protect;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jin.you.dd.LogData;
import jin.you.dd.LogRec;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.omg.PortableInterceptor.USER_EXCEPTION;

public class DMSClient {
	//unix系统日志文件
	private File logFile;
	//保存解析日志后第文件
	private File textlogFile;
	//保存每次解析日志第条目数
	private int batch;
	//保存书签文件名
	private File lastPositionFile;
	//保存配对日志文件
	private File logrecFile;
	//未配对日志文件
	private File loginlogFile;
	//服务端地址
	private String servserHost; 
	//端口号
	private int serverport;
	
	public DMSClient() throws Exception{
		
		try {
			Map<String, String> config = loadConfig();
			System.out.println(config);
			init(config);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("初始化出错");
		}
	}
	private void init(Map<String, String> config) throws Exception{
		try {
			this.logFile = new File(config.get("logFile"));
			this.textlogFile = new File(config.get("textlogFile"));
			this.batch = Integer.parseInt(config.get("batch"));
			this.lastPositionFile = new File(config.get("lastpositionfile"));
			this.logrecFile = new File(config.get("logrecfile"));
			this.loginlogFile = new File(config.get("loginlogfile"));
			this.servserHost = config.get("serverhost");
			this.serverport = Integer.parseInt(config.get("serverport"));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("文件打开出错");
			throw e;
		}
	
		
	}
	/**
	 * 
	 * 读取配置文件，并将文件存入一个集合中保存
	 * @return 返回值中key时标签名字，value是标签中第文本
	 * @throws Exception
	 */
	public Map<String, String> loadConfig() throws Exception{
		try {
			SAXReader reader = new SAXReader();
			Document doc = reader.read(new File("config.xml"));
			Map<String, String> map = new HashMap<String, String>();
			Element root = doc.getRootElement();
			List<Element> list = root.elements();
			for(Element ele:list){
				String name = ele.getName();
				String text = ele.getTextTrim();
				map.put(name, text);
			}
			return map;
			
		
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("config文件解析出错");
			throw e;
		}
		
	}	
	
	public void start(){
		
	}
	/**
	 * 发送日志
	 * @return 发送成功或者失败
	 */
	public boolean sendLog(){
		Socket socket=null;
		PrintWriter pw = null;
		BufferedReader br = null;
		if(!logrecFile.exists()){
			return false;
		}
		try {
			List<String> match = IOUtil.loadLogRec(logrecFile);
			socket = new Socket(servserHost,serverport);
			pw= new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);
			for(String sendStr:match){
				pw.println(sendStr);
			}
			pw.println("OVER");
			br=new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
			String in = br.readLine();
			if("OK".equals(in)){
				logrecFile.delete();
				return true;
			}else{
				System.out.println("服务器接收失败");
				return false;
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			System.out.println("发送日志失败");
		}finally{
			
				try {
					if(socket!=null){
						socket.close();
					}
					if(pw!=null)
						pw.close();
					if(br!=null)
						br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
		}
		
		return false;
	}
	/**
	 * 配对日志
	 * @return 配对成功或者失败
	 */
	
	public boolean matchLog(){
		if(logrecFile.exists()){
			return true;
		}
		if(!textlogFile.exists()){
			return false;
		}
		try {
			List<LogData> list = IOUtil.loadLogData(textlogFile);
			if(loginlogFile.exists()){
				list.addAll(IOUtil.loadLogData(loginlogFile));
			}
			List<LogRec> logrev = new ArrayList<LogRec>();
			Map<String, LogData> inMap = new HashMap<String, LogData>();
			Map<String, LogData> outMap = new HashMap<String, LogData>();
			
			for(LogData l:list){
				if(l.getType()==LogData.TYPE_LOGIN){
					inMap.put(l.getUser()+","+l.getPid(), l);
				}else if(l.getType() == LogData.TYPE_LOGOUT){
					outMap.put(l.getUser()+","+l.getPid(), l);
				}
			}
			Set<Entry<String, LogData>> entrySet = outMap.entrySet();
			for(Entry<String, LogData> en:entrySet){
				LogData ld = inMap.get(en.getKey());
				LogRec lr = new LogRec(ld,en.getValue());
				inMap.remove(en.getKey());
				logrev.add(lr);
			}
			IOUtil.saveCollections(logrev, logrecFile);
			IOUtil.saveCollections(inMap.values(), loginlogFile);
			
			textlogFile.delete();
			return true;
			
		} catch (Exception e) {
			System.out.println("配对失败");
			e.printStackTrace();
		}
		
		
		return false;
	}
	
	/**
	 * 解析日志
	 * @return 解析成功或者失败
	 */
	private boolean parseLogs(){
		if(textlogFile.exists()){
			return true;
		}
		if(!logFile.exists()){
			System.out.println(logFile+"不存在");
			return false;
		}
		long lastPosition = hasLogs();
		if(lastPosition<0){
			System.out.println("没有文件可以读取");
			return false;
		}
		RandomAccessFile raf = null;
		
		try {
			raf = new RandomAccessFile(logFile, "r");
			raf.seek(lastPosition);
			List<LogData> list = new ArrayList<LogData>();
			for(int i=0;i<batch;i++){
				if((logFile.length()-lastPosition)<LogData.LOG_LENGTH){
					break;
				}
				raf.seek(lastPosition+LogData.USER_OFFSET);
				String name = IOUtil.readString(raf, LogData.USER_LENGTH);
				
				raf.seek(lastPosition+LogData.PID_OFFSET);
				int pid = raf.readInt();
				
				raf.seek(lastPosition+LogData.TYPE_OFFSET);
				short type = raf.readShort();
				
				raf.seek(lastPosition+LogData.TIME_OFFSET);
				int time = raf.readInt();
				
				raf.seek(lastPosition+LogData.HOST_OFFSET);
				String host = IOUtil.readString(raf, LogData.HOST_LENGTH);
				
				LogData ld = new LogData(name, pid, type, time, host);
				list.add(ld);
				lastPosition = raf.getFilePointer();
			}
			IOUtil.saveCollections(list, textlogFile);
			IOUtil.saveLong(lastPosition, lastPositionFile);
			return true;
			
			
			
		} catch (Exception e) {
			System.out.println("解析日志失败");
		}finally{
			if(raf!=null){
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
			
		
		return false;
	}
	
	/**
	 * 根据书签中记录第位置判断是否还有日志可以解析
	 * ，若有，则将上次最后第位置返回
	 * @return
	 */
	private long hasLogs(){
		try {
			if(!lastPositionFile.exists()){
				return 0;
			}
			long lastPosition = IOUtil.readLong(lastPositionFile);
			if(logFile.length()-lastPosition>LogData.LOG_LENGTH){
				return lastPosition;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("读取标签文件出错");
		}
		
		return -1;
	}
	
	public static void main(String[] args) {
		try {
			DMSClient client = new DMSClient();
			client.start();
		} catch (Exception e) {
			
			e.printStackTrace();
			System.out.println("客户端运行失败");
		}
		
	}
	
}
