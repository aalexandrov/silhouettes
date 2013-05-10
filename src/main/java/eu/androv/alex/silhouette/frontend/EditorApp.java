package eu.androv.alex.silhouette.frontend;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.awt.Color;
import java.io.File;

import org.apache.log4j.Logger;

/**
 * Hello world!
 */
public class EditorApp {

	static {
		if (System.getProperty("log.file") == null) {
			System.setProperty("log.file", "silhouette.log");
		}
	}

	private static Logger logger = Logger.getLogger("silhouette.editor.app");

	@SuppressWarnings("null")
	public static void main(String[] args) {

		if (args.length < 1) {
			String arg1 = "./sample/collection001/98-00_Figure11.gif";
			arg1 = EditorApp.class.getClassLoader().getResource(arg1).getFile();
			args = new String[] { arg1 };
		}

		if (args.length < 1) {
			System.out.println("Usage: silhouette [image1] [image2] ... [imageN]");
			System.exit(-1);
		}
		
		logger.info("Starting ImageJ instance");
		ImageJ instance = new ImageJ(null, ImageJ.EMBEDDED);
		IJ.runPlugIn("eu.androv.alex.silhouette.imagej.tool.Bezier_Curve_Tool", "");
		
		for (String imagePath : args) {
			File imageFile = new File(imagePath);
			if (!imageFile.isFile() || !imageFile.canRead()) {
				logger.warn("Skipping non-readable input image '" + args[0] + "'");
			} else {
				logger.info(String.format("Editing image '%s'", imagePath));
				ImagePlus imp = IJ.openImage(imagePath);
				imp.show();
			}
		}
	}
}
