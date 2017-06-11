package de.hhn.opencv.video;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import de.hhn.opencv.utils.Utils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The controller associated with the only view of our application. The
 * application logic is implemented here. It handles the button for
 * starting/stopping the camera, the acquired video stream, the relative
 * controls and the histogram creation.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 2.0 (2017-03-10)
 * @since 1.0 (2013-11-20)
 * 
 */
public class VideoController {
	// the FXML button
	@FXML
	private Button button;
	// the FXML area for showing the current frame
	@FXML
	private ImageView currentFrame;
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;
	private static volatile int time;

	/**
	 * Initialize method, automatically called by @{link FXMLLoader}
	 */
	public void initialize() {
		System.out.println("initialized");
		this.capture = new VideoCapture();
		this.cameraActive = false;
	}

	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	protected void startCamera() {
		// set a fixed width for the frame
		this.currentFrame.setFitWidth(600);
		time = 0;
		// preserve image ratio
		this.currentFrame.setPreserveRatio(true);
		if (!this.cameraActive) {
			this.capture.open("C:\\Users\\AndyRadulescu\\Desktop\\imgRecog\\group4\\Ausgangsvideo.wmv");
			System.out.println("start capture...");
			if (this.capture.isOpened()) {
				System.out.println("inif");
				this.cameraActive = true;
				Runnable frameGrabber = new Runnable() {
					@Override
					public void run() {
						Mat frame = grabFrame();
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(currentFrame, imageToShow);
						time = time + 95;
					}
				};
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 95, TimeUnit.MILLISECONDS);
				// update the button content
				this.button.setText("Stop Video");
			} else {
				// log the error
				System.err.println("Impossible to open the video...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.button.setText("Start Video");
			// stop the timer
			this.stopAcquisition();
		}
	}

	private synchronized void hough(int x, int y, Mat cropped, Mat closing, Mat hough, Mat frame) {
		Mat lines = new Mat();
		Imgproc.HoughLinesP(cropped, lines, 1, Math.PI / 180, 30, 30, 5);
		Point point1 = new Point();
		Point point2 = new Point();
		Imgproc.cvtColor(closing, hough, Imgproc.COLOR_GRAY2BGR);
		for (int i = 0; i < lines.rows(); i++) {
			// tempvals - temp var for saving each line
			double datavals[] = lines.get(i, 0);
			point1 = new Point(datavals[0] + x, datavals[1] + y);
			point2 = new Point(datavals[2] + x, datavals[3] + y);
			Imgproc.line(frame, point1, point2, new Scalar(60, 179, 113), 7);
		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private synchronized Mat grabFrame() {
		Mat frame = new Mat();
		Mat closing = new Mat();
		Mat threshold = new Mat();
		Mat edges = new Mat();
		Mat hsv = new Mat();
		Mat hough = new Mat();
		Mat cropped = new Mat();
		Mat dilate = new Mat();
		Mat sobel1 = new Mat();
		Mat binary = new Mat();
		Mat gray = new Mat();
		Mat erode = new Mat();
		Mat openning = new Mat();
		if (this.capture.isOpened()) {
			try {
				this.capture.grab();
				this.capture.read(frame);
				if (!frame.empty()) {
					Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
					Imgproc.threshold(gray, binary, 150, 500, Imgproc.THRESH_BINARY);
					//Imgproc.Canny(gray, edges, 50, 350);

					Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
					Core.inRange(hsv, new Scalar(5, 60, 100, 0), new Scalar(33, 255, 255, 0), threshold);
					Imgproc.Sobel(threshold, sobel1, -1, 1, 0);
					Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));

					Imgproc.erode(sobel1, erode, kernel);
					Imgproc.dilate(sobel1, dilate, kernel);
					Imgproc.dilate(erode, openning, kernel);
					Imgproc.erode(dilate, closing, kernel);

					Rect regionOfInterest = new Rect(new Point(135, 350), new Point(280, 576));
					cropped = new Mat(closing, regionOfInterest);
					hough(135, 350, cropped, closing, hough, frame);

					if (time >= 5000 && time < 8900) {
						System.out.println(">5000");
						Rect regionOfInterest2 = new Rect(new Point(460, 420), new Point(720, 576));
						cropped = new Mat(closing, regionOfInterest2);
						hough(460, 420, cropped, closing, hough, frame);
					}
					if (time < 5000) {
						System.out.println("<5000");
						Rect regionOfInterest2 = new Rect(new Point(500, 350), new Point(600, 480));
						cropped = new Mat(closing, regionOfInterest2);
						hough(500, 350, cropped, closing, hough, frame);
					}
					if (time >= 8900) {
						System.out.println(">5000");
						Rect regionOfInterest2 = new Rect(new Point(485, 420), new Point(720, 576));
						cropped = new Mat(binary, regionOfInterest2);
						hough(485, 420, cropped, binary, hough, frame);
					}
				}
			} catch (Exception e) {
				System.err.println("Exception during the frame elaboration: " + e);
			}
		}
		//System.out.println(frame.cols() + " " + frame.rows());
		System.out.println(time);
		return frame;
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(95, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to close the video now... " + e);
			}
		}
		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image) {
		Utils.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed() {
		this.stopAcquisition();
	}
}
