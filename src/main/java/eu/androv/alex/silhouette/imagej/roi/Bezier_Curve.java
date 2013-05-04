package eu.androv.alex.silhouette.imagej.roi;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

/**
 * @Author Berin Martini
 * @Version 2007-06-06
 */
public class Bezier_Curve implements PlugInFilter {
	private ImagePlus imp;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	@Override
	public void run(ImageProcessor ip) {
		BezierCanvas bc = new BezierCanvas(imp);
		ImageWindow win = new ImageWindow(imp, bc);
	}

	class BezierCanvas extends ImageCanvas {
		private BezierList bezierList = new BezierList();

		private BezierPoint bezierPoint;

		private boolean dragging = false;

		private int pW = 8; // point width

		BezierCanvas(ImagePlus imp) {
			super(imp);
		}

		@Override
		public void update(Graphics g) // method called by repaint(). //overriding original update method
		{
			paint(g);
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g); // this passes to image to ImageCanvas super
			g.setColor(Color.red);
			updatePointGraphics(g);

		}

		@Override
		public void mouseDragged(MouseEvent e) {
			double x = offScreenXD(e.getX());
			double y = offScreenYD(e.getY());
			if (bezierList.isEmpty()) {
				bezierList.cursorPos(x, y);
			} else {
				if (bezierPoint == null) {
					bezierList.dragTo(this.imp.getRoi(), x, y);
				} else {
					bezierPoint.setPoint(x, y);
				}
			}
			repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// Once contral points are set then a click off one of the control points ends the plugin.
			if (!bezierList.isEmpty() && !IJ.shiftKeyDown()) {
				double x = offScreenXD(e.getX());
				double y = offScreenYD(e.getY());

				bezierPoint = bezierList.insideControlPoint(x, y);

				if (bezierPoint == null) {
					new ImageWindow(super.imp);
				}

			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			double x = offScreenX(e.getX());
			double y = offScreenY(e.getY());

			if (bezierList.isEmpty()) {
				bezierList.setNewBezierPoint(x, y);
				repaint();
				return;
			}

			bezierPoint = bezierList.insideControlPoint(x, y);
			if (bezierPoint == null) {
				bezierList.cursorPos(x, y);
				return;
			}

			if (IJ.spaceBarDown()) {
				int pointNum = bezierPoint.getPointNum();
				if (pointNum == 0 || pointNum == 3) {
					bezierPoint = bezierList.clonePoint(bezierPoint);
				}
				repaint();
				return;
			}

			if (IJ.shiftKeyDown()) {
				if (bezierList.onlyOneBezier()) {
					return;
				}

				int pointNum = bezierPoint.getPointNum();
				if (pointNum == 0 || pointNum == 3) {
					bezierList.removePoint(bezierPoint);
					bezierPoint = null;
					bezierList.cursorPos(x, y);
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			double x = offScreenXD(e.getX());
			double y = offScreenYD(e.getY());

			if (bezierList.isEmpty()) {
				bezierList.setNewBezierPoint(x, y);
				repaint();
			} else {
				if (bezierPoint == null) {
					bezierList.cursorPos(x, y);
					repaint();
				}
				bezierPoint = null;
			}
		}

		public void updatePointGraphics(Graphics g) {
			double[][] coor = bezierList.getPointCoordinates();
			for (int xx = 0; xx < coor.length; xx += 2) {
				double[] point0 = coor[xx];
				double[] point1 = coor[(xx + 1)];
				g.fillOval((screenXD(point0[0]) - (pW / 2)), (screenYD(point0[1]) - (pW / 2)), pW, pW);
				g.drawOval((screenXD(point1[0]) - (pW / 2)), (screenYD(point1[1]) - (pW / 2)), pW, pW);
				g.drawLine(screenXD(point0[0]), screenYD(point0[1]), screenXD(point1[0]), screenYD(point1[1]));

			}
			drawCurve();
		}

		private void drawCurve() {
			if (!dragging) {
				GeneralPath path = bezierList.getCurve();
				if (path == null)
					return;

				ShapeRoi curveROI = new ShapeRoi(path); // where path is a GeneralPath
				curveROI.setColor(Color.BLUE);
				this.imp.setRoi(curveROI);
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

	} // end class BezierCanvas

	class BezierList {
		private Bezier bezierStart = null;

		private Bezier bezierEnd = null;

		private Bezier bezierCurrent = null;

		private int pointSwitch = 0;

		private double x0, y0, x1, y1, x3, y3, xTmp, yTmp;

		public boolean isEmpty() {
			return (bezierStart == null);
		}

		public boolean onlyOneBezier() {
			return (bezierStart == bezierEnd);
		}

		public void setNewBezierPoint(double x, double y) {
			if (pointSwitch == 0) {
				x0 = x;
				y0 = y;
				xTmp = x;
				yTmp = y;
				pointSwitch = 1;
			} else if (pointSwitch == 1) {
				x1 = x;
				y1 = y;
				pointSwitch = 3;
			} else if (pointSwitch == 3) {
				x3 = x;
				y3 = y;
				xTmp = x;
				yTmp = y;
				pointSwitch = 2;
			} else if (pointSwitch == 2) {
				bezierStart = new Bezier(x0, y0, x1, y1, x, y, x3, y3);
				bezierEnd = bezierStart;
				pointSwitch = 0;
			}

		}

		public double[][] getPointCoordinates() {
			double[][] coordinates = { {} };
			if (isEmpty()) {
				if (pointSwitch == 1) {
					double[][] c = { { x0, y0 }, { xTmp, yTmp } };
					coordinates = c;
				} else if (pointSwitch == 3) {
					double[][] c = { { x0, y0 }, { x1, y1 } };
					coordinates = c;
				} else if (pointSwitch == 2) {
					double[][] c = { { x0, y0 }, { x1, y1 }, { x3, y3 }, { xTmp, yTmp } };
					coordinates = c;
				}

			} else {
				coordinates = bezierStart.getPointCoordinates();
				bezierCurrent = bezierStart.next();
				while (bezierCurrent != null) {
					double[][] coor = new double[coordinates.length + 4][2];
					System.arraycopy(coordinates, 0, coor, 0, coordinates.length);
					System.arraycopy(bezierCurrent.getPointCoordinates(), 0, coor, coordinates.length, 4);
					coordinates = coor;
					bezierCurrent = bezierCurrent.next();
				}
			}
			return coordinates;
		}

		public GeneralPath getCurve() {
			GeneralPath curve = bezierStart.getCurve();

			bezierCurrent = bezierStart.next();
			while (bezierCurrent != null) {
				curve.append(bezierCurrent.getCurve(), true);
				bezierCurrent = bezierCurrent.next();
			}

			return curve;
		}

		public void removePoint(BezierPoint point) { // Should not be called if only one bezier curve on image
			bezierCurrent = point.getParentBezier();

			if (point.getPointNum() == 3) { // True for the majority of cases

				if (bezierCurrent == bezierStart) {
					bezierStart = bezierCurrent.next();
					bezierStart.point0.movePoint(bezierCurrent.point0.x, bezierCurrent.point0.y);
					bezierStart.point1.movePoint(bezierCurrent.point1.x, bezierCurrent.point1.y);

				} else if (bezierCurrent == bezierEnd) {
					bezierEnd = bezierCurrent.previous();
					bezierEnd.setNext(null);
					bezierEnd.movePoint(3, bezierEnd.point3.x, bezierEnd.point3.y);

				} else {
					Bezier previous = bezierCurrent.previous();
					Bezier next = bezierCurrent.next();
					previous.setNext(next);
					next.setPrevious(previous);

					previous.movePoint(3, previous.point3.x, previous.point3.y);
					previous.movePoint(2, previous.point2.x, previous.point2.y);
				}

			} else if (point.getPointNum() == 0) { // point must belong to bezierStart if it has a pointNum of zero
				bezierStart = bezierCurrent.next();
				bezierStart.setPrevious(null);
				bezierStart.movePoint(0, bezierStart.point0.x, bezierStart.point0.y);

			}

		}

		public BezierPoint clonePoint(BezierPoint oldPoint) {
			Bezier oldBezier = oldPoint.getParentBezier();
			BezierPoint newPoint = null;
			Bezier newBezier = null;

			double[][] coor = oldBezier.getPointCoordinates();
			if (oldPoint.getPointNum() == 3) {
				newBezier = new Bezier(coor[2][0], coor[2][1], (coor[2][0] + coor[2][0] - coor[3][0]), (coor[2][1]
					+ coor[2][1] - coor[3][1]), coor[3][0], coor[3][1], coor[2][0], coor[2][1]);

				oldBezier.insertAsNext(newBezier);
				if (oldBezier.equals(bezierEnd)) {
					bezierEnd = newBezier;
				}
				newPoint = newBezier.point3;
			} else if (oldPoint.getPointNum() == 0) {
				newBezier = new Bezier(coor[0][0], coor[0][1], coor[1][0], coor[1][1],
					(coor[0][0] + coor[0][0] - coor[1][0]), (coor[0][1] + coor[0][1] - coor[1][1]), coor[0][0],
					coor[0][1]);
				oldBezier.insertAsPrevious(newBezier);
				if (oldBezier.equals(bezierStart)) {
					bezierStart = newBezier;
				}
				newPoint = newBezier.point0;
			}

			return newPoint;
		}

		public void cursorPos(double x, double y) {
			xTmp = x;
			yTmp = y;
		}

		public void dragTo(Roi roi, double x, double y) {
			double dx = xTmp - x;
			double dy = yTmp - y;
			java.awt.Rectangle rec = roi.getBounds();
			roi.setLocation((rec.x - (int) dx), (rec.y - (int) dy));

			bezierStart.point0.movePoint((bezierStart.point0.x - dx), (bezierStart.point0.y - dy));
			bezierStart.point1.movePoint((bezierStart.point1.x - dx), (bezierStart.point1.y - dy));
			bezierStart.point2.movePoint((bezierStart.point2.x - dx), (bezierStart.point2.y - dy));
			bezierStart.point3.movePoint((bezierStart.point3.x - dx), (bezierStart.point3.y - dy));

			bezierCurrent = bezierStart.next();
			while (bezierCurrent != null) {
				bezierCurrent.point0.movePoint((bezierCurrent.point0.x - dx), (bezierCurrent.point0.y - dy));
				bezierCurrent.point1.movePoint((bezierCurrent.point1.x - dx), (bezierCurrent.point1.y - dy));
				bezierCurrent.point2.movePoint((bezierCurrent.point2.x - dx), (bezierCurrent.point2.y - dy));
				bezierCurrent.point3.movePoint((bezierCurrent.point3.x - dx), (bezierCurrent.point3.y - dy));
				bezierCurrent = bezierCurrent.next();
			}
			xTmp = x;
			yTmp = y;
		}

		public BezierPoint insideControlPoint(double testX, double testY) {
			// The particular order in which points are checked is relided upon by other parts
			// of the programe, changing this will affect the behaver of other parts of the
			// program in potentialy strage ways.
			BezierPoint controlPoint = bezierStart.insideControlPoint(testX, testY);
			bezierCurrent = bezierStart.next();
			while (bezierCurrent != null && controlPoint == null) {
				controlPoint = bezierCurrent.insideControlPoint(testX, testY);
				bezierCurrent = bezierCurrent.next();
			}
			return controlPoint;
		}

	} // end BezierList

	class Bezier {
		private Bezier next = null;

		private Bezier previous = null;

		public BezierPoint point0;

		public BezierPoint point1;

		public BezierPoint point2;

		public BezierPoint point3;

		Bezier(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
			point0 = new BezierPoint(this, 0, x0, y0);
			point1 = new BezierPoint(this, 1, x1, y1);
			point2 = new BezierPoint(this, 2, x2, y2);
			point3 = new BezierPoint(this, 3, x3, y3);
		}

		public Bezier next() {
			return next;
		}

		public Bezier previous() {
			return previous;
		}

		public void setPrevious(Bezier newBezier) {
			previous = newBezier;
		}

		public void setNext(Bezier newBezier) {
			next = newBezier;
		}

		public void insertAsNext(Bezier newBezier) {
			newBezier.setNext(next);
			newBezier.setPrevious(this);
			if (next != null)
				next.setPrevious(newBezier);
			next = newBezier;

		}

		public void insertAsPrevious(Bezier newBezier) {
			newBezier.setNext(this);
			newBezier.setPrevious(previous);
			if (previous != null)
				previous.setNext(newBezier);
			previous = newBezier;

		}

		public void movePoint(int pointNum, double nx, double ny) {
			if (pointNum == 0) {
				double newP1X = point1.x - point0.x + nx;
				double newP1Y = point1.y - point0.y + ny;
				point1.movePoint(newP1X, newP1Y);
			} else if (pointNum == 1) {
				if (previous != null) {
					previous.point2.movePoint((point0.x + point0.x - point1.x), (point0.y + point0.y - point1.y));
				}
			} else if (pointNum == 2) {
				if (next != null) {
					next.point1.movePoint((point3.x + point3.x - point2.x), (point3.y + point3.y - point2.y));
				}
			} else if (pointNum == 3) {
				double newP2X = point2.x - point3.x + nx;
				double newP2Y = point2.y - point3.y + ny;
				point2.movePoint(newP2X, newP2Y);
				if (next != null) {
					next.point0.setPoint(nx, ny);
				}
			}
		}

		public double[][] getPointCoordinates() {
			double[][] coordinates = { { point0.x, point0.y }, { point1.x, point1.y }, { point3.x, point3.y },
				{ point2.x, point2.y } };
			return coordinates;
		}

		public GeneralPath getCurve() {
			GeneralPath curve = new GeneralPath();
			curve.moveTo((float) point0.x, (float) point0.y);
			curve.curveTo((float) point1.x, (float) point1.y, (float) point2.x, (float) point2.y, (float) point3.x,
				(float) point3.y);
			return curve;
		}

		public BezierPoint insideControlPoint(double testX, double testY) {
			// The order that the points are tested in are very important
			// if they get changed than the behaviour of other parts of
			// the program will get affected.
			if (point1.contains(testX, testY)) {
				return point1;
			} else if (point0.contains(testX, testY)) {
				return point0;
			} else if (point2.contains(testX, testY)) {
				return point2;
			} else if (point3.contains(testX, testY)) {
				return point3;
			}
			return null;
		}

	} // end class Bezier

	class BezierPoint {
		public double x;

		public double y;

		private Bezier bezier;

		private int pointNum; // will be set to 0, 1, 2 or 3 to identify which bezier control point it is.

		private int pW = 8; // point width

		BezierPoint(Bezier bezier, int pointNum, double x, double y) {
			this.bezier = bezier;
			this.pointNum = pointNum;

			movePoint(x, y);
		}

		public void movePoint(double nx, double ny) {
			this.x = nx;
			this.y = ny;
		}

		public void setPoint(double nx, double ny) {
			bezier.movePoint(pointNum, nx, ny);
			movePoint(nx, ny);
		}

		public Bezier getParentBezier() {
			return bezier;
		}

		public int getPointNum() {
			return pointNum;
		}

		public boolean contains(double testX, double testY) {
			Roi pointROI = new Roi(((int) x - (2 * pW)), ((int) y - (2 * pW)), (4 * pW), (4 * pW));
			return (pointROI.contains((int) testX, (int) testY));
		}
	} // end class BezierPoint

}