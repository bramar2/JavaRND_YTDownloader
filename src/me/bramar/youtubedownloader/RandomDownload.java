package me.bramar.youtubedownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class RandomDownload {
	private final Random r;
	protected RandomDownload() {
		r = new Random();
		try {
			init();
		}catch(Exception e1) {
			System.out.println("An error occured!");
			e1.printStackTrace();
		}
	}
	public int randomInt(int min, int max) {
		int n1 = min, n2 = max;
		min = Math.min(n1,n2);
		max = Math.max(n1,n2);
		if(n1 == n2) return n1;
		return r.nextInt(max - min) + min;
	}
	public void init() {
		System.out.println("Opening file chooser for User Data (Login Details)");
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setFileFilter(new FileFilter() {
			
			@Override
			public String getDescription() {
				return "User Data Folder";
			}
			
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		});
		chooser.setCurrentDirectory(new File("C:\\"));
		if(new File("D:\\").exists()) chooser.setCurrentDirectory(new File("D:\\"));
		int out = chooser.showDialog(null, "Set User Data");
		if(out != JFileChooser.APPROVE_OPTION) {
			System.out.println("Either it was cancelled or an error occured! Exiting...");
			schedule(2000, () -> System.exit(0));
			return;
		}
		File file = chooser.getSelectedFile();
		System.out.println("User data folder selected: " + file.getPath());
		
		//
		Scanner scan = new Scanner(System.in);
		System.out.println("How many random videos [Only up to 20 at a time]:");
		int amount = -1;
		try {
			amount = scan.nextInt();
		}catch(Exception ignored) {}
		scan.close();
		if(amount <= 0 || amount > 20) {
			System.out.println("The number inputted is invalid! Exiting...");
			schedule(2000, () -> System.exit(0));
			return;
		}
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--user-data-dir=" + file.getPath());
		ChromeDriver driver = new ChromeDriver(options);
		try {
			ArrayList<String> approvedLinks = new ArrayList<>();
			Thread.sleep(5000);
			driver.get("https://youtube.com");
			System.out.println("Waiting for 60 seconds for the page to load");
			Thread.sleep(60 * 1000);
			int ran = randomInt(10, 25);
			System.out.println("Scrolling down " + ran + " times to load more videos (5 second interval)");
			for(int i = 0; i <= ran; i++) {
				driver.findElement(By.tagName("body")).sendKeys(Keys.LEFT_CONTROL, Keys.END);
				Thread.sleep(5000);
			}
			driver.findElement(By.tagName("body")).sendKeys(Keys.LEFT_CONTROL, Keys.HOME);
			List<WebElement> videos = driver.findElements(By.cssSelector("#contents > ytd-rich-item-renderer"));
			for(WebElement element : new ArrayList<>(videos)) {
				try {
					WebElement in = element.findElement(By.cssSelector("div > ytd-rich-grid-media > yt-interaction"));
					if(in == null) throw new Exception();
					continue;
				}catch(Exception ignored) {}
				videos.remove(element);
			}
			System.out.println("There is a total of " + videos.size() + " videos detected");
			for(int i = 0; i < amount; i++) {
				try {
					WebElement video = videos.get(r.nextInt(videos.size()));
					WebElement details = video.findElement(By.cssSelector("div#content > ytd-rich-grid-media > div#dismissible > div#details > div#meta"));
					WebElement vtl = details.findElement(By.cssSelector("a#video-title-link.yt-simple-endpoint"));
					WebElement metadata = details.findElement(By.cssSelector("div.byline > ytd-video-meta-block > div#metadata"));
					String link = vtl.getAttribute("href");
					String title = vtl.getText();
					String owner = metadata.findElement(By.cssSelector("div#byline-container > ytd-channel-name > div#container")).getText();
					String extra = metadata.findElement(By.cssSelector("div#metadata-line > span")).getText() + " from " + metadata.findElement(By.cssSelector("div#metadata-line > span:nth-child(2)")).getText()
							+ " with length " + video.findElement(By.cssSelector("div#content > ytd-rich-grid-media > div#dismissible > ytd-thumbnail > a#thumbnail > div#overlays > ytd-thumbnail-overlay-time-status-renderer")).getText();
					int clicked = JOptionPane.showConfirmDialog(null, "Do you approve '" + title + "' by " + owner + " [" + extra + "] ?", "Confirmation", JOptionPane.YES_NO_OPTION);
					if(clicked == JOptionPane.CLOSED_OPTION) {
						System.out.println("GUI was closed. Exiting program");
						driver.quit();
						schedule(2000, () -> System.exit(0));
						return;
					}
					if(clicked != JOptionPane.YES_OPTION) throw new Exception();
					approvedLinks.add(link);
					continue;
				}catch(Exception ignored) {}
				i--;
			}
			driver.quit();
			System.out.println("Successfully got ALL approved links. Downloading them using Multi-download.");
			new Multidownload(false).download(approvedLinks);
		}catch(Exception e1) {
			if(driver != null) driver.quit();
			System.out.println("An error occured:");
			e1.printStackTrace();
			System.out.println("Exiting...");
			schedule(2000, () -> System.exit(0));
			return;
		}
	}
	private void schedule(long milliseconds, Runnable run) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() { public void run() { run.run(); }}, milliseconds, milliseconds);
	}
}
