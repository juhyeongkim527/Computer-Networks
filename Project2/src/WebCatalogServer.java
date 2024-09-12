import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WebCatalogServer {
	// 쿠키 설정을 위해 cookieName = "StudentNumber"로 설정하고 cookieValue에 넣을 값을 "studentNumber" 변수의 값으 설정하였습니다.
	private static final String cookieName = "StudentNumber"; 
	private static final String studentNumber = "2021093518";
	// 로컬 웹서버의 포트 번호를 8080으로 설정하였습니다.
	private static final int portNumber = 8080;
	// furniture.json 파일에서 furniture의 정보를 가져오기 위해 JSONObject Class를 사용하여 furnitureData 객체를 생성하였습니다.
	private static JSONObject furnitureData;
	
	// main method에서는 웹 서버를 시작하고 클라이언트의 연결 요청을 기다립니다.
	public static void main(String[] args) throws Exception {
		System.out.println("Listening on port: " + portNumber);
		// JSON 파일을 파싱하기 JSON parser 객체를 생성하였습니다.
		JSONParser parser = new JSONParser();
		// furniture.json 파일을 읽어와서,
		Reader reader = new FileReader("furniture.json");
		// JSONObject로 변환하였습니다.
		furnitureData = (JSONObject) parser.parse(reader);
		// 위에서 설정한 portNumber로 ServerSocket 생성하였습니다.
		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
			while (true) {
				// 클라이언트의 연결 요청을 기다립니다.
				Socket clientSocket = serverSocket.accept();
				// 새로운 클라이언트에 대해 새로운 Thread를 생성합니다.
				Thread clientThread = new Thread(new ClientHandler(clientSocket));
				// Thread를 시작합니다.
				clientThread.start();
			}
		}
	}
	
	// 클라이언트의 요청을 처리하는 데 사용되는 Runnable 클래스입니다.
	static class ClientHandler implements Runnable {
		// 클라이언트와 통신을 위해 Socket을 저장할 clientSocket 변수를 생성하였습니다.
		private final Socket clientSocket;

		// 생성자를 통해 clientSocket을 설정합니다.
		public ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		// Runnable 인터페이스의 메서드로, 클라이언트 요청을 처리하는 코드가 포함되어 있습니다.
		public void run() {
			try {
				// 클라이언트의 요청을 처리하는 부분입니다.
				handleRequest(clientSocket);
			} catch (IOException e) {
				// Exception 발생 시 오류 내용을 출력합니다.
				e.printStackTrace();
			}
		}
	}
	// 클라이언트의 요청을 처리하는 메서드입니다. 요청의 종류에 따라 정해 처리를 수행합니다.
	static void handleRequest(Socket clientSocket) throws IOException {
		// 클라이언트로부터 데이터를 읽기 위한 bufferedReader 변수 in을 생성하였습니다.
		BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		// 클라이언트에게 데이터를 보내기 위한 OutputStream 변수 out을 생성하였습니다.
		OutputStream out = clientSocket.getOutputStream();

		// HTTP 요청을 저장할 StringBuilder 변수 request를 생성하였습니다.
		StringBuilder request = new StringBuilder();
		String line;
		// 쿠키를 생성하고 확인하기 위한 cookieValue 변수입니다.
		String cookieValue = null;
		// 클라이언트로부터 데이터를 한 줄씩 읽어서 request에 추가합니다.
		while ((line = in.readLine()) != null && !line.isEmpty()) {
			request.append(line).append("\r\n");
			// 입력 줄에 "Cookie:" 가 존재한다면 cookieValue 값에 "Cookie:" 뒷 부분부터 쿠키 정보 추출합니다. 
			if (line.startsWith("Cookie:")) {
				cookieValue = line.substring(8).trim();
			}
		}
		// HTTP 요청을 공백 문자로 분리하여 배열에 저장합니다.
		String[] requestLines = request.toString().split("\\s");
		// 요청 경로를 가져옵니다.
		String path = requestLines[1];
		// 만약 요청 경로가 "/" 또는 "/index.html"인 경우 index.html 페이지 요청을 처리합니다.
		if (path.equals("/") || path.equals("/index.html")) {
			handleIndexRequest(out, cookieValue);
		} 
		// 만약 요청 경로가 "/chair", "/closet", "/table" 중 하나라면, detail.html 페이지 요청을 처리합니다.
		else if (path.equals("/chair") || path.equals("/closet") || path.equals("/table")) {
			// name으로 /을 제외하고 넘겨주기 위해 substring(1) 코드를 작성하였습니다.
			handleDetailRequest(out, path.substring(1));
		} 
		// 만약 요청 경로가 "/images/"로 시작한다면, 이미지 요청을 처리합니다.
		else if (path.startsWith("/images/")) {
			// /images/ 뒤 부터 가져오기 위해 substring(8) 코드를 작성하였습니다.
			handleImageRequest(out, path.substring(8));
		} 
		// else의 경우 404 응답을 보냅니다.
		else {
			String response = "HTTP/1.1 404 Not Found\r\n\r\n";
			out.write(response.getBytes());
		}
		// BufferedReader를 닫습니다.
		in.close();
		// OutputStream을 닫습니다.
		out.close();
		// 클라이언트 소켓을 닫습니다.
		clientSocket.close();
	}
	
	// 클라이언트의 인덱스 페이지 요청을 처리하는 메서드입니다. 쿠키를 설정하고 확인하는 것도 위 메서드에서 담당합니다.
	static void handleIndexRequest(OutputStream out, String cookieValue) throws IOException {
		System.out.println("Index page requested");
		//cookieName과 studentNumber로 쿠키 헤더를 설정합니다.
		String cookieHeader = "Set-Cookie: " + cookieName + "=" + studentNumber + "\r\n";
		// HTTP 응답을 보내고 쿠키 헤더를 전송합니다.
		out.write(("HTTP/1.1 200 OK\r\n" + cookieHeader + "Content-Type: text/html\r\n\r\n").getBytes());
		
		// 쿠키 값이 존재하고, 미리 설정해 둔 StudentNumber=2021093518이 쿠키에 포함되어 있으면 쿠키가 존재하는 것으로 판단합니다.
		if (cookieValue != null && cookieValue.contains(cookieName + "=" + studentNumber)) {
			System.out.println("Returning user, welcome " + studentNumber);
		} 
		// 아닌 경우 새로운 사용자라는 것을 나타내고 쿠키가 set 되었다고 출력합니다.
		else {
			System.out.println("New user requested page, cookie will be set.");
		}
		// index.html 파일을 읽어와서 응답으로 보내는 코드입니다.
		File file = new File("index.html").getCanonicalFile();

		// 파일이 없으면 404(Not Found) 에러가 보이도록 설정하였습니다.
		if (!file.isFile()) {
			String response = "HTTP/1.1 404 Not Found\r\n\r\n";
			out.write(response.getBytes());
		}
		// 파일이 존재하면 200 응답과 함께 파일 내용을 응답으로 보내도록 하였습니다.
		else {
			FileInputStream fs = new FileInputStream(file);
			final byte[] buffer = new byte[0x10000];
			int count = 0;
			while ((count = fs.read(buffer)) >= 0) {
				out.write(buffer, 0, count);
			}
			fs.close();
		}
	}

	// 클라이언트의 상세 페이지 요청을 처리하는 메서드입니다. 가구 이름에 따른 상세 정보를 보여줍니다.
	static void handleDetailRequest(OutputStream out, String furnitureName) throws IOException {
		// furniture.json 파일의 Name 필드에 각 furniture 이이 대문자로 시작되어 있어서, detail.html 경로와 맞추기 위해 첫 문자를 소문자로 변경하였습니다.
		furnitureName = Character.toUpperCase(furnitureName.charAt(0)) + furnitureName.substring(1);
		// furniture 데이터가 담긴 JSON 배열을 가져오는 코드입니다.
		JSONArray furnitureArray = (JSONArray) furnitureData.get("Furniture");
		JSONObject furniture = null;
		// JSON 배열에 어떤 항목이 있는지 찾아서 furniture에 저장하는 코드입니다.
		for (Object obj : furnitureArray) {
			JSONObject furnitureObj = (JSONObject) obj;
			if (furnitureObj.get("Name").equals(furnitureName)) {
				furniture = furnitureObj;
				break;
			}
		}
		// furniture 항목을 찾지 못했다면 404 에러를 발생시킵니다.
		if (furniture == null) {
			String response = "HTTP/1.1 404 Not Found\r\n\r\n";
			out.write(response.getBytes());
		} else {
			System.out.println(furnitureName + "'s detail page requested");
			// detail.html 파일을 읽어와서 템플릿 내의 플레이스홀더를 각 furniture.json 파일에서 가져온 정보로 대체하는 코드입니다.
			String detailHtml = new String(Files.readAllBytes(Paths.get("detail.html")), StandardCharsets.UTF_8);
			detailHtml = detailHtml.replace("OBJECT TITLE", (String) furniture.get("Name"));
			detailHtml = detailHtml.replace("OBJECT PRICE", (String) furniture.get("Price"));
			detailHtml = detailHtml.replace("OBJECT DESCRIPTION", (String) furniture.get("Description"));
			String imagePath = ((String) furniture.get("ImageLocation")).substring(2);
			// ImageHandler로 처리하기 위해 html 파일의 src에 /images/를 추가합니다.
			detailHtml = detailHtml.replace("src=\"\"", "src=\"/images/" + imagePath + "\"");

			// 정상적으로 처리되었다는 HTTP 응답을 전송합니다.
			out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + detailHtml).getBytes());
		}
	}

	// html 보안 때문인지, 이미지 업로드가 안되는 오류가 발생하여 이미지를 정상적으로 로딩하기 위한 method를 작성하였습니다.
	static void handleImageRequest(OutputStream out, String imagePath) throws IOException {
		// 저장된 이미지 경로에서 이미지 데이터를 로드합니다.
		byte[] imageData = loadImageData(imagePath);

		if (imageData != null) {
			// 이미지 파일 확장자(.png)에 기반하여 콘텐츠 타입을 입력하였습니다.
			String contentType = "image/png";

			// contentType과 함께 응답 헤더를 전송합니다.
			out.write(("HTTP/1.1 200 OK\r\nContent-Type: " + contentType + "\r\n\r\n").getBytes());
			// 이미지 데이터를 응답으로 전송합니다.
			out.write(imageData);
		} 
		// 이미지 데이터를 찾지 못했을 경우 404 에러를 반환합니다.
		else {
			String response = "HTTP/1.1 404 Not Found\r\n\r\n";
			out.write(response.getBytes());
		}
	}

	// furniture.json 파일에서 Name으로 가구 detail.html 페이지를 보내기 위한 helping method 입니다.
	static JSONObject getFurnitureByName(String furnitureName) {
		// furnitureData에서 "Furniture"라는 키를 가진 데이터를 JSONArray 형태로 가져옵니다.
		JSONArray furnitureArray = (JSONArray) furnitureData.get("Furniture");
		for (Object obj : furnitureArray) {
			// 배열의 각 요소를 JSONObject로 변환합니다.
			JSONObject furnitureObj = (JSONObject) obj;
			// 만약 현재 JSONObject의 "Name" 속성이 입력받은 furnitureName과 같다면, 해당 JSONObject를 반환합니다.
			if (furnitureObj.get("Name").equals(furnitureName)) {
				return furnitureObj;
			}
		}
		// 만일 배열을 모두 순회했음에도 해당 이름의 가구를 찾지 못했다면 null을 반환합니다.
		return null;
	}

	// handleImageRequest method에서 이미지 데이터를 로드하기 위한 helping metho입니다.
	static byte[] loadImageData(String imagePath) {
		try {
			// 주어진 imagePath로부터 모든 바이트를 읽어들여, 바이트 배열로 반환합니다.
			return Files.readAllBytes(Paths.get(imagePath));
		} 
		// 파일 읽기 과정에서 IOException이 발생하면, 에러 스택을 출력하고 null을 반환합니다.
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
