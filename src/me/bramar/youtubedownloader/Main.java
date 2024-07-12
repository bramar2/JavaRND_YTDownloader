package me.bramar.youtubedownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Main {
	
	public static final String PATH_TO_SELENIUM = "D:\\marvel\\ECLIPSE JAVA\\selenium\\chromedriver_win32\\chromedriver.exe".replace("\\", File.separator);
	public static String DOWNLOAD_PATH = getDownloadPath(); 
	private static String getDownloadPath() {
		try {
			return new File(Main.class.getProtectionDomain().getCodeSource().getLocation()
				    .toURI()).getParent();
		}catch(Exception ignored) {}
		return null;
	}
	private Main() {
		this(true);
	}
	private Main(boolean initialize) {
		if(initialize) init();
	}
	public String getResolution() throws IOException {
		File readme = new File(DOWNLOAD_PATH + "\\Youtube Downloader.yml".replace("\\", File.separator));
		Scanner scan = new Scanner(readme);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if(!line.startsWith("#")) {
				String res = line;
				if(res.contains("#")) res = res.split("#")[0];
				res = res.replace(" ", "");
				scan.close();
				return res;
			}
		}
		scan.close();
		return null;
	}
	public void init() {
		String link = getYoutubeLink();
		if(link == null) System.exit(0);
		Thread thread = new Thread() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, "Sit tight while we boot up Chrome and download it automatically!", "Sit tight!", JOptionPane.OK_OPTION);
			}
		};
		thread.start();
		ChromeDriver driver = null;
		try {
			String res = getResolution();
			File folder = new File(DOWNLOAD_PATH);
			if(!folder.exists()) folder.mkdirs();
			long oldCount = Files.walk(Paths.get(folder.getPath())).filter((f) -> f.toFile().getName().toLowerCase().endsWith(".mp4")).count();
			ChromeOptions options = new ChromeOptions();
			HashMap<String, Object> chromePref = new HashMap<>();
			chromePref.put("profile.default_content_settings.popups", 0);
			chromePref.put("download.default_directory", DOWNLOAD_PATH);
			options.setExperimentalOption("prefs", chromePref);
			driver = new ChromeDriver(options);
			driver.get("https://y2mate.com");
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			WebElement textbox = driver.findElement(By.cssSelector("#txt-url"));
			textbox.click();
			textbox.sendKeys(link);
			driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
			driver.findElement(By.cssSelector("#btn-submit")).click();
			boolean found = false;
			for(int i = 0; i < 25; i++) {
				// 25 attempts, 10 seconds in-between
				try {
					System.out.println("Attempt #" + i + " at finding the video");
					WebElement element = driver.findElement(By.cssSelector("#result > div > div.col-xs-12.col-sm-5.col-md-5 > div.thumbnail.cover > a > img"));
					if(element != null) {
						found = true;
						break;
					}
					Thread.sleep(10000);
				}catch(Exception ignored) {}
			}
			if(!found) throw new DownloaderException.LoadFailedException();
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("document.querySelector(\"#bootstrap-themes > div:nth-child(2)\").remove();");
			driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
			List<WebElement> list = driver.findElement(By.cssSelector("#mp4 > table > tbody")).findElements(By.xpath(".//tr")).get(0).findElements(By.xpath(".//td"));
			WebElement downloadButton = list.get(list.size()-1);
			if(res != null) {
				// Resolution Picker
				boolean resFound = false;
				WebElement resButton = null;
				
				for(WebElement element : driver.findElement(By.cssSelector("#mp4 > table > tbody")).findElements(By.xpath(".//tr"))) {
					try {
						List<WebElement> array = element.findElements(By.cssSelector("td"));
						String resName = array.get(0).getText().replace(" ", "");
						if(resName.toLowerCase().startsWith(res.toLowerCase())) {
							resFound = true;
							resButton = array.get(array.size()-1);
							System.out.println("Resolution found! Resolution full name in website: '" + array.get(0).getText() + "'");
							break;
						}
					}catch(Exception ignored) {}
				}
				
				if(resFound && resButton != null) downloadButton = resButton;
			}
			downloadButton.click();
			found = false;
			for(int i = 0; i < 35; i++) {
				try {
					System.out.println("Attempt #" + i + " to check if the download button has arrived");
					WebElement element = driver.findElement(By.xpath("/html/body/div[1]/div[2]/div[2]/div/div[2]/div[2]/div/a"));
					if(element != null) {
						found = true;
						break;
					}
					Thread.sleep(5000);
				}catch(Exception ignored) {}
			}
			if(!found) throw new DownloaderException.LoadFailedException();
			String href = driver.findElement(By.xpath("/html/body/div[1]/div[2]/div[2]/div/div[2]/div[2]/div/a")).getAttribute("href");
			// LINK TO DOWNLOAD
			driver.get("chrome://version");
			driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
			System.out.println("Downloading video...");
			driver.get(href);
			Thread.sleep(500);
			driver.get("chrome://downloads");
			Thread.sleep(500);
			for(int i = 0; i < 5000; i++) {
				try {
					// Automatically presses Retry or Resume button if exists
					try {
						Object obj = js.executeScript("document.querySelector(\"downloads-manager\").shadowRoot.querySelectorAll(\"#downloadsList downloads-item\")");
						System.out.println("Object class name: " + obj.getClass());
						NodeList nodeList = (NodeList) obj;
						System.out.println("Nodelist == null: " + nodeList == null);
						Thread.sleep(10);
						if(nodeList.getLength() > 0) for(int n = 0; n < nodeList.getLength(); n++) {
							try {
								js.executeScript("document.querySelector(\"downloads-manager\").shadowRoot.querySelectorAll(\"#downloadsList downloads-item\")[" + n + "].shadowRoot.querySelector(\"cr-button[focus-type=retry]\").click()");
								continue;
							}catch(Exception ignored) {}
							try {
								Node node = (Node) js.executeScript("document.querySelector(\"downloads-manager\").shadowRoot.querySelectorAll(\"#downloadsList downloads-item\")[" + n + "].shadowRoot.querySelector(\"cr-button[focus-type=pauseOrResume]\")");
								if(node.getTextContent().toLowerCase().contains("resume")) {
									// Its a resume and not a pause button!
									js.executeScript("document.querySelector(\"downloads-manager\").shadowRoot.querySelectorAll(\"#downloadsList downloads-item\")[" + n + "].shadowRoot.querySelector(\"cr-button[focus-type=pauseOrResume]\").click()");
								}
								continue;
							}catch(Exception ignored) {}
						}
					}catch(Exception e1) {
						System.out.println(e1.getClass() + ": " + e1.getMessage());
						if(e1.getStackTrace() != null) for(StackTraceElement s : e1.getStackTrace()) {
							if(s.getClassName().toLowerCase().contains("main")) {
								System.out.println("At line " + s.getLineNumber() + " at file " + s.getFileName() + " on method " + s.getMethodName() + " in " + s.getClassName());
							}
						}
					}
					//
					System.out.println("Attempt #" + i + " of checking if it's downloaded");
					long newCount = Files.walk(Paths.get(folder.getPath())).filter((f) -> f.toFile().getName().toLowerCase().endsWith(".mp4")).count();
					if(newCount > oldCount) {
						// Downloaded
						System.out.println("Download finish!");
						driver.quit();
						System.out.println("ChromeDriver has been closed!");
						cleanFileNames();
						System.out.println("File names have been cleaned!");
						JOptionPane.showMessageDialog(null, "Video has been successfully downloaded!", "Done!", 0);
						System.exit(0);
					}
					Thread.sleep(10000);
				}catch(Exception ignored) {}
			}
			throw new DownloaderException.TooLongException(500);
		}catch(Exception e1) {
			e1.printStackTrace();
			if(driver != null) driver.quit();
			System.out.println("Driver has been closed!");
			System.out.println("Exiting...");
			System.exit(e1.getClass().getName().hashCode());
		}
		
	}
	
	private static final String[] PREFIXES = new String[] {
			"y2mate.com - ",
			"y2mate.com -",
			"y2mate.com ",
			"y2mate.com",
			"http://y2mate.com - ",
			"http://y2mate.com -",
			"http://y2mate.com ",
			"http://y2mate.com",
			"https://y2mate.com - ",
			"https://y2mate.com -",
			"https://y2mate.com ",
			"https://y2mate.com",
	};
	
	private void cleanFileNames() throws IOException {
		int mp4count = 0;
		for(File file : new File(DOWNLOAD_PATH).listFiles()) {
			if(!file.getName().toLowerCase().endsWith(".mp4")) continue;
			mp4count++;
			System.out.println("Going through file " + file.getPath());
			boolean replace = false;
			String newName = file.getName();
			for(String prefix : PREFIXES) {
				if(newName.toLowerCase().startsWith(prefix)) {
					newName = newName.substring(prefix.length());
					replace = true;
				}
			}
			if(replace) {
				// Replace
				System.out.println("Renaming file " + file.getPath() + " to " + newName);
				String sourcePath = file.toPath().toString();
				Path source = file.toPath();
				String targetPath = "";
				String separator = file.getName().contains("\\") ? "\\" : File.separator;
				String[] split = file.getPath().split("\\" + separator);
				for(int i = 0; i < split.length - 1; i++) {
					targetPath += split[i] + separator;
				}
				targetPath += newName;
				Path target = Paths.get(targetPath);
				Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Successfully renamed file " + sourcePath + " to " + targetPath);
			}else System.out.println("No detections to rename the file");
		}
		if(mp4count == 0) System.out.println("No video files in the directory!");
	}
	public String getYoutubeLink() {
		return JOptionPane.showInputDialog("Put youtube link here!");
	}
	public static void main(String[] args) {
		ArrayList<String> list = new ArrayList<>(Arrays.asList(args));
		try {
			for(int i = 0; i < list.size(); i++) {
				String str = list.get(i);
				if(str.startsWith("-downloadpath=") || str.startsWith("-dpath=")) {
					if(str.startsWith("-downloadpath=\"") || str.startsWith("-dpath=\"")) {
						String path = str.split("=\"")[1];
						for(int a = (i + 1); a < list.size(); a++) {
							if(list.get(a).contains("\"")) {
								path += " " + list.get(a).split("\"")[0];
								break;
							}
							path += " " + list.get(a);
						}
						DOWNLOAD_PATH = path.replace("%20", " ");
					}else {
						String path = str.split("=")[1].replace("%20", " ");
						DOWNLOAD_PATH = path;
					}
				}
			}
		}catch(Exception ignored) {}
		System.setProperty("webdriver.chrome.driver", PATH_TO_SELENIUM);
		System.out.println("Selenium Chrome Driver Path: " + PATH_TO_SELENIUM);
		System.out.println("Download Path (where all downloads from this go): " + DOWNLOAD_PATH);
		try {
			if(contains(list, "--cleanFileNames")) {
				System.out.println("Cleaning file names");
				new Main(false).cleanFileNames();
				return;
			}else if(contains(list, "--multidownload") || contains(list, "--multi-download") || contains(list, "--multipledownload") || contains(list, "--multiple-download")) {
				System.out.println("Running multi-download");
				new Multidownload();
				return;
			}else if(contains(list, "--random")) {
				System.out.println("Running random download");
				new RandomDownload();
				return;
			}
		}catch(Exception ignored) {}		
		System.out.println("Running normal download");
		new Main();
	}
	public static boolean contains(Iterable<String> iter, String str) {
		for(String s : iter) {
			if(s.equalsIgnoreCase(str)) return true;
		}
		return false;
	}
}
