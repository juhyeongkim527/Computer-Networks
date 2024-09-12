import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Reader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WebCatalogServer {
	// 쿠키 설정을 위해 cookieName = "StudentNumber"로 설정하고 cookieValue에 넣을 값을 studentNumber
	// 변수로 설정하였습니다.
	private static final String cookieName = "StudentNumber";
	private static final String studentNumber = "2021093518";
	// 로컬 웹서버의 포트 번호를 8080으로 설정하였습니다.
	private static final int portNumber = 8080;
	// furniture.json 파일에서 furniture의 정보를 가져오기 위해 JSONObject Class를 사용하여
	// furnitureData 객체를 생성하였습니다.
	private static JSONObject furnitureData;

	public static void main(String[] args) throws Exception {
		// JSON 파일을 파싱하기 JSON parser 객체를 생성하였습니다.
		JSONParser parser = new JSONParser();
		// furniture.json 파일을 읽어와서,
		Reader reader = new FileReader("furniture.json");
		// JSONObject로 변환하였습니다.
		furnitureData = (JSONObject) parser.parse(reader);
		// 위에서 설정한 portNumber를 활용하여 HTTP 서버를 생성하였습니다.
		HttpServer server = HttpServer.create(new InetSocketAddress(portNumber), 0);
		// html 페이지의 각 요청을 처리하기 위해 IndexHandler와 DetailHandler를 생성하였습니다.
		server.createContext("/", new IndexHandler());
		server.createContext("/chair", new DetailHandler());
		server.createContext("/closet", new DetailHandler());
		server.createContext("/table", new DetailHandler());
		// 웹 브라우저 보안 정책으로 인해 이미지가 정상적으로 표시되지 않는 문제를 해결하기 위해 ImageHandler를 생성하였습니다.
		server.createContext("/images/chair.png", new ImageHandler("chair.png"));
		server.createContext("/images/closet.png", new ImageHandler("closet.png"));
		server.createContext("/images/table.png", new ImageHandler("table.png"));
		// 요청 처리를 위한 실행자를 null으로 설정하여 "/" 경로를 기본 경로로 설정하였습니다.
		server.setExecutor(null);
		// 서버를 시작합니다.
		server.start();
		System.out.println("Listening on port: " + portNumber);
	}

	// index.html에 따라 각 페이지 요청을 처리하는 IndexHandler작성하였습니다.
	static class IndexHandler implements HttpHandler {
		public void handle(HttpExchange exchange) throws IOException {
			System.out.println("Index page requested");

			// 사용자를 구분하기 위해 HttpCookie 클스를 이용하여 cookie 객체를 생성하였습니다.
			// cookieName과 studentNumber를 활용하여 cookie의 Name과 Value를 설정하였습니다.
			HttpCookie cookie = new HttpCookie(cookieName, studentNumber);
			exchange.getResponseHeaders().add("Set-Cookie", cookie.toString());

			// RequestHeader에서 쿠키 값을 읽어와서 새로운 사용자인지, 쿠키 값이 저장된 사용자인지 확인하였습니다.
			String cookieValue = exchange.getRequestHeaders().getFirst("Cookie");
			if (cookieValue != null && cookieValue.contains(cookieName + "=" + "\"" + studentNumber + "\"")) {
				System.out.println("Returning user, welcome " + studentNumber);
			} else {
				System.out.println("New user requested page, cookie will be set.");
			}

			// index.html 파일을 읽어와서 응답으로 보내는 코드입니다.
			File file = new File("index.html").getCanonicalFile();

			// 파일이 없으면 404(Not Found) 에러가 보이도록 설정하였습니다.
			if (!file.isFile()) {
				String response = "404 (Not Found)\n";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
			// 파일이 존재하면 200 응답과 함께 파일 내용을 응답으로 보내도록 하였습니다.
			else {
				exchange.sendResponseHeaders(200, 0);
				OutputStream os = exchange.getResponseBody();
				FileInputStream fs = new FileInputStream(file);
				final byte[] buffer = new byte[0x10000];
				int count = 0;
				while ((count = fs.read(buffer)) >= 0) {
					os.write(buffer, 0, count);
				}
				fs.close();
				os.close();
			}
		}
	}
	
	// 이미지를 정상적으로 반환하기 위해 ImageHandler 클래스를 작하였습니다.
	static class ImageHandler implements HttpHandler {
		private String imagePath;
		// 생성자를 통해 imagePath를 받아옵니다.
		public ImageHandler(String imagePath) {
			this.imagePath = imagePath;
		}
		// detail.html 페이지에서 이미지 요청을 처리하는 handler 메서드입니다.
		public void handle(HttpExchange exchange) throws IOException {
			// 이미지 파일을 정상적으로 가져오기 위해 File 클래스를 사용하여 경로를 사용합니다.
			File imageFile = new File(imagePath);
			// 이미지 파일이 존재하지 않으면 404 에러를 출력합니다.
			if (!imageFile.exists()) {
				String response = "404 (Not Found)\n";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} else {
				// 이미지 파일이 존쟇라면 200 응답을 생성합니다.
				exchange.sendResponseHeaders(200, imageFile.length());
				OutputStream os = exchange.getResponseBody();
				// FileInputStream을 통해 이미지 파일을 읽어와서 client에게 보냅니다.
				FileInputStream fs = new FileInputStream(imageFile);
				final byte[] buffer = new byte[0x10000];
				int count = 0;
				while ((count = fs.read(buffer)) >= 0) {
					os.write(buffer, 0, count);
				}
				fs.close();
				os.close();
			}
		}
	}

	// index.html에서 이동한 detail.html 페이지로 이동하기 위한 요청을 처리하는 DetailHandler 클래스를 작성하였습니다.
	static class DetailHandler implements HttpHandler {
		public void handle(HttpExchange exchange) throws IOException {
			// 요청된 furniture 항목의 이름을 추출하여 String 타입의 변수로 저장하였습니다.
			String furnitureName = exchange.getRequestURI().getPath().substring(1);
			// Json 파일의 Name 필드에 각 furniture 네임이대문자로 시작되어 있어서, html 경로와 맞추기 위해 첫 문자를 소문자로
			// 변경하였습니다.
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
				String response = "404 (Not Found)\n";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
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
				// 200 응답과 함께 furniture 항목의 detail.html 페이지를 응답으로 보냅니다.
				exchange.sendResponseHeaders(200, detailHtml.getBytes().length);
				OutputStream os = exchange.getResponseBody();
				os.write(detailHtml.getBytes());
				os.close();
			}
		}
	}
}