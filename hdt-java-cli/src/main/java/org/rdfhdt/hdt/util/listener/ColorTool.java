package org.rdfhdt.hdt.util.listener;

public class ColorTool {
	private final boolean color;
	private final boolean quiet;

	public ColorTool(boolean color, boolean quiet) {
		this.color = color;
		this.quiet = quiet;
	}

	public ColorTool(boolean color) {
		this(color, false);
	}


	public String prefix(String pref, int r, int g, int b) {
		return colorReset() + "[" + color(r, g, b) + pref + colorReset() + "]";
	}

	public void log(String msg) {
		log(msg, false);
	}
	public void log(String msg, boolean ignoreQuiet) {
		if (!quiet || ignoreQuiet) {
			System.out.println(prefix("INFO", 3, 1, 5) + " " + colorReset() + msg);
		}
	}

	public void logValue(String msg, String value, boolean ignoreQuiet) {
		if (!quiet || ignoreQuiet) {
			System.out.println(color(3, 1, 5) + msg + colorReset() + value);
		}
	}

	public void logValue(String msg, String value) {
		logValue(msg, value, false);
	}

	public void warn(String msg) {
		warn(msg, false);
	}

	public void warn(String msg, boolean ignoreQuiet) {
		if (!quiet || ignoreQuiet) {
			System.out.println(prefix("WARN", 5, 5, 0) + " " + colorReset() + msg);
		}
	}
	public void error(String text) {
		error(text, false);
	}


	public void error(String text, boolean ignoreQuiet) {
		error(null, text, ignoreQuiet);
	}

	public void error(String title, String text) {
		error(title, text, false);
	}

	public void error(String title, String text, boolean ignoreQuiet) {
		if (!quiet || ignoreQuiet) {
			if (title != null) {
				System.out.println(prefix("ERRR", 5, 0, 0) + " " + prefix(title, 5, 3, 0) + " " + colorReset() + text);
			} else {
				System.out.println(prefix("ERRR", 5, 0, 0) + " " + colorReset() + text);
			}
		}
	}

	public String color(int r, int g, int b) {
		if (!color) {
			return "";
		}
		int color = 16 + 36 * r + 6 * g + b;
		return "\033[38;5;" + color + "m";
	}

	public String colorReset() {
		return color ? "\033[0m" : "";
	}
}
