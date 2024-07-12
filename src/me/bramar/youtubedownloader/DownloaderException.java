package me.bramar.youtubedownloader;

public class DownloaderException {
	public static class LoadFailedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public LoadFailedException() {
			super("Unable to load video!");
		}
	}
	public static class NoLinkException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public NoLinkException() {
			super("No link is provided!");
		}
	}
	public static class TooLongException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public TooLongException() {
			super("It's been too many attempts. Exiting now...");
		}
		public TooLongException(int amount) {
			super("It's been " + amount + " attempts to check! Exiting now...");
		}
	}
}
