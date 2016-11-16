package jin.you.protect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


















import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import jin.you.dd.LogRec;

public class DMSServer {
	private ServerSocket server;
	private ExecutorService threadPool;
	private File serverLogFile;

	private BlockingQueue< String> queue = new LinkedBlockingQueue<String>();

	public DMSServer() throws Exception{
		try {
			System.out.println("系统正在初始化。。");
			Map<String, String> config = loadConfig();
			init(config);
			System.out.println("系统初始化完毕");

		} catch (Exception e) {
			System.out.println("初始化失败");
			e.printStackTrace();
			throw e;
		}


	}
	/**
	 * 读取服务器配置文件	
	 * @return服务器配置文件中各配置信息
	 * @throws Exception
	 */
	private static Map<String, String> loadConfig() throws Exception{
		try {
			SAXReader reader = new SAXReader();
			Map<String, String> map = new HashMap<String, String>();
			Document doc = reader.read(new File("server-config.xml")); 
			Element root = doc.getRootElement();
			List<Element> list = root.elements();
			for(Element ele:list){
				String key = ele.getName();
				String value = ele.getTextTrim();
				map.put(key, value);
			}
			return map;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("服务器配置文件加载失败");
			throw e;
		}
	}
	/**
	 * 初始化
	 * @param map
	 * @throws Exception
	 */
	private void init(Map<String, String> map)throws Exception{
		this.server = new ServerSocket(Integer.parseInt(map.get("serverport")));
		this.serverLogFile = new File(map.get("logrecfile"));
		this.threadPool = Executors.newFixedThreadPool(Integer.parseInt(map.get("threadsum")));

	}

	class SaveLogrec implements Runnable{

		public void run() {
			PrintWriter pw = null;
			
			try {
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(serverLogFile)),true);
				while(true){
					if(queue.size()>0){
						pw.println(queue.poll());
					}else{
						pw.flush();
						Thread.sleep(500);
					}
				}
			} catch (FileNotFoundException e) {
		
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			

		}

	}
	public void start() throws Exception{
		try {
			SaveLogrec sl = new SaveLogrec();
			new Thread(sl).start();
			while(true){
				Socket socket= server.accept();
				ClientThread ct = new ClientThread(socket);
				threadPool.execute(ct);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		
	}
	
	/**
	 * 负责接收一个客户端发送的数据，并将其键入队列中
	 * @author jin
	 *
	 */
	class ClientThread implements Runnable{
		Socket socket =null;
		public ClientThread(Socket socket) {
			this.socket = socket;
		}
		public void run() {
			PrintWriter pw= null;
			BufferedReader br = null;
			try {
				pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);
				br = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));

				String str = null;
				while((str=br.readLine())!=null){
					if("OVER".equals(str)){
						break;
					}else{
						queue.offer(str);
					}
				}
				pw.println("OK");

			} catch (UnsupportedEncodingException e) {

				e.printStackTrace();
				pw.println("Error");

			} catch (IOException e) {

				e.printStackTrace();
				pw.println("Error");
			}finally{
				try {
					if(socket!=null){
						socket.close();
					}
					if(pw!=null){
						pw.close();
					}if(br!=null){
						br.close();
					}
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		}

	}

	public static void main(String[] args) {
		DMSServer dms;
		try {
			dms = new DMSServer();
			dms.start();
		} catch (Exception e) {
			System.out.println("服务端启动出错");
			e.printStackTrace();
		}
		
	}

}
